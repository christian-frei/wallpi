package ch.survalit.wallpi.wallpapers.control;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.json.JsonObject;
import jakarta.ws.rs.WebApplicationException;

interface Fixtures {

    String SOURCES = """
            [
              {"title": "Star Trek", "description": "The final frontier",
               "url": "https://wallhaven.cc/api/v1/search?q=star%20trek&atleast=3840x2160&purity=111&page=7"},
              {"title": "Unsplash Nature", "description": "Forests and fjords",
               "url": "https://api.unsplash.com/search/photos?query=nature&per_page=2", "seasonal": true},
              {"title": "No Url", "description": "forgot the url"},
              {"description": "forgot the title", "url": "https://wallhaven.cc/api/v1/search?q=borg"},
              {"title": "Elsewhere", "description": "not an image API we know",
               "url": "https://example.com/api?q=tribbles"},
              {"title": "Nature", "description": "Wallhaven landscapes",
               "url": "https://wallhaven.cc/api/v1/search?atleast=3840x2160&q=nature", "seasonal": true},
              {"title": "Bad Flag", "description": "seasonal is not a boolean",
               "url": "https://wallhaven.cc/api/v1/search?q=risa", "seasonal": "yes"}
            ]
            """;

    static void rejected(Throwable error, int status, String reasonFragment) {
        assertThat(error)
                .isInstanceOf(WebApplicationException.class)
                .hasMessageContaining(reasonFragment);
        var response = ((WebApplicationException) error).getResponse();
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(((JsonObject) response.getEntity()).getString("reason")).isEqualTo(error.getMessage());
    }
}
