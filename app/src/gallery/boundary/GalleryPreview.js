import BElement from "../../BElement.js";
import { html } from "lit-html";
import { applyWallpaper, closePreview, shownWallpapers, stepPreview } from "../control/GalleryControl.js";
import { UNSPLASH_REFERRAL } from "./GallerySection.js";

const STEPS = { ArrowRight: 1, ArrowLeft: -1 };

class GalleryPreview extends BElement {

    extractState({ gallery: { preview, sources, sections, minWidth, applying } }) {
        if (!preview) return { applying };
        const shown = shownWallpapers(sections[preview.sourceId], minWidth);
        const index = shown.findIndex(({ id }) => id === preview.wallpaperId);
        return {
            source: sources.find(({ id }) => id === preview.sourceId),
            wallpaper: shown[index],
            index,
            total: shown.length,
            applying
        };
    }

    triggerViewUpdate() {
        super.triggerViewUpdate();
        // the modal dialog brings focus trapping, Escape and focus return; state decides whether it is open
        const dialog = this.querySelector("dialog");
        const open = Boolean(this.state.wallpaper);
        if (open && !dialog.open) dialog.showModal();
        if (!open && dialog.open) dialog.close();
    }

    onKeydown(event) {
        const step = STEPS[event.key];
        if (!step || event.target.closest("a")) return;
        event.preventDefault();
        stepPreview(step);
    }

    view() {
        const { wallpaper } = this.state;
        return html`
        <dialog class="preview" aria-labelledby="preview-heading"
                @close=${_ => this.state.wallpaper && closePreview()}
                @keydown=${event => this.onKeydown(event)}>
            ${wallpaper ? this.content() : ""}
        </dialog>
        `;
    }

    content() {
        const { source, wallpaper, index, total, applying } = this.state;
        return html`
        <header>
            <button type="button" class="icon" aria-label="Close preview" @click=${_ => closePreview()}>×</button>
            <h2 id="preview-heading" class="visually-hidden">${source.title} wallpaper ${index + 1}</h2>
            ${wallpaper.photographer ? this.attribution(wallpaper.photographer) : ""}
            <span class="position mono">${index + 1} / ${total}</span>
            <button type="button" class="icon" aria-label="Previous wallpaper" @click=${_ => stepPreview(-1)}>←</button>
            <button type="button" class="icon" aria-label="Next wallpaper" @click=${_ => stepPreview(1)}>→</button>
        </header>
        <div class="frame">
            <img src=${wallpaper.preview} alt="">
        </div>
        <footer>
            <span class="mono">${wallpaper.resolution}</span>
            <button type="button" class="primary" ?disabled=${Boolean(applying)}
                    @click=${_ => applyWallpaper(source.id, wallpaper.id)}>Set wallpaper</button>
        </footer>
        `;
    }

    attribution({ name, profile }) {
        return html`
        <p class="attribution">
            Photo by <a href="${profile}?${UNSPLASH_REFERRAL}">${name}</a>
            on <a href="https://unsplash.com/?${UNSPLASH_REFERRAL}">Unsplash</a>
        </p>
        `;
    }
}
customElements.define("b-gallery-preview", GalleryPreview);
