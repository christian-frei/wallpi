package ch.survalit.wallpi.desktop.control;

import ch.survalit.wallpi.wallpapers.control.Catalog;
import ch.survalit.wallpi.wallpapers.control.Reasons;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response.Status;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class Desktop {

    static final String BUSY = "another wallpaper is being applied";

    @Inject
    Catalog catalog;

    @Inject
    Pictures pictures;

    @Inject
    DesktopPicture desktopPicture;

    AtomicBoolean applying = new AtomicBoolean();

    public Path apply(String sourceId, String wallpaperId) {
        if (!applying.compareAndSet(false, true)) {
            throw new ClientErrorException(BUSY, Reasons.of(Status.CONFLICT, BUSY));
        }
        try {
            var stored = pictures.store(catalog.original(sourceId, wallpaperId));
            desktopPicture.set(stored);
            return stored;
        } finally {
            applying.set(false);
        }
    }
}
