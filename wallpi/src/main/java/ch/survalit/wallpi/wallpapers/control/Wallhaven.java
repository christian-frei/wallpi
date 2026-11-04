package ch.survalit.wallpi.wallpapers.control;

import ch.survalit.wallpi.wallpapers.entity.Provider;
import ch.survalit.wallpi.wallpapers.entity.Source;
import ch.survalit.wallpi.wallpapers.entity.Wallpaper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonObject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response.Status;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/// [Wallhaven API v1](https://wallhaven.cc/help/api)
@ApplicationScoped
public class Wallhaven {

    static final String SAFE_FOR_WORK = "sfw";

    @ConfigProperty(name = "wallpi.wallhaven.uri", defaultValue = "https://wallhaven.cc")
    URI base;

    public Wallpaper.Page page(Source source, int number) {
        var query = ImageApi.append(ImageApi.query(source.url(), "purity", "page"), "purity=100&page=" + number);
        var uri = base.resolve(source.url().getRawPath() + "?" + query);
        var response = ImageApi.send(ImageApi.request(uri).build(), source.title());
        var json = ImageApi.json(response, source.title()).asJsonObject();
        var wallpapers = json.getJsonArray("data").getValuesAs(JsonObject.class).stream()
                .filter(Wallhaven::isSafeForWork)
                .map(Wallhaven::wallpaper)
                .toList();
        var meta = json.getJsonObject("meta");
        return new Wallpaper.Page(number, wallpapers, number < meta.getInt("last_page"));
    }

    public Wallpaper original(Source source, String id) {
        var uri = base.resolve("/api/v1/w/" + URLEncoder.encode(id, StandardCharsets.UTF_8));
        var response = ImageApi.send(ImageApi.request(uri).build(), source.title());
        if (ImageApi.isNotFound(response)) {
            var reason = "wallpaper %s is unknown at Wallhaven".formatted(id);
            throw new NotFoundException(reason, Reasons.of(Status.NOT_FOUND, reason));
        }
        return wallpaper(ImageApi.json(response, source.title()).asJsonObject().getJsonObject("data"));
    }

    static boolean isSafeForWork(JsonObject wallpaper) {
        return SAFE_FOR_WORK.equals(wallpaper.getString("purity", ""));
    }

    static Wallpaper wallpaper(JsonObject json) {
        return Wallpaper.of(
                json.getString("id"),
                Provider.WALLHAVEN,
                URI.create(json.getJsonObject("thumbs").getString("large")),
                URI.create(json.getString("path")),
                json.getInt("dimension_x"),
                json.getInt("dimension_y"));
    }
}
