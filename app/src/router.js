const isSupported = () => "URLPattern" in globalThis && "navigation" in globalThis;

export const initRouter = (outlet, routeConfig) => {
    // URLPattern and the Navigation API are Baseline Newly Available: without them, render the start route
    if (!isSupported()) {
        outlet.replaceChildren(document.createElement(routeConfig[0].component));
        return;
    }
    const routes = routeConfig.map(({ path, component }) => ({
        pattern: new URLPattern({ pathname: path }),
        component
    }));

    const render = url => {
        const match = routes
            .map(({ pattern, component }) => ({ result: pattern.exec(url), component }))
            .find(({ result }) => result);
        if (!match) return false;
        const routeComponent = document.createElement(match.component);
        Object.entries(match.result.pathname.groups)
            .filter(([, value]) => value !== undefined)
            .forEach(([name, value]) => routeComponent.setAttribute(name, value));
        outlet.replaceChildren(routeComponent);
        return true;
    };

    navigation.addEventListener("navigate", event => {
        if (!event.canIntercept || event.hashChange || event.downloadRequest) return;
        if (event.navigationType === "reload") return;
        const url = new URL(event.destination.url);
        if (!routes.some(({ pattern }) => pattern.test(url))) return;
        event.intercept({ handler: () => render(url) });
    });

    render(new URL(location.href));
};
