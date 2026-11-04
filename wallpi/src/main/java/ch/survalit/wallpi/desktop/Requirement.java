package ch.survalit.wallpi.desktop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Generated from the capability spec in [desktop] — do not edit.
/// Marks the boundary method or test that realizes the given requirement statements.
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Requirement {

    /// One constant per statement id in the spec's `## Requirements`.
    enum Rn {
        /// When a wallpaper is applied, the BC shall resolve its original through wallpapers and store it in ~/Pictures/wallpi under a file name unique to its provider and wallpaper id.
        R1_1("R1.1", "When a wallpaper is applied, the BC shall resolve its original through wallpapers and store it in ~/Pictures/wallpi under a file name unique to its provider and wallpaper id."),
        /// If ~/Pictures/wallpi does not exist, then the BC shall create it before storing the original.
        R1_2("R1.2", "If ~/Pictures/wallpi does not exist, then the BC shall create it before storing the original."),
        /// When a wallpaper is applied whose original is already stored, the BC shall reuse the stored file without downloading it again.
        R1_3("R1.3", "When a wallpaper is applied whose original is already stored, the BC shall reuse the stored file without downloading it again."),
        /// When the original is stored, the BC shall set it as the desktop picture on every connected screen and report the applied file.
        R1_4("R1.4", "When the original is stored, the BC shall set it as the desktop picture on every connected screen and report the applied file."),
        /// If downloading the original fails, then the BC shall reject the request, keep the current desktop picture and leave no partial file behind.
        R1_5("R1.5", "If downloading the original fails, then the BC shall reject the request, keep the current desktop picture and leave no partial file behind."),
        /// If macOS refuses to set the desktop picture, then the BC shall reject the request with the reason macOS gave and keep the stored file.
        R1_6("R1.6", "If macOS refuses to set the desktop picture, then the BC shall reject the request with the reason macOS gave and keep the stored file."),
        /// While a wallpaper is being applied, when another apply is requested, the BC shall reject it as busy.
        R1_7("R1.7", "While a wallpaper is being applied, when another apply is requested, the BC shall reject it as busy.");

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
