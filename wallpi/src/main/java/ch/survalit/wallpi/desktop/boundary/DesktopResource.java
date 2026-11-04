package ch.survalit.wallpi.desktop.boundary;

import static ch.survalit.wallpi.desktop.Requirement.Rn.*;

import ch.survalit.wallpi.desktop.Requirement;
import ch.survalit.wallpi.desktop.control.Desktop;
import ch.survalit.wallpi.wallpapers.control.Reasons;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("desktop")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DesktopResource {

    static final String MISSING_IDS = "sourceId and wallpaperId are required";

    @Inject
    Desktop desktop;

    @PUT
    @Path("wallpaper")
    @Requirement({R1_1, R1_2, R1_3, R1_4, R1_5, R1_6, R1_7})
    public Response applyWallpaper(JsonObject wallpaper) {
        if (wallpaper == null || !wallpaper.containsKey("sourceId") || !wallpaper.containsKey("wallpaperId")) {
            throw new BadRequestException(MISSING_IDS, Reasons.of(Status.BAD_REQUEST, MISSING_IDS));
        }
        var file = desktop.apply(wallpaper.getString("sourceId"), wallpaper.getString("wallpaperId"));
        return Response.ok(Json.createObjectBuilder().add("file", file.toString()).build()).build();
    }
}
