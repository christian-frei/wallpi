package ch.survalit.wallpi.wallpapers.control;

import ch.survalit.wallpi.wallpapers.entity.Provider;
import ch.survalit.wallpi.wallpapers.entity.Source;
import ch.survalit.wallpi.wallpapers.entity.Wallpaper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import module java.net.http;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/// [Unsplash API](https://unsplash.com/documentation) — attribution and download tracking follow the
/// [API guidelines](https://help.unsplash.com/en/articles/2511245-unsplash-api-guidelines).
@ApplicationScoped
public class Unsplash {

    static final String ACCESS_KEY = "accessKey";

    @ConfigProperty(name = "wallpi.unsplash.uri", defaultValue = "https://api.unsplash.com")
    URI base;

    /// Read on every request, so a key added or changed by hand works without a restart.
    @ConfigProperty(name = "wallpi.unsplash.key-file")
    Path keyFile;

    public Wallpaper.Page page(Source source, int number) {
        var query = ImageApi.append(ImageApi.query(source.url(), "page"), "page=" + number);
        var uri = base.resolve(source.url().getRawPath() + "?" + query);
        var response = ImageApi.send(authorized(uri), source.title());
        var json = ImageApi.json(response, source.title());
        if (json instanceof JsonObject search) {
            return new Wallpaper.Page(number, wallpapers(search.getJsonArray("results")), number < search.getInt("total_pages", number));
        }
        if (json instanceof JsonArray photos) {
            return new Wallpaper.Page(number, wallpapers(photos), hasNextLink(response));
        }
        throw ImageApi.badGateway("source '%s' answered with unexpected JSON".formatted(source.title()));
    }

    public Wallpaper original(Source source, String id) {
        var uri = base.resolve("/photos/" + URLEncoder.encode(id, StandardCharsets.UTF_8));
        var response = ImageApi.send(authorized(uri), source.title());
        if (ImageApi.isNotFound(response)) {
            var reason = "wallpaper %s is unknown at Unsplash".formatted(id);
            throw new NotFoundException(reason, Reasons.of(Status.NOT_FOUND, reason));
        }
        var photo = ImageApi.json(response, source.title()).asJsonObject();
        registerDownload(source, URI.create(photo.getJsonObject("links").getString("download_location")));
        return wallpaper(photo);
    }

    void registerDownload(Source source, URI downloadLocation) {
        ImageApi.json(ImageApi.send(authorized(downloadLocation), source.title()), source.title());
    }

    HttpRequest authorized(URI uri) {
        return ImageApi.request(uri)
                .header("Authorization", "Client-ID " + key())
                .header("Accept-Version", "v1")
                .build();
    }

    String key() {
        if (Files.notExists(keyFile)) {
            throw unavailable("Unsplash access key is missing: create %s with {\"accessKey\": \"…\"}".formatted(keyFile));
        }
        try (var reader = Json.createReader(new StringReader(Files.readString(keyFile)))) {
            if (!(reader.readValue() instanceof JsonObject json)) {
                throw unavailable("Unsplash key file %s is not valid JSON: expected an object".formatted(keyFile));
            }
            var key = json.getString(ACCESS_KEY, "").strip();
            if (key.isEmpty()) {
                throw unavailable("Unsplash key file %s has no accessKey".formatted(keyFile));
            }
            return key;
        } catch (JsonException e) {
            throw unavailable("Unsplash key file %s is not valid JSON: %s".formatted(keyFile, e.getMessage()));
        } catch (IOException e) {
            throw unavailable("Unsplash key file %s cannot be read: %s".formatted(keyFile, e));
        }
    }

    static ServiceUnavailableException unavailable(String reason) {
        return new ServiceUnavailableException(reason, Reasons.of(Status.SERVICE_UNAVAILABLE, reason));
    }

    static boolean hasNextLink(HttpResponse<String> response) {
        return response.headers().firstValue("Link")
                .filter(link -> link.contains("rel=\"next\""))
                .isPresent();
    }

    static List<Wallpaper> wallpapers(JsonArray photos) {
        return photos.getValuesAs(JsonObject.class).stream()
                .map(Unsplash::wallpaper)
                .toList();
    }

    static Wallpaper wallpaper(JsonObject photo) {
        var urls = photo.getJsonObject("urls");
        var user = photo.getJsonObject("user");
        return Wallpaper.attributed(
                photo.getString("id"),
                Provider.UNSPLASH,
                URI.create(urls.getString("small")),
                URI.create(urls.getString("raw")),
                photo.getInt("width"),
                photo.getInt("height"),
                new Wallpaper.Attribution(user.getString("name"), URI.create(user.getJsonObject("links").getString("html"))));
    }
}
