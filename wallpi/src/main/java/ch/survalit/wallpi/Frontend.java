package ch.survalit.wallpi;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.nio.file.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/// Serves `app/src` straight from the file system — the frontend has no build step, so edits show on reload.
/// Paths without a file fall through to the JAX-RS resources.
@ApplicationScoped
public class Frontend {

    @ConfigProperty(name = "wallpi.app")
    Path root;

    void serve(@Observes Router router) {
        var files = StaticHandler.create(FileSystemAccess.ROOT, root.toAbsolutePath().normalize().toString())
                .setCachingEnabled(false)
                .setIndexPage("index.html");
        router.route("/*").handler(files);
    }
}
