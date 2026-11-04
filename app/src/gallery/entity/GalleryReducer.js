import { createReducer } from "@reduxjs/toolkit";
import {
    sourcesListedAction, sourcesFailedAction,
    pageRequestedAction, pageLoadedAction, pageFailedAction,
    applyRequestedAction, appliedAction, applyFailedAction
} from "../control/GalleryControl.js";

/**
 * @typedef {{ id: string, title: string, description: string, status: "valid" | "invalid" | "unsupported", reason?: string }} Source
 * @typedef {{ id: string, provider: string, preview: string, resolution: string, photographer?: { name: string, profile: string } }} Wallpaper
 * @typedef {{ wallpapers: Wallpaper[], page: number, hasMore: boolean, loading: boolean, error: string }} Section
 */

const emptySection = () => ({ wallpapers: [], page: 0, hasMore: false, loading: false, error: "" });

const initialState = {
    /** @type {Source[]} */
    sources: [],
    sourcesLoading: true,
    sourcesError: "",
    /** @type {Record<string, Section>} */
    sections: {},
    applying: "",
    applied: "",
    /** @type {Record<string, string>} */
    failures: {}
};

export const gallery = createReducer(initialState, builder => {
    builder.addCase(sourcesListedAction, (state, { payload }) => {
        state.sources = payload;
        state.sourcesLoading = false;
        state.sections = Object.fromEntries(payload.map(({ id }) => [id, emptySection()]));
    }).addCase(sourcesFailedAction, (state, { payload }) => {
        state.sourcesLoading = false;
        state.sourcesError = payload;
    }).addCase(pageRequestedAction, (state, { payload }) => {
        state.sections[payload].loading = true;
        state.sections[payload].error = "";
    }).addCase(pageLoadedAction, (state, { payload: { id, page } }) => {
        const section = state.sections[id];
        section.loading = false;
        section.page = page.page;
        section.hasMore = page.hasMore;
        section.wallpapers = section.wallpapers.concat(page.wallpapers);
    }).addCase(pageFailedAction, (state, { payload: { id, reason } }) => {
        state.sections[id].loading = false;
        state.sections[id].error = reason;
    }).addCase(applyRequestedAction, (state, { payload }) => {
        state.applying = payload;
        delete state.failures[payload];
    }).addCase(appliedAction, (state, { payload }) => {
        state.applying = "";
        state.applied = payload;
    }).addCase(applyFailedAction, (state, { payload: { key, reason } }) => {
        state.applying = "";
        state.failures[key] = reason;
    });
});
