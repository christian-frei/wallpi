import "./gallery/boundary/Gallery.js";
import { initRouter } from "./router.js";

initRouter(document.querySelector(".view"), [
    { path: "/", component: "b-gallery" }
]);
