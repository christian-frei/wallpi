package ch.survalit.wallpi.desktop.control;

import ch.survalit.wallpi.wallpapers.control.Reasons;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/// Sets the desktop picture through System Events, one desktop per connected screen.
/// `tell every desktop to set picture` only reaches the first screen, hence the explicit loop.
@ApplicationScoped
public class DesktopPicture {

    public record Execution(int exitCode, String error) {

        boolean failed() {
            return exitCode != 0;
        }
    }

    Function<List<String>, Execution> shell = DesktopPicture::execute;

    public void set(Path picture) {
        var execution = shell.apply(command(picture));
        if (execution.failed()) {
            var reason = "macOS refused to set the desktop picture: " + execution.error().strip();
            throw new InternalServerErrorException(reason, Reasons.of(Status.INTERNAL_SERVER_ERROR, reason));
        }
    }

    static List<String> command(Path picture) {
        var script = """
                tell application "System Events"
                    repeat with eachDesktop in desktops
                        set picture of eachDesktop to POSIX file "%s"
                    end repeat
                end tell
                """.formatted(quoted(picture)).strip();
        return List.of("osascript", "-e", script);
    }

    static String quoted(Path picture) {
        return picture.toAbsolutePath().toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    static Execution execute(List<String> command) {
        try {
            var process = new ProcessBuilder(command).start();
            var error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            return new Execution(process.waitFor(), error);
        } catch (IOException e) {
            return new Execution(-1, e.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Execution(-1, "interrupted");
        }
    }
}
