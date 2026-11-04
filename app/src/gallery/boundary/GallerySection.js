import BElement from "../../BElement.js";
import { html } from "lit-html";
import {
    filterByResolution, filterBySeason, keyOf, loadMore, MIN_WIDTHS, openPreview, SEASONS, shownWallpapers
} from "../control/GalleryControl.js";

export const UNSPLASH_REFERRAL = "utm_source=wallpi&utm_medium=referral";
const SEASON_CHOICES = [
    { value: "", label: "All seasons" },
    ...SEASONS.map(season => ({ value: season, label: season[0].toUpperCase() + season.slice(1) }))
];

const count = shown => shown === 1 ? "1 wallpaper" : `${shown} wallpapers`;

class GallerySection extends BElement {

    static observedAttributes = ["source-id"];

    // choosing another source reuses this element with a new id
    attributeChangedCallback() {
        if (this.isConnected) this.triggerViewUpdate();
    }

    extractState({ gallery: { sources, sections, minWidth, applying, applied, failures } }) {
        const id = this.getAttribute("source-id");
        const section = sections[id];
        return {
            source: sources.find(source => source.id === id),
            section,
            shown: shownWallpapers(section, minWidth),
            minWidth,
            applying,
            applied,
            failures
        };
    }

    view() {
        const { source, section, shown } = this.state;
        const headingId = `source-${source.id}`;
        return html`
        <section aria-labelledby=${headingId} aria-busy=${section.loading}>
            <header>
                <h2 id=${headingId}>${source.title}</h2>
                <p>${source.description}</p>
            </header>
            <div class="toolbar">
                ${source.seasonal ? this.seasonFilter() : ""}
                ${this.resolutionFilter()}
                <p class="shown">${count(shown.length)}</p>
            </div>
            <div class="scroller">
                ${source.status === "valid" ? "" : html`<p role="alert" class="error">${source.reason}</p>`}
                ${section.error ? html`<p role="alert" class="error">${section.error}</p>` : ""}
                <ul class="cards">
                    ${shown.map((wallpaper, index) => this.card(wallpaper, index))}
                </ul>
                ${section.loading ? html`<p role="status" class="status">Loading wallpapers…</p>` : ""}
                ${section.hasMore && !section.loading
                    ? html`<button type="button" class="more" @click=${_ => loadMore(source.id)}>Load more ${source.title} wallpapers</button>`
                    : ""}
            </div>
        </section>
        `;
    }

    seasonFilter() {
        const { source, section } = this.state;
        return html`
        <fieldset class="segments">
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

    resolutionFilter() {
        const { minWidth } = this.state;
        return html`
        <fieldset class="segments mono">
            <legend>Resolution</legend>
            ${MIN_WIDTHS.map(({ width, label }) => html`
            <label>
                <input type="radio" name="resolution" value=${width}
                       .checked=${minWidth === width}
                       @change=${_ => filterByResolution(width)}>
                ${label}
            </label>`)}
        </fieldset>
        `;
    }

    card(wallpaper, index) {
        const { source, failures } = this.state;
        const key = keyOf(source.id, wallpaper.id);
        const failure = failures[key];
        return html`
        <li class="card">
            <button type="button"
                    aria-label="Preview ${source.title} wallpaper ${index + 1}, ${wallpaper.resolution}"
                    aria-current=${this.state.applied === key ? "true" : "false"}
                    @click=${_ => openPreview(source.id, wallpaper.id)}>
                <img src=${wallpaper.preview} alt="" loading="lazy">
                <span class="resolution">${wallpaper.resolution}</span>
                ${this.mark(key)}
            </button>
            <p class="caption">
                ${wallpaper.photographer
                    ? html`<a href="${wallpaper.photographer.profile}?${UNSPLASH_REFERRAL}">${wallpaper.photographer.name}</a>`
                    : html`<span>${source.title}</span>`}
                <span class="mono">${wallpaper.resolution}</span>
            </p>
            ${failure ? html`<p role="alert" class="error">${failure}</p>` : ""}
        </li>
        `;
    }

    mark(key) {
        const { applying, applied } = this.state;
        if (applying === key) return html`<span class="mark">Applying…</span>`;
        if (applied === key) return html`<span class="mark">In use</span>`;
        return "";
    }
}
customElements.define("b-gallery-section", GallerySection);
