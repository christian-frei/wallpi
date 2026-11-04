import BElement from "../../BElement.js";
import { html } from "lit-html";
import { chooseSource, openGallery } from "../control/GalleryControl.js";
import "./GallerySection.js";
import "./GalleryPreview.js";

const SOURCES_FILE = "~/.wallpi/sources.json";

class Gallery extends BElement {

    connectedCallback() {
        super.connectedCallback();
        openGallery();
    }

    extractState({ gallery: { sources, sourcesLoading, sourcesError, sections, chosen, current, toast } }) {
        return { sources, sourcesLoading, sourcesError, sections, chosen, current, toast };
    }

    view() {
        const { toast } = this.state;
        return html`
        <div class="shell">
            <aside>
                <h1>wallpi</h1>
                ${this.sourceList()}
                ${this.current()}
            </aside>
            <main id="content">
                ${this.content()}
            </main>
            <b-gallery-preview></b-gallery-preview>
            ${toast ? html`<p role="status" class="toast">${toast}</p>` : ""}
        </div>
        `;
    }

    content() {
        const { sourcesLoading, sourcesError, chosen } = this.state;
        if (sourcesError) {
            return html`<p role="alert" class="error notice">Could not list the sources in ${SOURCES_FILE}: ${sourcesError}</p>`;
        }
        if (sourcesLoading) {
            return html`<p role="status" class="status notice">Loading sources…</p>`;
        }
        return chosen ? html`<b-gallery-section source-id=${chosen}></b-gallery-section>` : "";
    }

    sourceList() {
        const { sources, sections, chosen } = this.state;
        return html`
        <nav aria-labelledby="sources-heading">
            <h2 id="sources-heading" class="eyebrow">Sources</h2>
            <ul>
                ${sources.map(({ id, title, status }) => html`
                <li>
                    <button type="button" aria-current=${chosen === id ? "true" : "false"} @click=${_ => chooseSource(id)}>
                        <span>${title}</span>
                        <span class="count" aria-hidden="true">${status === "valid" ? sections[id].wallpapers.length || "" : "!"}</span>
                    </button>
                </li>`)}
            </ul>
        </nav>
        `;
    }

    current() {
        const { current } = this.state;
        if (!current) return "";
        return html`
        <section class="current" aria-labelledby="current-heading">
            <h2 id="current-heading" class="eyebrow">Current</h2>
            <img src=${current.preview} alt="">
            <p>${current.title} · ${current.resolution}</p>
        </section>
        `;
    }
}
customElements.define("b-gallery", Gallery);
