import { render } from "lit-html";
import store from "./store.js";

export default class BElement extends HTMLElement {

    constructor() {
        super();
        this.state = {};
    }

    connectedCallback() {
        this.unsubscribe = store.subscribe(_ => this.triggerViewUpdate());
        this.triggerViewUpdate();
    }

    triggerViewUpdate() {
        this.state = this.extractState(store.getState());
        render(this.view(), this.getRenderTarget());
    }

    extractState(reduxState) {
        return reduxState;
    }

    getRenderTarget() {
        return this;
    }

    view() {
    }

    disconnectedCallback() {
        this.unsubscribe();
    }
}
