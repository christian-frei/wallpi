package ch.survalit.wallpi.desktop.control;

import ch.survalit.wallpi.wallpapers.control.Reasons;
import ch.survalit.wallpi.wallpapers.entity.Wallpaper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.regex.Pattern;
import module java.net.http;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/// Keeps every applied original in one folder, one file per provider and wallpaper.
@ApplicationScoped
public class Pictures {

    static final Pattern EXTENSION = Pattern.compile("\\.([A-Za-z0-9]{1,5})$");
    static final String DEFAULT_EXTENSION = "jpg";
    static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(2);

    HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @ConfigProperty(name = "wallpi.pictures")
    Path folder;

    public Path store(Wallpaper wallpaper) {
        var file = fileOf(wallpaper);
        if (Files.exists(file)) {
            return file;
        }
        createFolder();
        var partial = folder.resolve(file.getFileName() + ".part");
        try {
            download(wallpaper, partial);
            return Files.move(partial, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw downloadFailed(wallpaper, e.toString());
        } finally {
            deleteQuietly(partial);
        }
    }

    Path fileOf(Wallpaper wallpaper) {
        var id = wallpaper.id().replaceAll("[^A-Za-z0-9_-]", "_");
        return folder.resolve("%s-%s.%s".formatted(wallpaper.provider().name().toLowerCase(), id, extensionOf(wallpaper)));
    }

    static String extensionOf(Wallpaper wallpaper) {
        var matcher = EXTENSION.matcher(String.valueOf(wallpaper.original().getPath()));
        return matcher.find() ? matcher.group(1).toLowerCase() : DEFAULT_EXTENSION;
    }

    void download(Wallpaper wallpaper, Path partial) throws IOException {
        var request = HttpRequest.newBuilder(wallpaper.original()).timeout(DOWNLOAD_TIMEOUT).build();
        HttpResponse<Path> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofFile(partial));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw downloadFailed(wallpaper, "interrupted");
        }
        if (response.statusCode() / 100 != 2) {
            throw downloadFailed(wallpaper, "HTTP " + response.statusCode());
        }
    }

    void createFolder() {
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            var reason = "cannot create %s: %s".formatted(folder, e);
            throw new InternalServerErrorException(reason, Reasons.of(Status.INTERNAL_SERVER_ERROR, reason));
        }
    }

    static void deleteQuietly(Path partial) {
        try {
            Files.deleteIfExists(partial);
        } catch (IOException e) {
            System.getLogger(Pictures.class.getName()).log(System.Logger.Level.WARNING, "cannot delete partial download " + partial, e);
        }
    }

    static ServerErrorException downloadFailed(Wallpaper wallpaper, String detail) {
        var reason = "downloading %s failed: %s".formatted(wallpaper.original(), detail);
        return new ServerErrorException(reason, Reasons.of(Reasons.BAD_GATEWAY, reason));
    }
}
