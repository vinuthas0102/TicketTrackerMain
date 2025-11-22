/**
 * Router
 *
 * Simple client-side router for single-page navigation
 */

export class Router {
    constructor() {
        this.routes = {};
        this.currentRoute = null;
    }

    addRoute(path, handler) {
        this.routes[path] = handler;
    }

    navigate(path) {
        if (this.routes[path]) {
            this.currentRoute = path;
            this.routes[path]();
            window.history.pushState({}, '', path);
        } else {
            console.error(`Route not found: ${path}`);
        }
    }

    init() {
        window.addEventListener('popstate', () => {
            const path = window.location.hash.slice(1) || '/';
            if (this.routes[path]) {
                this.routes[path]();
            }
        });

        // Handle initial route
        const initialPath = window.location.hash.slice(1) || '/';
        if (this.routes[initialPath]) {
            this.routes[initialPath]();
        }
    }
}
