package ch.survalit.wallpi.wallpapers.entity;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

public enum Provider {
    WALLHAVEN("wallhaven.cc", "q"),
    UNSPLASH("api.unsplash.com", "query");

    final String host;
    final String searchParameter;

    Provider(String host, String searchParameter) {
        this.host = host;
        this.searchParameter = searchParameter;
    }

    public static Optional<Provider> of(URI url) {
        return Stream.of(values())
                .filter(provider -> provider.host.equalsIgnoreCase(url.getHost()))
                .findFirst();
    }
}
