package ch.survalit.wallpi.wallpapers.control;

import ch.survalit.wallpi.wallpapers.entity.Season;
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

    /// An empty `season` lists the source unfiltered.
    public Wallpaper.Page page(String sourceId, int number, String season) {
        var source = narrowed(usable(sourceId), season);
        return switch (source.provider()) {
            case WALLHAVEN -> wallhaven.page(source, number);
            case UNSPLASH -> unsplash.page(source, number);
        };
    }

    static Source narrowed(Source source, String season) {
        if (season.isEmpty()) {
            return source;
        }
        var known = Season.of(season).orElseThrow(() -> badRequest("unknown season %s, expected spring, summer, autumn or winter".formatted(season)));
        if (!source.seasonal()) {
            throw badRequest("source '%s' is not seasonal".formatted(source.title()));
        }
        return source.narrowedTo(known);
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
            throw badRequest("source '%s' is %s: %s".formatted(source.title(), source.status().name().toLowerCase(), source.reason()));
        }
        return source;
    }

    static BadRequestException badRequest(String reason) {
        return new BadRequestException(reason, Reasons.of(Status.BAD_REQUEST, reason));
    }
}
