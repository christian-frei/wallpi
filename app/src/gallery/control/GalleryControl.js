import { createAction } from "@reduxjs/toolkit";
import store from "../../store.js";

export const sourcesListedAction = createAction("sourcesListedAction");
export const sourcesFailedAction = createAction("sourcesFailedAction");
export const pageRequestedAction = createAction("pageRequestedAction");
export const pageLoadedAction = createAction("pageLoadedAction");
export const pageFailedAction = createAction("pageFailedAction");
export const seasonChosenAction = createAction("seasonChosenAction");
export const applyRequestedAction = createAction("applyRequestedAction");
export const appliedAction = createAction("appliedAction");
export const applyFailedAction = createAction("applyFailedAction");
export const sourceChosenAction = createAction("sourceChosenAction");
export const previewOpenedAction = createAction("previewOpenedAction");
export const previewClosedAction = createAction("previewClosedAction");
export const minWidthChosenAction = createAction("minWidthChosenAction");
export const toastExpiredAction = createAction("toastExpiredAction");

export const SEASONS = ["spring", "summer", "autumn", "winter"];

/** Minimum original widths of the resolution filter; 0 shows every card. */
export const MIN_WIDTHS = [
    { width: 0, label: "Any" },
    { width: 3840, label: "4K+" },
    { width: 5120, label: "5K+" },
    { width: 6000, label: "6K+" }
];

const TOAST_MILLIS = 2400;

/**
 * Meteorological season of the northern hemisphere: March starts spring, June summer, September autumn, December winter.
 * @param {Date} date
 */
export const seasonOf = date => SEASONS[Math.floor(((date.getMonth() + 10) % 12) / 3)];

/** Identifies a wallpaper across sources — wallpaper ids are only unique within their provider. */
export const keyOf = (sourceId, wallpaperId) => `${sourceId}/${wallpaperId}`;

/**
 * The cards a section shows under the resolution filter.
 * @param {{ wallpapers: { resolution: string }[] }} section
 * @param {number} minWidth
 */
export const shownWallpapers = ({ wallpapers }, minWidth) =>
    wallpapers.filter(({ resolution }) => Number.parseInt(resolution) >= minWidth);

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
    const { season } = store.getState().gallery.sections[id];
    const seasonQuery = season ? `&season=${season}` : "";
    store.dispatch(pageRequestedAction(id));
    try {
        const loaded = await request(`/sources/${encodeURIComponent(id)}/wallpapers?page=${page}${seasonQuery}`);
        store.dispatch(pageLoadedAction({ id, season, page: loaded }));
    } catch ({ message }) {
        store.dispatch(pageFailedAction({ id, season, reason: message }));
    }
};

export const openGallery = async () => {
    try {
        const sources = await request("/sources");
        store.dispatch(sourcesListedAction({ sources, season: seasonOf(new Date()) }));
        if (sources.length) chooseSource(sources[0].id);
    } catch ({ message }) {
        store.dispatch(sourcesFailedAction(message));
    }
};

/** Shows a source's section, loading its first page the first time — Unsplash allows few requests per hour. */
export const chooseSource = id => {
    store.dispatch(sourceChosenAction(id));
    const { sources, sections } = store.getState().gallery;
    const { status } = sources.find(source => source.id === id);
    const { page, loading } = sections[id];
    if (status === "valid" && page === 0 && !loading) loadPage(id, 1);
};

export const loadMore = id => {
    const { page } = store.getState().gallery.sections[id];
    return loadPage(id, page + 1);
};

/**
 * @param {string} id
 * @param {string} season one of SEASONS, or "" for all seasons
 */
export const filterBySeason = (id, season) => {
    store.dispatch(seasonChosenAction({ id, season }));
    return loadPage(id, 1);
};

/** @param {number} minWidth one of MIN_WIDTHS */
export const filterByResolution = minWidth => store.dispatch(minWidthChosenAction(minWidth));

export const openPreview = (sourceId, wallpaperId) => store.dispatch(previewOpenedAction({ sourceId, wallpaperId }));

export const closePreview = () => store.dispatch(previewClosedAction());

/** @param {1 | -1} step next or previous shown card, wrapping around */
export const stepPreview = step => {
    const { preview, sections, minWidth } = store.getState().gallery;
    if (!preview) return;
    const shown = shownWallpapers(sections[preview.sourceId], minWidth);
    const index = shown.findIndex(({ id }) => id === preview.wallpaperId);
    if (index < 0) return;
    const { id } = shown[(index + step + shown.length) % shown.length];
    openPreview(preview.sourceId, id);
};

let toastTimer;

export const applyWallpaper = async (sourceId, wallpaperId) => {
    const { applying, sections, sources } = store.getState().gallery;
    if (applying) return;
    const key = keyOf(sourceId, wallpaperId);
    const wallpaper = sections[sourceId].wallpapers.find(({ id }) => id === wallpaperId);
    const { title } = sources.find(({ id }) => id === sourceId);
    store.dispatch(applyRequestedAction(key));
    try {
        await request("/desktop/wallpaper", {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ sourceId, wallpaperId })
        });
        store.dispatch(appliedAction({ key, current: { preview: wallpaper.preview, title, resolution: wallpaper.resolution } }));
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => store.dispatch(toastExpiredAction()), TOAST_MILLIS);
    } catch ({ message }) {
        store.dispatch(applyFailedAction({ key, reason: message }));
    }
};
