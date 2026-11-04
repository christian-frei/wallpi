package ch.survalit.wallpi.desktop.control;

import static ch.survalit.wallpi.desktop.Requirement.Rn.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import ch.survalit.wallpi.desktop.Requirement;
import ch.survalit.wallpi.wallpapers.entity.Provider;
import ch.survalit.wallpi.wallpapers.entity.Wallpaper;
import jakarta.json.JsonObject;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import module jdk.httpserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DesktopTest {

    static final String IMAGE = "ENTERPRISE-NCC-1701-D";
    static final int TRUNCATED_LENGTH = 4096;

    record Fixture(Desktop desktop, FakeCatalog catalog, Path folder, HttpServer originals,
                   List<String> downloads, List<List<String>> commands) {

        URI original(String path) {
            return URI.create("http://127.0.0.1:%d%s".formatted(originals.getAddress().getPort(), path));
        }

        void resolveTo(String path) {
            catalog.original = Wallpaper.of("enterprise", Provider.WALLHAVEN, original("/thumb.jpg"), original(path), 3840, 2160);
        }

        Path expectedFile() {
            return folder.resolve("wallhaven-enterprise.jpg");
        }

        List<Path> storedFiles() {
            if (Files.notExists(folder)) {
                return List.of();
            }
            try (var files = Files.list(folder)) {
                return files.toList();
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }
    }

    record Outcome(Path value, Throwable error) {

        static Outcome of(Supplier<Path> act) {
            try {
                return new Outcome(act.get(), null);
            } catch (RuntimeException e) {
                return new Outcome(null, e);
            }
        }
    }

    @TempDir
    Path home;

    Fixture fixture;

    @BeforeEach
    void init() throws IOException {
        var downloads = new CopyOnWriteArrayList<String>();
        var originals = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        originals.createContext("/", exchange -> serve(exchange, downloads));
        originals.start();
        var commands = new CopyOnWriteArrayList<List<String>>();
        var desktop = new Desktop();
        desktop.catalog = new FakeCatalog();
        desktop.pictures = new Pictures();
        desktop.pictures.folder = home.resolve("Pictures/wallpi");
        desktop.desktopPicture = new DesktopPicture();
        desktop.desktopPicture.shell = command -> record(commands, command);
        fixture = new Fixture(desktop, (FakeCatalog) desktop.catalog, desktop.pictures.folder, originals, downloads, commands);
        fixture.resolveTo("/full/enterprise.jpg");
    }

    @AfterEach
    void stop() {
        fixture.originals().stop(0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("applyWallpaperCases")
    void applyWallpaper(Requirement.Rn requirement, Consumer<Fixture> arrange, BiConsumer<Fixture, Outcome> expectation) {
        arrange.accept(fixture);
        var outcome = Outcome.of(() -> fixture.desktop().apply("star-trek", "enterprise"));
        expectation.accept(fixture, outcome);
    }

    static Stream<Arguments> applyWallpaperCases() {
        return Stream.of(
                arguments(R1_1, arrange(_ -> {}), expect(DesktopTest::resolvedAndStoredUnderProviderAndId)),
                arguments(R1_2, arrange(_ -> {}), expect(DesktopTest::createsMissingFolder)),
                arguments(R1_3, arrange(DesktopTest::alreadyStored), expect(DesktopTest::reusesStoredFile)),
                arguments(R1_4, arrange(_ -> {}), expect(DesktopTest::setsEveryDesktop)),
                arguments(R1_5, arrange(DesktopTest::connectionDrops), expect(DesktopTest::failedDownloadLeavesNothing)),
                arguments(R1_6, arrange(DesktopTest::macOSRefuses), expect(DesktopTest::refusalKeepsFile)),
                arguments(R1_7, arrange(DesktopTest::applyInFlight), expect(DesktopTest::rejectsWhileBusy)));
    }

    static void resolvedAndStoredUnderProviderAndId(Fixture fixture, Outcome outcome) {
        assertThat(fixture.catalog().resolved).containsExactly("star-trek/enterprise");
        assertThat(outcome.value()).isEqualTo(fixture.expectedFile());
        assertThat(content(outcome.value())).isEqualTo(IMAGE);
        assertThat(fixture.downloads()).containsExactly("/full/enterprise.jpg");
    }

    static void createsMissingFolder(Fixture fixture, Outcome outcome) {
        assertThat(fixture.folder()).isDirectory();
        assertThat(fixture.storedFiles()).containsExactly(fixture.expectedFile());
    }

    static void alreadyStored(Fixture fixture) {
        try {
            Files.createDirectories(fixture.folder());
            Files.writeString(fixture.expectedFile(), "CACHED");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    static void reusesStoredFile(Fixture fixture, Outcome outcome) {
        assertThat(outcome.value()).isEqualTo(fixture.expectedFile());
        assertThat(content(outcome.value())).isEqualTo("CACHED");
        assertThat(fixture.downloads()).isEmpty();
        assertThat(fixture.commands()).hasSize(1);
    }

    static void setsEveryDesktop(Fixture fixture, Outcome outcome) {
        var script = """
                tell application "System Events"
                    repeat with eachDesktop in desktops
                        set picture of eachDesktop to POSIX file "%s"
                    end repeat
                end tell""".formatted(fixture.expectedFile().toAbsolutePath());
        assertThat(fixture.commands()).containsExactly(List.of("osascript", "-e", script));
        assertThat(outcome.value()).isEqualTo(fixture.expectedFile());
    }

    /// Announces more bytes than it sends, then closes the socket, like a connection dropping mid-download.
    static void connectionDrops(Fixture fixture) {
        try {
            var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            Thread.ofVirtual().start(() -> sendTruncated(socket));
            var original = URI.create("http://127.0.0.1:%d/truncated.jpg".formatted(socket.getLocalPort()));
            fixture.catalog().original = Wallpaper.of("enterprise", Provider.WALLHAVEN, original, original, 3840, 2160);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    static void sendTruncated(ServerSocket socket) {
        try (socket; var connection = socket.accept(); var out = connection.getOutputStream()) {
            connection.getInputStream().read(new byte[1024]);
            out.write("""
                    HTTP/1.1 200 OK\r
                    Content-Length: %d\r
                    \r
                    %s""".formatted(TRUNCATED_LENGTH, IMAGE).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    static void failedDownloadLeavesNothing(Fixture fixture, Outcome outcome) {
        rejected(outcome.error(), 502, "downloading http://127.0.0.1:");
        rejected(outcome.error(), 502, "/truncated.jpg failed");
        assertThat(fixture.storedFiles()).isEmpty();
        fixture.resolveTo("/missing.jpg");
        var missing = Outcome.of(() -> fixture.desktop().apply("star-trek", "enterprise"));
        rejected(missing.error(), 502, "failed: HTTP 404");
        assertThat(fixture.storedFiles()).isEmpty();
        assertThat(fixture.commands()).isEmpty();
    }

    static void macOSRefuses(Fixture fixture) {
        fixture.desktop().desktopPicture.shell = _ -> new DesktopPicture.Execution(1, "execution error: System Events got an error: Not authorized. (-1743)\n");
    }

    static void refusalKeepsFile(Fixture fixture, Outcome outcome) {
        rejected(outcome.error(), 500, "macOS refused to set the desktop picture: execution error: System Events got an error: Not authorized. (-1743)");
        assertThat(fixture.storedFiles()).containsExactly(fixture.expectedFile());
    }

    static void applyInFlight(Fixture fixture) {
        fixture.catalog().release = new CountDownLatch(1);
        var first = CompletableFuture.supplyAsync(() -> fixture.desktop().apply("star-trek", "enterprise"));
        fixture.catalog().awaitEntered();
        fixture.catalog().first = first;
    }

    static void rejectsWhileBusy(Fixture fixture, Outcome outcome) {
        rejected(outcome.error(), 409, "another wallpaper is being applied");
        fixture.catalog().release.countDown();
        assertThat(fixture.catalog().first.join()).isEqualTo(fixture.expectedFile());
        assertThat(fixture.desktop().apply("star-trek", "enterprise")).isEqualTo(fixture.expectedFile());
    }

    static Consumer<Fixture> arrange(Consumer<Fixture> arrangement) {
        return arrangement;
    }

    static BiConsumer<Fixture, Outcome> expect(BiConsumer<Fixture, Outcome> expectation) {
        return expectation;
    }

    static void rejected(Throwable error, int status, String reasonFragment) {
        assertThat(error)
                .isInstanceOf(WebApplicationException.class)
                .hasMessageContaining(reasonFragment);
        var response = ((WebApplicationException) error).getResponse();
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(((JsonObject) response.getEntity()).getString("reason")).isEqualTo(error.getMessage());
    }

    static DesktopPicture.Execution record(List<List<String>> commands, List<String> command) {
        commands.add(command);
        return new DesktopPicture.Execution(0, "");
    }

    static String content(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    static void serve(HttpExchange exchange, List<String> downloads) throws IOException {
        var path = exchange.getRequestURI().getPath();
        downloads.add(path);
        if (!path.equals("/full/enterprise.jpg")) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }
        var body = IMAGE.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
