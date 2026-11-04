/// # Wallpi
/// > Browse wallpapers from configured image APIs and put the chosen one, at its largest, on every screen of this Mac.
///
/// ## Components
/// - `desktop` may call `wallpapers`; never the reverse.
/// - `gallery` (frontend, `app/src/gallery`) reaches the server only through the `wallpapers` and `desktop` boundaries.
///
/// ## System invariants
/// - S1 — The system shall accept requests only from the local machine. _(why: it changes the desktop of the user's Mac and serves a single user)_
///
/// ## Ubiquitous language
/// - Source — a configured entry (title, description, url) whose url is a query against an image API. Owned by `wallpapers`.
/// - Provider — the image API a source queries, Wallhaven or Unsplash, told apart by the url's host.
/// - Wallpaper — one image a source yields, identified within its provider; has a preview and an original.
/// - Preview — the small image shown on a card.
/// - Original — the largest available image of a wallpaper.
/// - Season — spring, summer, autumn or winter; narrows the wallpapers of a seasonal source. Owned by `wallpapers`.
///
/// ## Decisions
/// - D1 — Wallhaven and Unsplash are the providers, told apart by the source url's host. _(why: both cover Star Trek and nature, Wallhaven without a key; rejected: NASA Image Library)_
/// - D2 — The Unsplash access key comes from the `UNSPLASH_ACCESS_KEY` environment variable. _(why: keeps the secret out of the sources file; rejected: a key field in the sources JSON)_
/// - D3 — Sources live in `~/.wallpi/sources.json`, read anew on every listing. _(why: editable without a rebuild; rejected: a sources file bundled in the app)_
/// - D4 — Every applied original is kept in `~/Pictures/wallpi`. _(why: re-applying skips the download; rejected: a temp folder holding only the current image)_
/// - D5 — The server is split into `wallpapers` and `desktop`. _(why: finding images and changing the Mac are separate responsibilities; rejected: one server BC)_
/// - D6 — The frontend lives in `app/src` and is served by the Quarkus service. _(why: keeps the web-components layout and one local origin without CORS; rejected: placing it in META-INF/resources)_
///
/// ## Stack
/// - microprofile-server (Quarkus) · base package `ch.survalit.wallpi` · service module `wallpi/`, system tests `wallpi-st/`
/// - web-components · `app/src/`, served by the Quarkus service
package ch.survalit.wallpi;
