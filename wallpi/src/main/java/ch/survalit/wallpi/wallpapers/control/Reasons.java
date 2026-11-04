package ch.survalit.wallpi.wallpapers.control;

import jakarta.json.Json;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/// Rejections carry their reason as a JSON body, so the gallery can show it in place.
public interface Reasons {

    int BAD_GATEWAY = 502;

    static Response of(Status status, String reason) {
        return of(status.getStatusCode(), reason);
    }

    static Response of(int status, String reason) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Json.createObjectBuilder().add("reason", reason).build())
                .build();
    }
}
