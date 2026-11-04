import BElement from "../../BElement.js";
import { html } from "lit-html";
import { openGallery } from "../control/GalleryControl.js";
import "./GallerySection.js";

const SOURCES_FILE = "~/.wallpi/sources.json";

class Gallery extends BElement {

    connectedCallback() {
        super.connectedCallback();
        openGallery();
    }

    extractState({ gallery: { sources, sourcesLoading, sourcesError } }) {
        return { sources, sourcesLoading, sourcesError };
    }

    view() {
        const { sources, sourcesLoading, sourcesError } = this.state;
        if (sourcesError) {
            return html`<p role="alert" class="error">Could not list the sources in ${SOURCES_FILE}: ${sourcesError}</p>`;
        }
        if (sourcesLoading) {
            return html`<p role="status" class="status">Loading sources…</p>`;
        }
        return html`${sources.map(({ id }) => html`<b-gallery-section source-id=${id}></b-gallery-section>`)}`;
    }
}
customElements.define("b-gallery", Gallery);
