import BElement from "../../BElement.js";
import { html } from "lit-html";
import { applyWallpaper, filterBySeason, keyOf, loadMore, SEASONS } from "../control/GalleryControl.js";

const UNSPLASH_REFERRAL = "utm_source=wallpi&utm_medium=referral";
const SEASON_CHOICES = [
    { value: "", label: "All seasons" },
    ...SEASONS.map(season => ({ value: season, label: season[0].toUpperCase() + season.slice(1) }))
];

class GallerySection extends BElement {

    extractState({ gallery: { sources, sections, applying, applied, failures } }) {
        const id = this.getAttribute("source-id");
        return {
            source: sources.find(source => source.id === id),
            section: sections[id],
            applying,
            applied,
            failures
        };
    }

    view() {
        const { source, section } = this.state;
        const headingId = `source-${source.id}`;
        return html`
        <section aria-labelledby=${headingId} aria-busy=${section.loading}>
            <h2 id=${headingId}>${source.title}</h2>
            <p>${source.description}</p>
            ${source.seasonal ? this.seasonFilter() : ""}
            ${source.status === "valid" ? "" : html`<p role="alert" class="error">${source.reason}</p>`}
            ${section.error ? html`<p role="alert" class="error">${section.error}</p>` : ""}
            <ul class="cards">
                ${section.wallpapers.map((wallpaper, index) => this.card(wallpaper, index))}
            </ul>
            ${section.loading ? html`<p role="status" class="status">Loading wallpapers…</p>` : ""}
            ${section.hasMore && !section.loading
                ? html`<button type="button" class="more" @click=${_ => loadMore(source.id)}>Load more ${source.title} wallpapers</button>`
                : ""}
        </section>
        `;
    }

    seasonFilter() {
        const { source, section } = this.state;
        return html`
        <fieldset class="seasons">
            <legend>Season</legend>
            ${SEASON_CHOICES.map(({ value, label }) => html`
            <label>
                <input type="radio" name="season-${source.id}" value=${value}
                       .checked=${section.season === value}
                       @change=${_ => filterBySeason(source.id, value)}>
                ${label}
            </label>`)}
        </fieldset>
        `;
    }

    card(wallpaper, index) {
        const { source, applying, applied, failures } = this.state;
        const key = keyOf(source.id, wallpaper.id);
        const failure = failures[key];
        return html`
        <li>
            <figure class="card">
                <button type="button"
                        aria-label="Apply ${source.title} wallpaper ${index + 1}, ${wallpaper.resolution}"
                        aria-current=${applied === key ? "true" : "false"}
                        ?disabled=${Boolean(applying)}
                        @click=${_ => applyWallpaper(source.id, wallpaper.id)}>
                    <img src=${wallpaper.preview} alt="" loading="lazy">
                    <span class="caption">
                        <span>${wallpaper.resolution}</span>
                        <span class="mark">${this.mark(key)}</span>
                    </span>
                </button>
                ${wallpaper.photographer ? this.attribution(wallpaper.photographer) : ""}
                ${failure ? html`<p role="alert" class="error">${failure}</p>` : ""}
            </figure>
        </li>
        `;
    }

    mark(key) {
        const { applying, applied } = this.state;
        if (applying === key) return "Applying…";
        if (applied === key) return "Applied";
        return "";
    }

    attribution({ name, profile }) {
        return html`
        <figcaption>
            Photo by <a href="${profile}?${UNSPLASH_REFERRAL}">${name}</a>
            on <a href="https://unsplash.com/?${UNSPLASH_REFERRAL}">Unsplash</a>
        </figcaption>
        `;
    }
}
customElements.define("b-gallery-section", GallerySection);
