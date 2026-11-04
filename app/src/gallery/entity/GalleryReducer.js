import { createReducer } from "@reduxjs/toolkit";
import {
    sourcesListedAction, sourcesFailedAction,
    pageRequestedAction, pageLoadedAction, pageFailedAction, seasonChosenAction,
    applyRequestedAction, appliedAction, applyFailedAction
} from "../control/GalleryControl.js";

/**
 * @typedef {{ id: string, title: string, description: string, seasonal: boolean, status: "valid" | "invalid" | "unsupported", reason?: string }} Source
 * @typedef {{ id: string, provider: string, preview: string, resolution: string, photographer?: { name: string, profile: string } }} Wallpaper
 * @typedef {{ wallpapers: Wallpaper[], page: number, hasMore: boolean, loading: boolean, error: string, season: string }} Section
 *   `season` is "" for all seasons
 */

const emptySection = (season = "") => ({ wallpapers: [], page: 0, hasMore: false, loading: false, error: "", season });

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

// a page requested before the season changed belongs to the previous filter
const isStale = (section, season) => section.season !== season;

export const gallery = createReducer(initialState, builder => {
    builder.addCase(sourcesListedAction, (state, { payload: { sources, season } }) => {
        state.sources = sources;
        state.sourcesLoading = false;
        state.sections = Object.fromEntries(sources.map(({ id, seasonal }) => [id, emptySection(seasonal ? season : "")]));
    }).addCase(sourcesFailedAction, (state, { payload }) => {
        state.sourcesLoading = false;
        state.sourcesError = payload;
    }).addCase(pageRequestedAction, (state, { payload }) => {
        state.sections[payload].loading = true;
        state.sections[payload].error = "";
    }).addCase(pageLoadedAction, (state, { payload: { id, season, page } }) => {
        const section = state.sections[id];
        if (isStale(section, season)) return;
        section.loading = false;
        section.page = page.page;
        section.hasMore = page.hasMore;
        section.wallpapers = section.wallpapers.concat(page.wallpapers);
    }).addCase(pageFailedAction, (state, { payload: { id, season, reason } }) => {
        const section = state.sections[id];
        if (isStale(section, season)) return;
        section.loading = false;
        section.error = reason;
    }).addCase(seasonChosenAction, (state, { payload: { id, season } }) => {
        state.sections[id] = emptySection(season);
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
