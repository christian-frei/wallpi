package ch.survalit.wallpi.wallpapers.entity;

import java.util.Optional;
import java.util.stream.Stream;

public enum Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    public static Optional<Season> of(String name) {
        return Stream.of(values())
                .filter(season -> season.term().equals(name))
                .findFirst();
    }

    public String term() {
        return name().toLowerCase();
    }
}
