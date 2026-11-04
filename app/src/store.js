import { configureStore } from "@reduxjs/toolkit";
import { gallery } from "./gallery/entity/GalleryReducer.js";

// no localStorage persistence: wallpapers are re-fetched on every open, and remembering
// the applied wallpaper across reloads is out of the gallery's scope
const store = configureStore({ reducer: { gallery } });
export default store;
