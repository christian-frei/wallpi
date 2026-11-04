package ch.survalit.wallpi.wallpapers.control;

import static ch.survalit.wallpi.wallpapers.Requirement.Rn.*;
import static ch.survalit.wallpi.wallpapers.control.Fixtures.SOURCES;
import static ch.survalit.wallpi.wallpapers.control.Fixtures.rejected;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import ch.survalit.wallpi.wallpapers.Requirement;
import ch.survalit.wallpi.wallpapers.entity.Source;
import ch.survalit.wallpi.wallpapers.entity.Source.Status;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SourcesFileTest {

    record Listing(SourcesFile file, List<Source> sources, Throwable error) {

        Source titled(String title) {
            return sources.stream()
                    .filter(source -> source.title().equals(title))
                    .findFirst()
                    .orElseThrow();
        }

        List<String> titles() {
            return titles(sources);
        }

        static List<String> titles(List<Source> sources) {
            return sources.stream().map(Source::title).toList();
        }
    }

    @TempDir
    Path home;

    @ParameterizedTest(name = "{0}")
    @MethodSource("listSourcesCases")
    void listSources(Requirement.Rn requirement, String content, Consumer<Listing> expectation) throws IOException {
        var sources = new SourcesFile();
        sources.file = home.resolve(".wallpi/sources.json");
        if (content != null) {
            Files.createDirectories(sources.file.getParent());
            Files.writeString(sources.file, content);
        }
        expectation.accept(list(sources));
    }

    static Listing list(SourcesFile sources) {
        try {
            return new Listing(sources, sources.all(), null);
        } catch (RuntimeException e) {
            return new Listing(sources, List.of(), e);
        }
    }

    static Stream<Arguments> listSourcesCases() {
        return Stream.of(
                arguments(R1_1, SOURCES, (Consumer<Listing>) SourcesFileTest::inFileOrder),
                arguments(R1_2, SOURCES, (Consumer<Listing>) SourcesFileTest::rereadsEdits),
                arguments(R1_3, SOURCES, (Consumer<Listing>) SourcesFileTest::stableIds),
                arguments(R1_4, null, (Consumer<Listing>) SourcesFileTest::createsDefaults),
                arguments(R1_5, "[{\"title\": ", (Consumer<Listing>) SourcesFileTest::rejectsBrokenJson),
                arguments(R1_6, SOURCES, (Consumer<Listing>) SourcesFileTest::marksIncompleteEntries),
                arguments(R1_7, SOURCES, (Consumer<Listing>) SourcesFileTest::marksUnknownHosts));
    }

    static void inFileOrder(Listing listing) {
        assertThat(listing.titles()).containsExactly("Star Trek", "Unsplash Nature", "No Url", "", "Elsewhere");
        var starTrek = listing.titled("Star Trek");
        assertThat(starTrek.description()).isEqualTo("The final frontier");
        assertThat(starTrek.url()).isEqualTo(URI.create("https://wallhaven.cc/api/v1/search?q=star%20trek&atleast=3840x2160&purity=111&page=7"));
        assertThat(starTrek.toJSON().getString("id")).isEqualTo(starTrek.id());
    }

    static void rereadsEdits(Listing listing) {
        write(listing.file(), """
                [{"title": "Deep Space Nine", "description": "", "url": "https://wallhaven.cc/api/v1/search?q=ds9"}]
                """);
        assertThat(Listing.titles(listing.file().all())).containsExactly("Deep Space Nine");
    }

    static void stableIds(Listing listing) {
        var ids = listing.sources().stream().map(Source::id).toList();
        var relisted = listing.file().all().stream().map(Source::id).toList();
        assertThat(relisted).isEqualTo(ids);
        assertThat(ids).doesNotHaveDuplicates();
    }

    static void createsDefaults(Listing listing) {
        assertThat(listing.file().file).exists();
        assertThat(listing.titles()).containsExactly("Star Trek", "Nature", "Nature on Unsplash");
        assertThat(listing.sources()).allMatch(Source::isValid);
    }

    static void rejectsBrokenJson(Listing listing) {
        rejected(listing.error(), 500, listing.file().file.toString());
    }

    static void marksIncompleteEntries(Listing listing) {
        assertThat(listing.titled("No Url").status()).isEqualTo(Status.INVALID);
        assertThat(listing.titled("No Url").reason()).isEqualTo("missing url");
        assertThat(listing.titled("").status()).isEqualTo(Status.INVALID);
        assertThat(listing.titled("").reason()).isEqualTo("missing title");
        assertThat(listing.titled("Star Trek").status()).isEqualTo(Status.VALID);
        assertThat(listing.sources()).hasSize(5);
    }

    static void marksUnknownHosts(Listing listing) {
        var elsewhere = listing.titled("Elsewhere");
        assertThat(elsewhere.status()).isEqualTo(Status.UNSUPPORTED);
        assertThat(elsewhere.reason()).isEqualTo("unsupported host example.com, expected wallhaven.cc or api.unsplash.com");
        assertThat(listing.titled("Unsplash Nature").status()).isEqualTo(Status.VALID);
        assertThat(listing.sources()).hasSize(5);
    }

    static void write(SourcesFile sources, String content) {
        try {
            Files.writeString(sources.file, content);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
