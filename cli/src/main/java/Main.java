/// `wallpi` command: starts the installed wallpi server in the background and opens it in the browser,
/// or stops it. The server and the frontend live in the install home, `~/.local/share/wallpi` unless
/// `WALLPI_HOME` points elsewhere.

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

String version = "2026-11-05.1";

static final int PORT = 9447;
static final String URL = "http://localhost:" + PORT;
static final Duration START_TIMEOUT = Duration.ofSeconds(30);
static final Duration STOP_GRACE = Duration.ofSeconds(10);

void main(String... args) throws Exception {
    var command = args.length == 0 ? "start" : args[0];
    switch (command) {
        case "start" -> start();
        case "stop" -> stop();
        case "status" -> status();
        case "--version", "version" -> IO.println("wallpi " + version);
        default -> usage();
    }
}

Path home() {
    var configured = System.getenv("WALLPI_HOME");
    return configured != null
            ? Path.of(configured)
            : Path.of(System.getProperty("user.home"), ".local", "share", "wallpi");
}

void start() throws Exception {
    if (listening()) {
        IO.println("wallpi is already running on " + URL);
        open();
        return;
    }
    var home = home();
    var server = home.resolve("server/quarkus-run.jar");
    if (!Files.exists(server)) {
        IO.println("wallpi is not installed in " + home + ", run install.sh");
        System.exit(1);
    }
    var log = home.resolve("wallpi.log");
    new ProcessBuilder("java", "-Dwallpi.app=" + home.resolve("app"), "-jar", server.toString())
            .directory(home.toFile())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .redirectInput(ProcessBuilder.Redirect.from(Path.of("/dev/null").toFile()))
            .start();
    IO.print("starting wallpi");
    var deadline = Instant.now().plus(START_TIMEOUT);
    while (!listening()) {
        if (Instant.now().isAfter(deadline)) {
            IO.println("\nwallpi did not start within " + START_TIMEOUT.toSeconds() + "s, see " + log);
            System.exit(1);
        }
        IO.print(".");
        Thread.sleep(500);
    }
    IO.println("\nwallpi is running on " + URL);
    open();
}

/// Stops every wallpi server process, never other programs on the port.
void stop() throws InterruptedException {
    var servers = ProcessHandle.allProcesses()
            .filter(this::isWallpi)
            .toList();
    if (servers.isEmpty()) {
        IO.println("wallpi is not running");
        return;
    }
    servers.forEach(process -> {
        IO.println("stopping wallpi (pid " + process.pid() + ")");
        process.destroy();
    });
    var deadline = Instant.now().plus(STOP_GRACE);
    while (servers.stream().anyMatch(ProcessHandle::isAlive) && Instant.now().isBefore(deadline))
        Thread.sleep(250);
    servers.stream()
            .filter(ProcessHandle::isAlive)
            .forEach(process -> {
                IO.println("wallpi (pid " + process.pid() + ") ignored the stop request, killing it");
                process.destroyForcibly();
            });
}

void status() {
    IO.println(listening() ? "wallpi is running on " + URL : "wallpi is not running");
}

boolean isWallpi(ProcessHandle process) {
    return process.info().commandLine()
            .filter(line -> line.contains("quarkus-run.jar"))
            .filter(line -> line.contains("wallpi"))
            .isPresent();
}

boolean listening() {
    try (var socket = new Socket()) {
        socket.connect(new InetSocketAddress("127.0.0.1", PORT), 200);
        return true;
    } catch (IOException notListening) {
        return false;
    }
}

void open() throws IOException {
    new ProcessBuilder("open", URL).inheritIO().start();
}

void usage() {
    IO.println("""
            usage: wallpi [start|stop|status|version]
              start    start the server in the background and open it in the browser (default)
              stop     stop the running server
              status   tell whether the server is running
              version  print the version""");
}
