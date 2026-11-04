import { createAction } from "@reduxjs/toolkit";
import store from "../../store.js";

export const sourcesListedAction = createAction("sourcesListedAction");
export const sourcesFailedAction = createAction("sourcesFailedAction");
export const pageRequestedAction = createAction("pageRequestedAction");
export const pageLoadedAction = createAction("pageLoadedAction");
export const pageFailedAction = createAction("pageFailedAction");
export const applyRequestedAction = createAction("applyRequestedAction");
export const appliedAction = createAction("appliedAction");
export const applyFailedAction = createAction("applyFailedAction");

/** Identifies a wallpaper across sources — wallpaper ids are only unique within their provider. */
export const keyOf = (sourceId, wallpaperId) => `${sourceId}/${wallpaperId}`;

const reasonOf = async response => {
    try {
        const { reason } = await response.json();
        return reason ?? `HTTP ${response.status}`;
    } catch {
        return `HTTP ${response.status}`;
    }
};

const request = async (url, options) => {
    const response = await fetch(url, options);
    if (!response.ok) throw new Error(await reasonOf(response));
    return response.json();
};

const loadPage = async (id, page) => {
    store.dispatch(pageRequestedAction(id));
    try {
        const loaded = await request(`/sources/${encodeURIComponent(id)}/wallpapers?page=${page}`);
        store.dispatch(pageLoadedAction({ id, page: loaded }));
    } catch ({ message }) {
        store.dispatch(pageFailedAction({ id, reason: message }));
    }
};

export const openGallery = async () => {
    try {
        const sources = await request("/sources");
        store.dispatch(sourcesListedAction(sources));
        sources
            .filter(({ status }) => status === "valid")
            .forEach(({ id }) => loadPage(id, 1));
    } catch ({ message }) {
        store.dispatch(sourcesFailedAction(message));
    }
};

export const loadMore = id => {
    const { page } = store.getState().gallery.sections[id];
    return loadPage(id, page + 1);
};

export const applyWallpaper = async (sourceId, wallpaperId) => {
    if (store.getState().gallery.applying) return;
    const key = keyOf(sourceId, wallpaperId);
    store.dispatch(applyRequestedAction(key));
    try {
        await request("/desktop/wallpaper", {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ sourceId, wallpaperId })
        });
        store.dispatch(appliedAction(key));
    } catch ({ message }) {
        store.dispatch(applyFailedAction({ key, reason: message }));
    }
};
