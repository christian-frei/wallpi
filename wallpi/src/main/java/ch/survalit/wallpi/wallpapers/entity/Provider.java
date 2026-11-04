package ch.survalit.wallpi.wallpapers.entity;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

public enum Provider {
    WALLHAVEN("wallhaven.cc"),
    UNSPLASH("api.unsplash.com");

    final String host;

    Provider(String host) {
        this.host = host;
    }

    public static Optional<Provider> of(URI url) {
        return Stream.of(values())
                .filter(provider -> provider.host.equalsIgnoreCase(url.getHost()))
                .findFirst();
    }
}
