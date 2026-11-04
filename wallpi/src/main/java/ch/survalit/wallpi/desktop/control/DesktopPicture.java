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
import org.eclipse.microprofile.config.inject.ConfigProperty;

/// Sets the desktop picture through System Events, one desktop per connected screen.
/// `tell every desktop to set picture` only reaches the first screen, hence the explicit loop.
/// System Events only reaches the visible Space of each screen, so the picture is then written into
/// every Space of macOS's wallpaper store and `WallpaperAgent` restarted to show it (D8).
@ApplicationScoped
public class DesktopPicture {

    /// Replaces the desktop entry of the system default, each display and each Space of each display;
    /// screen saver entries stay as they are.
    static final String EVERY_SPACE = """
            ObjC.import("Foundation");

            function run([storePath, picturePath]) {
                const read = $.NSData.dataWithContentsOfFile(storePath);
                if (read.isNil()) throw new Error(`cannot read the wallpaper store ${storePath}`);
                const store = $.NSPropertyListSerialization.propertyListWithDataOptionsFormatError(read, $.NSPropertyListMutableContainersAndLeaves, null, null);
                const configuration = $.NSPropertyListSerialization.dataWithPropertyListFormatOptionsError(
                    $({ type: "imageFile", url: { relative: $.NSURL.fileURLWithPath(picturePath).absoluteString.js } }),
                    $.NSPropertyListBinaryFormat_v1_0, 0, null);
                const now = $.NSDate.date;
                const desktop = () => $({
                    Content: {
                        Choices: [{ Provider: "com.apple.wallpaper.choice.image", Files: [], Configuration: configuration }],
                        Shuffle: "$null",
                        EncodedOptionValues: "$null"
                    },
                    LastSet: now,
                    LastUse: now
                });
                const values = dictionary => dictionary.isNil() ? [] : ObjC.unwrap(dictionary.allValues);
                const entries = [
                    store.objectForKey("SystemDefault"),
                    ...values(store.objectForKey("Displays")),
                    ...values(store.objectForKey("Spaces")).flatMap(space => [space.objectForKey("Default"), ...values(space.objectForKey("Displays"))])
                ].filter(entry => !entry.isNil() && !entry.objectForKey("Desktop").isNil());
                entries.forEach(entry => entry.setObjectForKey(desktop(), "Desktop"));
                const written = $.NSPropertyListSerialization.dataWithPropertyListFormatOptionsError(store, $.NSPropertyListBinaryFormat_v1_0, 0, null);
                if (!written.writeToFileAtomically(storePath, true)) throw new Error(`cannot write the wallpaper store ${storePath}`);
                return `${entries.length} desktops`;
            }
            """;

    @ConfigProperty(name = "wallpi.wallpaper-store")
    Path store;

    public record Execution(int exitCode, String error) {

        boolean failed() {
            return exitCode != 0;
        }
    }

    Function<List<String>, Execution> shell = DesktopPicture::execute;

    public void set(Path picture) {
        run(visibleSpaces(picture), "macOS refused to set the desktop picture: ");
        run(everySpace(store, picture), "macOS refused to set the desktop picture in every Space: ");
        // exits non-zero when no agent runs, which then has nothing stale to show
        shell.apply(restartWallpaperAgent());
    }

    void run(List<String> command, String refusal) {
        var execution = shell.apply(command);
        if (execution.failed()) {
            var reason = refusal + execution.error().strip();
            throw new InternalServerErrorException(reason, Reasons.of(Status.INTERNAL_SERVER_ERROR, reason));
        }
    }

    static List<String> everySpace(Path store, Path picture) {
        return List.of("osascript", "-l", "JavaScript", "-e", EVERY_SPACE,
                store.toAbsolutePath().toString(), picture.toAbsolutePath().toString());
    }

    static List<String> restartWallpaperAgent() {
        return List.of("killall", "WallpaperAgent");
    }

    static List<String> visibleSpaces(Path picture) {
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
