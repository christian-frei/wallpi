package ch.survalit.wallpi.wallpapers.control;

import ch.survalit.wallpi.wallpapers.entity.Source;
import ch.survalit.wallpi.wallpapers.entity.Wallpaper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response.Status;

@ApplicationScoped
public class Catalog {

    @Inject
    SourcesFile sources;

    @Inject
    Wallhaven wallhaven;

    @Inject
    Unsplash unsplash;

    public Wallpaper.Page page(String sourceId, int number) {
        var source = usable(sourceId);
        return switch (source.provider()) {
            case WALLHAVEN -> wallhaven.page(source, number);
            case UNSPLASH -> unsplash.page(source, number);
        };
    }

    public Wallpaper original(String sourceId, String wallpaperId) {
        var source = usable(sourceId);
        return switch (source.provider()) {
            case WALLHAVEN -> wallhaven.original(source, wallpaperId);
            case UNSPLASH -> unsplash.original(source, wallpaperId);
        };
    }

    Source usable(String sourceId) {
        var source = sources.find(sourceId);
        if (!source.isValid()) {
            var reason = "source '%s' is %s: %s".formatted(source.title(), source.status().name().toLowerCase(), source.reason());
            throw new BadRequestException(reason, Reasons.of(Status.BAD_REQUEST, reason));
        }
        return source;
    }
}
