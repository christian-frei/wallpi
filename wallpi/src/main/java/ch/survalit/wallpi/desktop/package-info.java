/// # Desktop
/// > Put a chosen wallpaper's largest original on every screen and in every Space of this Mac.
///
/// ## Boundary
/// - `apply-wallpaper` — fetch a wallpaper's original and set it as the desktop picture on all screens in every Space
///
/// ## Requirements
/// ### R1: Apply a wallpaper
/// - R1.1 — When a wallpaper is applied, the BC shall resolve its original through `wallpapers` and store it in `~/Pictures/wallpi` under a file name unique to its provider and wallpaper id.
/// - R1.2 — If `~/Pictures/wallpi` does not exist, then the BC shall create it before storing the original.
/// - R1.3 — When a wallpaper is applied whose original is already stored, the BC shall reuse the stored file without downloading it again.
/// - R1.4 — When the original is stored, the BC shall set it as the desktop picture on every connected screen, in every Space of each screen, and report the applied file.
/// - R1.5 — If downloading the original fails, then the BC shall reject the request, keep the current desktop picture and leave no partial file behind.
/// - R1.6 — If macOS refuses to set the desktop picture on a screen or in a Space, then the BC shall reject the request with the reason macOS gave and keep the stored file.
/// - R1.7 — While a wallpaper is being applied, when another apply is requested, the BC shall reject it as busy.
///
/// ## Out of scope
/// - Choosing individual screens — every connected screen gets the same picture.
/// - Deleting stored originals.
/// - Scheduled or rotating wallpapers.
package ch.survalit.wallpi.desktop;
