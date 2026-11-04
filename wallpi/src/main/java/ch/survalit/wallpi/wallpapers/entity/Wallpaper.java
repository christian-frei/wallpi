package ch.survalit.wallpi.wallpapers.entity;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.net.URI;
import java.util.List;

public record Wallpaper(String id, Provider provider, URI preview, URI original, int width, int height, Attribution attribution) {

    public record Attribution(String name, URI profile) {

        JsonObject toJSON() {
            return Json.createObjectBuilder()
                    .add("name", name)
                    .add("profile", profile.toString())
                    .build();
        }
    }

    public record Page(int number, List<Wallpaper> wallpapers, boolean hasMore) {

        public JsonObject toJSON() {
            return Json.createObjectBuilder()
                    .add("page", number)
                    .add("hasMore", hasMore)
                    .add("wallpapers", Json.createArrayBuilder(wallpapers.stream().map(Wallpaper::toJSON).toList()))
                    .build();
        }
    }

    public static Wallpaper of(String id, Provider provider, URI preview, URI original, int width, int height) {
        return new Wallpaper(id, provider, preview, original, width, height, null);
    }

    public static Wallpaper attributed(String id, Provider provider, URI preview, URI original, int width, int height, Attribution attribution) {
        return new Wallpaper(id, provider, preview, original, width, height, attribution);
    }

    public String resolution() {
        return "%dx%d".formatted(width, height);
    }

    public JsonObject toJSON() {
        var builder = Json.createObjectBuilder()
                .add("id", id)
                .add("provider", provider.name().toLowerCase())
                .add("preview", preview.toString())
                .add("width", width)
                .add("height", height)
                .add("resolution", resolution());
        if (attribution != null) {
            builder.add("photographer", attribution.toJSON());
        }
        return builder.build();
    }

    public JsonObject originalJSON() {
        return Json.createObjectBuilder()
                .add("id", id)
                .add("provider", provider.name().toLowerCase())
                .add("location", original.toString())
                .add("width", width)
                .add("height", height)
                .add("resolution", resolution())
                .build();
    }
}
