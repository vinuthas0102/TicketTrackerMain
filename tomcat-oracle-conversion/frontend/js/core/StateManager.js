/**
 * State Manager
 *
 * Manages application state using a simple pub-sub pattern
 */

export class StateManager {
    constructor() {
        this.state = {};
        this.listeners = {};
    }

    setState(key, value) {
        this.state[key] = value;
        this.notifyListeners(key, value);
    }

    getState(key) {
        return this.state[key];
    }

    subscribe(key, callback) {
        if (!this.listeners[key]) {
            this.listeners[key] = [];
        }
        this.listeners[key].push(callback);
    }

    unsubscribe(key, callback) {
        if (this.listeners[key]) {
            this.listeners[key] = this.listeners[key].filter(cb => cb !== callback);
        }
    }

    notifyListeners(key, value) {
        if (this.listeners[key]) {
            this.listeners[key].forEach(callback => callback(value));
        }
    }

    clearState() {
        this.state = {};
    }
}
