package ch.survalit.wallpi.desktop.control;

import ch.survalit.wallpi.wallpapers.control.Catalog;
import ch.survalit.wallpi.wallpapers.entity.Wallpaper;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/// Resolves every wallpaper to a fixed original; can hold a resolution open to keep an apply in flight.
class FakeCatalog extends Catalog {

    Wallpaper original;
    List<String> resolved = new CopyOnWriteArrayList<>();
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(0);
    CompletableFuture<Path> first;

    void awaitEntered() {
        try {
            if (!entered.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("first apply never reached the catalog");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    @Override
    public Wallpaper original(String sourceId, String wallpaperId) {
        resolved.add(sourceId + "/" + wallpaperId);
        entered.countDown();
        awaitRelease();
        return original;
    }

    void awaitRelease() {
        try {
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("apply was never released");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
