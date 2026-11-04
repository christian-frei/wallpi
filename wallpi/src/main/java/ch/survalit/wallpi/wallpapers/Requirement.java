package ch.survalit.wallpi.wallpapers;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Generated from the capability spec in [wallpapers] — do not edit.
/// Marks the boundary method or test that realizes the given requirement statements.
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Requirement {

    /// One constant per statement id in the spec's `## Requirements`.
    enum Rn {
        /// When sources are listed, the BC shall read them from ~/.wallpi/sources.json and return each with its id, title, description and url, in file order.
        R1_1("R1.1", "When sources are listed, the BC shall read them from ~/.wallpi/sources.json and return each with its id, title, description and url, in file order."),
        /// When sources are listed, the BC shall read the file anew, so that edits take effect without a restart.
        R1_2("R1.2", "When sources are listed, the BC shall read the file anew, so that edits take effect without a restart."),
        /// The BC shall give each source an id that stays the same across listings of an unchanged file.
        R1_3("R1.3", "The BC shall give each source an id that stays the same across listings of an unchanged file."),
        /// If the sources file does not exist, then the BC shall create it with default sources for Star Trek and nature and return those.
        R1_4("R1.4", "If the sources file does not exist, then the BC shall create it with default sources for Star Trek and nature and return those."),
        /// If the sources file is not valid JSON, then the BC shall reject the listing with a reason naming the file.
        R1_5("R1.5", "If the sources file is not valid JSON, then the BC shall reject the listing with a reason naming the file."),
        /// If a source entry lacks a title or a url, then the BC shall return that entry marked invalid with the reason, and still return all other sources.
        R1_6("R1.6", "If a source entry lacks a title or a url, then the BC shall return that entry marked invalid with the reason, and still return all other sources."),
        /// If a source url's host is neither Wallhaven nor Unsplash, then the BC shall return that source marked unsupported, and still return all other sources.
        R1_7("R1.7", "If a source url's host is neither Wallhaven nor Unsplash, then the BC shall return that source marked unsupported, and still return all other sources."),
        /// When sources are listed, the BC shall report for each source whether it is seasonal, not seasonal unless the entry says so.
        R1_8("R1.8", "When sources are listed, the BC shall report for each source whether it is seasonal, not seasonal unless the entry says so."),
        /// If a source entry's seasonal flag is neither true nor false, then the BC shall return that entry marked invalid with the reason, and still return all other sources.
        R1_9("R1.9", "If a source entry's seasonal flag is neither true nor false, then the BC shall return that entry marked invalid with the reason, and still return all other sources."),
        /// When the BC creates the default sources file, it shall mark the nature sources seasonal.
        R1_10("R1.10", "When the BC creates the default sources file, it shall mark the nature sources seasonal."),
        /// When the wallpapers of a source are requested, the BC shall return the first page of the provider's results, each with a wallpaper id, a preview image and the original's resolution.
        R2_1("R2.1", "When the wallpapers of a source are requested, the BC shall return the first page of the provider's results, each with a wallpaper id, a preview image and the original's resolution."),
        /// When a further page of a source is requested, the BC shall return that page of the same query together with whether another page exists.
        R2_2("R2.2", "When a further page of a source is requested, the BC shall return that page of the same query together with whether another page exists."),
        /// When a Wallhaven source is queried, the BC shall keep the source url's query parameters and return only wallpapers rated safe for work.
        R2_3("R2.3", "When a Wallhaven source is queried, the BC shall keep the source url's query parameters and return only wallpapers rated safe for work."),
        /// When an Unsplash source is queried, the BC shall authenticate with the access key from UNSPLASH_ACCESS_KEY and return each wallpaper with the photographer's name and profile link.
        R2_4("R2.4", "When an Unsplash source is queried, the BC shall authenticate with the access key from UNSPLASH_ACCESS_KEY and return each wallpaper with the photographer's name and profile link."),
        /// If no Unsplash access key is configured, then the BC shall reject wallpaper requests for Unsplash sources with a reason naming the missing key, and keep serving Wallhaven sources.
        R2_5("R2.5", "If no Unsplash access key is configured, then the BC shall reject wallpaper requests for Unsplash sources with a reason naming the missing key, and keep serving Wallhaven sources."),
        /// If the provider is unreachable or answers with an error, then the BC shall reject the request with a reason naming the source.
        R2_6("R2.6", "If the provider is unreachable or answers with an error, then the BC shall reject the request with a reason naming the source."),
        /// If the requested source does not exist or is invalid or unsupported, then the BC shall reject the request.
        R2_7("R2.7", "If the requested source does not exist or is invalid or unsupported, then the BC shall reject the request."),
        /// When the wallpapers of a seasonal source are requested for a season, the BC shall add the season's name to the source's search terms.
        R2_8("R2.8", "When the wallpapers of a seasonal source are requested for a season, the BC shall add the season's name to the source's search terms."),
        /// If a season is requested for a source that is not seasonal, then the BC shall reject the request.
        R2_9("R2.9", "If a season is requested for a source that is not seasonal, then the BC shall reject the request."),
        /// If the requested season is not spring, summer, autumn or winter, then the BC shall reject the request.
        R2_10("R2.10", "If the requested season is not spring, summer, autumn or winter, then the BC shall reject the request."),
        /// When the original of a Wallhaven wallpaper is resolved, the BC shall return the location and resolution of its full-resolution image.
        R3_1("R3.1", "When the original of a Wallhaven wallpaper is resolved, the BC shall return the location and resolution of its full-resolution image."),
        /// When the original of an Unsplash wallpaper is resolved, the BC shall return the location and resolution of its raw image and register the download with Unsplash.
        R3_2("R3.2", "When the original of an Unsplash wallpaper is resolved, the BC shall return the location and resolution of its raw image and register the download with Unsplash."),
        /// If the provider does not know the wallpaper, then the BC shall reject the resolution.
        R3_3("R3.3", "If the provider does not know the wallpaper, then the BC shall reject the resolution.");

        private final String id;
        private final String statement;

        Rn(String id, String statement) {
            this.id = id;
            this.statement = statement;
        }

        public String statement() {
            return statement;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    Rn[] value();
}
