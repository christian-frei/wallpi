package ch.survalit.wallpi.wallpapers.control;

import ch.survalit.wallpi.wallpapers.entity.Source;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SourcesFile {

    static final String DEFAULT_SOURCES = """
            [
              {
                "title": "Star Trek",
                "description": "Starships, bridges and the final frontier",
                "url": "https://wallhaven.cc/api/v1/search?q=star%20trek&atleast=2560x1440&sorting=relevance"
              },
              {
                "title": "Nature",
                "description": "Landscapes, forests and mountains",
                "url": "https://wallhaven.cc/api/v1/search?q=nature&atleast=3840x2160&sorting=relevance",
                "seasonal": true
              },
              {
                "title": "Nature on Unsplash",
                "description": "Landscape photography from Unsplash",
                "url": "https://api.unsplash.com/search/photos?query=nature%20landscape&orientation=landscape&per_page=24",
                "seasonal": true
              }
            ]
            """;

    @ConfigProperty(name = "wallpi.sources")
    Path file;

    public List<Source> all() {
        if (Files.notExists(file)) {
            createDefault();
        }
        try (var reader = Json.createReader(new StringReader(read()))) {
            if (!(reader.readValue() instanceof JsonArray sources)) {
                throw invalid("expected a JSON array of sources");
            }
            return sources.stream()
                    .map(Source::fromJSON)
                    .toList();
        } catch (JsonException e) {
            throw invalid(e.getMessage());
        }
    }

    public Source find(String id) {
        return all().stream()
                .filter(source -> source.id().equals(id))
                .findFirst()
                .orElseThrow(() -> unknown(id));
    }

    String read() {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw invalid("cannot be read: " + e);
        }
    }

    void createDefault() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, DEFAULT_SOURCES);
        } catch (IOException e) {
            throw invalid("cannot be created: " + e);
        }
    }

    InternalServerErrorException invalid(String detail) {
        var reason = "sources file %s is not valid JSON: %s".formatted(file, detail);
        return new InternalServerErrorException(reason, Reasons.of(Status.INTERNAL_SERVER_ERROR, reason));
    }

    static NotFoundException unknown(String id) {
        var reason = "unknown source " + id;
        return new NotFoundException(reason, Reasons.of(Status.NOT_FOUND, reason));
    }
}
