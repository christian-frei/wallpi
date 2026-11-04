package ch.survalit.wallpi.wallpapers.control;

import static ch.survalit.wallpi.wallpapers.Requirement.Rn.*;
import static ch.survalit.wallpi.wallpapers.control.Fixtures.SOURCES;
import static ch.survalit.wallpi.wallpapers.control.Fixtures.rejected;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import ch.survalit.wallpi.wallpapers.Requirement;
import ch.survalit.wallpi.wallpapers.entity.Provider;
import ch.survalit.wallpi.wallpapers.entity.Source;
import ch.survalit.wallpi.wallpapers.entity.Wallpaper;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CatalogTest {

    record Fixture(Catalog catalog, ImageApiStub stub) {

        String idOf(String title) {
            return catalog.sources.all().stream()
                    .filter(source -> source.title().equals(title))
                    .map(Source::id)
                    .findFirst()
                    .orElse(title);
        }
    }

    record Outcome<T>(T value, Throwable error) {

        static <T> Outcome<T> of(Supplier<T> act) {
            try {
                return new Outcome<>(act.get(), null);
            } catch (RuntimeException e) {
                return new Outcome<>(null, e);
            }
        }
    }

    @TempDir
    Path home;

    Fixture fixture;

    @BeforeEach
    void init() throws IOException {
        var sources = new SourcesFile();
        sources.file = Files.writeString(home.resolve("sources.json"), SOURCES);
        var stub = new ImageApiStub();
        var catalog = new Catalog();
        catalog.sources = sources;
        catalog.wallhaven = new Wallhaven();
        catalog.wallhaven.base = stub.uri();
        catalog.unsplash = new Unsplash();
        catalog.unsplash.base = stub.uri();
        catalog.unsplash.accessKey = Optional.of("duke");
        fixture = new Fixture(catalog, stub);
    }

    @AfterEach
    void stop() {
        fixture.stub().close();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("listWallpapersCases")
    void listWallpapers(Requirement.Rn requirement, String source, int page, String season,
                        Consumer<Fixture> arrange, BiConsumer<Fixture, Outcome<Wallpaper.Page>> expectation) {
        arrange.accept(fixture);
        var outcome = Outcome.of(() -> fixture.catalog().page(fixture.idOf(source), page, season));
        expectation.accept(fixture, outcome);
    }

    static Stream<Arguments> listWallpapersCases() {
        return Stream.of(
                arguments(R2_1, "Star Trek", 1, "", arrange(f -> f.stub().reply("/api/v1/search", search(1, 3, item("enterprise", "sfw"), item("voyager", "sfw")))),
                        expect(CatalogTest::firstPage)),
                arguments(R2_2, "Star Trek", 3, "", arrange(f -> f.stub().reply("/api/v1/search", search(3, 3, item("defiant", "sfw")))),
                        expect(CatalogTest::lastPage)),
                arguments(R2_3, "Star Trek", 1, "", arrange(f -> f.stub().reply("/api/v1/search", search(1, 1, item("enterprise", "sfw"), item("orion", "sketchy"), item("risa", "nsfw")))),
                        expect(CatalogTest::safeForWorkWithSourceQuery)),
                arguments(R2_4, "Unsplash Nature", 1, "", arrange(f -> f.stub().reply("/search/photos", unsplashSearch(2, photo("fjord", "https://images.unsplash.com/download")))),
                        expect(CatalogTest::attributedAndAuthorized)),
                arguments(R2_5, "Unsplash Nature", 1, "", arrange(CatalogTest::withoutUnsplashKey),
                        expect(CatalogTest::missingKeySparesWallhaven)),
                arguments(R2_6, "Star Trek", 1, "", arrange(f -> f.stub().reply("/api/v1/search", 500, "{}", Map.of())),
                        expect(CatalogTest::providerFailureNamesSource)),
                arguments(R2_7, "Elsewhere", 1, "", arrange(_ -> {}),
                        expect(CatalogTest::unusableSourceRejected)),
                arguments(R2_8, "Unsplash Nature", 1, "autumn", arrange(CatalogTest::bothProvidersAnswer),
                        expect(CatalogTest::seasonAddedToSearchTerms)),
                arguments(R2_9, "Star Trek", 1, "winter", arrange(_ -> {}),
                        expect(CatalogTest::seasonOnNonSeasonalRejected)),
                arguments(R2_10, "Nature", 1, "monsoon", arrange(_ -> {}),
                        expect(CatalogTest::unknownSeasonRejected)));
    }

    static void bothProvidersAnswer(Fixture fixture) {
        fixture.stub().reply("/search/photos", unsplashSearch(1, photo("maple", "https://images.unsplash.com/download")));
        fixture.stub().reply("/api/v1/search", search(1, 1, item("larch", "sfw")));
    }

    static void seasonAddedToSearchTerms(Fixture fixture, Outcome<Wallpaper.Page> outcome) {
        assertThat(outcome.value().wallpapers()).extracting(Wallpaper::id).containsExactly("maple");
        assertThat(fixture.stub().last("/search/photos").query().split("&")).containsExactly("query=nature%20autumn", "per_page=2", "page=1");
        var wallhaven = fixture.catalog().page(fixture.idOf("Nature"), 1, "spring");
        assertThat(wallhaven.wallpapers()).extracting(Wallpaper::id).containsExactly("larch");
        assertThat(fixture.stub().last("/api/v1/search").query().split("&")).containsExactly("atleast=3840x2160", "q=nature%20spring", "purity=100", "page=1");
    }

    static void seasonOnNonSeasonalRejected(Fixture fixture, Outcome<Wallpaper.Page> outcome) {
        rejected(outcome.error(), 400, "source 'Star Trek' is not seasonal");
        assertThat(fixture.stub().received).isEmpty();
    }

    static void unknownSeasonRejected(Fixture fixture, Outcome<Wallpaper.Page> outcome) {
        rejected(outcome.error(), 400, "unknown season monsoon, expected spring, summer, autumn or winter");
        assertThat(fixture.stub().received).isEmpty();
    }

    static void firstPage(Fixture fixture, Outcome<Wallpaper.Page> outcome) {
        var page = outcome.value();
        assertThat(page.number()).isEqualTo(1);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.wallpapers()).extracting(Wallpaper::id).containsExactly("enterprise", "voyager");
        var enterprise = page.wallpapers().getFirst();
        assertThat(enterprise.preview()).isEqualTo(URI.create("https://th.wallhaven.cc/lg/enterprise.jpg"));
        assertThat(enterprise.resolution()).isEqualTo("3840x2160");
        assertThat(enterprise.attribution()).isNull();
    }

    static void lastPage(Fixture fixture, Outcome<Wallpaper.Page> outcome) {
        assertThat(outcome.value().number()).isEqualTo(3);
        assertThat(outcome.value().hasMore()).isFalse();
        assertThat(fixture.stub().last("/api/v1/search").hasParameter("page=3")).isTrue();
    }

    static void safeForWorkWithSourceQuery(Fixture fixture, Outcome<Wallpaper.Page> outcome) {
        assertThat(outcome.value().wallpapers()).extracting(Wallpaper::id).containsExactly("enterprise");
        var request = fixture.stub().last("/api/v1/search");
        assertThat(request.query().split("&")).containsExactly("q=star%20trek", "atleast=3840x2160", "purity=100", "page=1");
    }

    static void attributedAndAuthorized(Fixture fixture, Outcome<Wallpaper.Page> outcome) {
        var page = outcome.value();
        assertThat(page.hasMore()).isTrue();
        var fjord = page.wallpapers().getFirst();
        assertThat(fjord.attribution()).isEqualTo(new Wallpaper.Attribution("Jean-Luc Picard", URI.create("https://unsplash.com/@picard")));
        assertThat(fjord.preview()).isEqualTo(URI.create("https://images.unsplash.com/fjord?w=400"));
        var request = fixture.stub().last("/search/photos");
        assertThat(request.headers().getFirst("Authorization")).isEqualTo("Client-ID duke");
        assertThat(request.query().split("&")).containsExactly("query=nature", "per_page=2", "page=1");
    }

    static void withoutUnsplashKey(Fixture fixture) {
        fixture.catalog().unsplash.accessKey = Optional.empty();
        fixture.stub().reply("/api/v1/search", search(1, 1, item("enterprise", "sfw")));
    }

    static void missingKeySparesWallhaven(Fixture fixture, Outcome<Wallpaper.Page> outcome) {
        rejected(outcome.error(), 503, "UNSPLASH_ACCESS_KEY");
        assertThat(fixture.stub().received).noneMatch(request -> request.path().equals("/search/photos"));
        var wallhaven = fixture.catalog().page(fixture.idOf("Star Trek"), 1, "");
        assertThat(wallhaven.wallpapers()).extracting(Wallpaper::id).containsExactly("enterprise");
    }

    static void providerFailureNamesSource(Fixture fixture, Outcome<Wallpaper.Page> outcome) {
        rejected(outcome.error(), 502, "source 'Star Trek' answered HTTP 500");
        fixture.catalog().wallhaven.base = closedPort();
        var unreachable = Outcome.of(() -> fixture.catalog().page(fixture.idOf("Star Trek"), 1, ""));
        rejected(unreachable.error(), 502, "source 'Star Trek' is unreachable");
    }

    static void unusableSourceRejected(Fixture fixture, Outcome<Wallpaper.Page> outcome) {
        rejected(outcome.error(), 400, "source 'Elsewhere' is unsupported");
        rejected(Outcome.of(() -> fixture.catalog().page(fixture.idOf("No Url"), 1, "")).error(), 400, "source 'No Url' is invalid: missing url");
        rejected(Outcome.of(() -> fixture.catalog().page("tribble", 1, "")).error(), 404, "unknown source tribble");
        assertThat(fixture.stub().received).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("resolveOriginalCases")
    void resolveOriginal(Requirement.Rn requirement, String source, String wallpaperId,
                         Consumer<Fixture> arrange, BiConsumer<Fixture, Outcome<Wallpaper>> expectation) {
        arrange.accept(fixture);
        var outcome = Outcome.of(() -> fixture.catalog().original(fixture.idOf(source), wallpaperId));
        expectation.accept(fixture, outcome);
    }

    static Stream<Arguments> resolveOriginalCases() {
        return Stream.of(
                arguments(R3_1, "Star Trek", "enterprise", arrange(f -> f.stub().reply("/api/v1/w/enterprise", "{\"data\": %s}".formatted(item("enterprise", "sfw")))),
                        expectOriginal(CatalogTest::wallhavenFullResolution)),
                arguments(R3_2, "Unsplash Nature", "fjord", arrange(CatalogTest::unsplashPhoto),
                        expectOriginal(CatalogTest::unsplashRawAndRegistered)),
                arguments(R3_3, "Star Trek", "borg-cube", arrange(_ -> {}),
                        expectOriginal(CatalogTest::unknownWallpaperRejected)));
    }

    static void wallhavenFullResolution(Fixture fixture, Outcome<Wallpaper> outcome) {
        var original = outcome.value();
        assertThat(original.provider()).isEqualTo(Provider.WALLHAVEN);
        assertThat(original.original()).isEqualTo(URI.create("https://w.wallhaven.cc/full/enterprise.jpg"));
        assertThat(original.resolution()).isEqualTo("3840x2160");
    }

    static void unsplashPhoto(Fixture fixture) {
        var downloadLocation = fixture.stub().uri() + "/photos/fjord/download";
        fixture.stub().reply("/photos/fjord", photo("fjord", downloadLocation));
        fixture.stub().reply("/photos/fjord/download", "{\"url\": \"https://images.unsplash.com/fjord\"}");
    }

    static void unsplashRawAndRegistered(Fixture fixture, Outcome<Wallpaper> outcome) {
        var original = outcome.value();
        assertThat(original.provider()).isEqualTo(Provider.UNSPLASH);
        assertThat(original.original()).isEqualTo(URI.create("https://images.unsplash.com/fjord"));
        assertThat(original.resolution()).isEqualTo("6000x4000");
        var registration = fixture.stub().last("/photos/fjord/download");
        assertThat(registration.headers().getFirst("Authorization")).isEqualTo("Client-ID duke");
    }

    static void unknownWallpaperRejected(Fixture fixture, Outcome<Wallpaper> outcome) {
        rejected(outcome.error(), 404, "wallpaper borg-cube is unknown at Wallhaven");
    }

    static Consumer<Fixture> arrange(Consumer<Fixture> arrangement) {
        return arrangement;
    }

    static BiConsumer<Fixture, Outcome<Wallpaper.Page>> expect(BiConsumer<Fixture, Outcome<Wallpaper.Page>> expectation) {
        return expectation;
    }

    static BiConsumer<Fixture, Outcome<Wallpaper>> expectOriginal(BiConsumer<Fixture, Outcome<Wallpaper>> expectation) {
        return expectation;
    }

    static String search(int currentPage, int lastPage, String... items) {
        return """
                {"data": [%s], "meta": {"current_page": %d, "last_page": %d, "per_page": 24, "total": 57}}
                """.formatted(String.join(",", items), currentPage, lastPage);
    }

    static String item(String id, String purity) {
        return """
                {"id": "%1$s", "purity": "%2$s", "path": "https://w.wallhaven.cc/full/%1$s.jpg",
                 "dimension_x": 3840, "dimension_y": 2160, "resolution": "3840x2160",
                 "thumbs": {"large": "https://th.wallhaven.cc/lg/%1$s.jpg"}}
                """.formatted(id, purity);
    }

    static String unsplashSearch(int totalPages, String... photos) {
        return """
                {"total": 30, "total_pages": %d, "results": [%s]}
                """.formatted(totalPages, Stream.of(photos).collect(Collectors.joining(",")));
    }

    static String photo(String id, String downloadLocation) {
        return """
                {"id": "%1$s", "width": 6000, "height": 4000,
                 "urls": {"raw": "https://images.unsplash.com/%1$s", "small": "https://images.unsplash.com/%1$s?w=400"},
                 "links": {"download_location": "%2$s"},
                 "user": {"name": "Jean-Luc Picard", "links": {"html": "https://unsplash.com/@picard"}}}
                """.formatted(id, downloadLocation);
    }

    static URI closedPort() {
        try (var socket = new ServerSocket(0)) {
            return URI.create("http://127.0.0.1:" + socket.getLocalPort());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
