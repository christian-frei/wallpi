package ch.survalit.wallpi.wallpapers.entity;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record Source(String id, String title, String description, URI url, Provider provider, Status status, String reason) {

    public enum Status { VALID, INVALID, UNSUPPORTED }

    static final int ID_BYTES = 6;

    public static Source fromJSON(JsonValue value) {
        if (!(value instanceof JsonObject json)) {
            return invalid(idOf(value.toString()), "", "", "entry is not a JSON object");
        }
        var title = json.getString("title", "").strip();
        var description = json.getString("description", "").strip();
        var url = json.getString("url", "").strip();
        var id = idOf(title + '\n' + url);
        if (title.isEmpty()) {
            return invalid(id, title, description, "missing title");
        }
        if (url.isEmpty()) {
            return invalid(id, title, description, "missing url");
        }
        return fromURL(id, title, description, url);
    }

    static Source fromURL(String id, String title, String description, String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return invalid(id, title, description, "url is malformed: " + e.getMessage());
        }
        if (!uri.isAbsolute()) {
            return invalid(id, title, description, "url is not absolute: " + url);
        }
        return Provider.of(uri)
                .map(provider -> new Source(id, title, description, uri, provider, Status.VALID, ""))
                .orElseGet(() -> new Source(id, title, description, uri, null, Status.UNSUPPORTED,
                        "unsupported host " + uri.getHost() + ", expected wallhaven.cc or api.unsplash.com"));
    }

    static Source invalid(String id, String title, String description, String reason) {
        return new Source(id, title, description, null, null, Status.INVALID, reason);
    }

    /// Hashing the entry keeps ids stable across reads of an unchanged file, independent of position.
    static String idOf(String content) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, ID_BYTES);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is mandatory on every Java platform", e);
        }
    }

    public boolean isValid() {
        return status == Status.VALID;
    }

    public JsonObject toJSON() {
        var builder = Json.createObjectBuilder()
                .add("id", id)
                .add("title", title)
                .add("description", description)
                .add("status", status.name().toLowerCase());
        if (url != null) {
            builder.add("url", url.toString());
        }
        if (!reason.isEmpty()) {
            builder.add("reason", reason);
        }
        return builder.build();
    }
}
