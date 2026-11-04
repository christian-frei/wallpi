package ch.survalit.wallpi.wallpapers.boundary;

import static ch.survalit.wallpi.wallpapers.Requirement.Rn.*;

import ch.survalit.wallpi.wallpapers.Requirement;
import ch.survalit.wallpi.wallpapers.control.Catalog;
import ch.survalit.wallpi.wallpapers.control.SourcesFile;
import ch.survalit.wallpi.wallpapers.entity.Source;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("sources")
@Produces(MediaType.APPLICATION_JSON)
public class SourcesResource {

    @Inject
    SourcesFile sources;

    @Inject
    Catalog catalog;

    @GET
    @Requirement({R1_1, R1_2, R1_3, R1_4, R1_5, R1_6, R1_7})
    public Response listSources() {
        var listed = sources.all().stream()
                .map(Source::toJSON)
                .toList();
        return Response.ok(Json.createArrayBuilder(listed).build()).build();
    }

    @GET
    @Path("{sourceId}/wallpapers")
    @Requirement({R2_1, R2_2, R2_3, R2_4, R2_5, R2_6, R2_7})
    public Response listWallpapers(@PathParam("sourceId") String sourceId, @QueryParam("page") @DefaultValue("1") int page) {
        return Response.ok(catalog.page(sourceId, page).toJSON()).build();
    }

    @GET
    @Path("{sourceId}/wallpapers/{wallpaperId}/original")
    @Requirement({R3_1, R3_2, R3_3})
    public Response resolveOriginal(@PathParam("sourceId") String sourceId, @PathParam("wallpaperId") String wallpaperId) {
        return Response.ok(catalog.original(sourceId, wallpaperId).originalJSON()).build();
    }
}
