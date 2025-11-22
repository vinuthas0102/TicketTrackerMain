/**
 * Authentication Module
 * Handles user authentication, session management, and authorization
 */

const Auth = {
    /**
     * Login user with email and password
     */
    async login(email, password) {
        try {
            const response = await API.login(email, password);

            // Store authentication token and user data
            if (response.token) {
                localStorage.setItem('authToken', response.token);
            }

            if (response.user) {
                localStorage.setItem('currentUser', JSON.stringify(response.user));
            }

            return response;
        } catch (error) {
            console.error('Login failed:', error);
            throw error;
        }
    },

    /**
     * Logout current user
     */
    async logout() {
        try {
            await API.logout();
        } catch (error) {
            console.error('Logout error:', error);
        } finally {
            // Clear local storage
            localStorage.removeItem('authToken');
            localStorage.removeItem('currentUser');

            // Redirect to login page
            window.location.href = 'index.html';
        }
    },

    /**
     * Get current logged-in user
     */
    getCurrentUser() {
        const userJson = localStorage.getItem('currentUser');
        return userJson ? JSON.parse(userJson) : null;
    },

    /**
     * Check if user is authenticated
     */
    isAuthenticated() {
        return !!localStorage.getItem('authToken');
    },

    /**
     * Check if user has specific role
     */
    hasRole(role) {
        const user = this.getCurrentUser();
        return user && user.role === role;
    },

    /**
     * Check if user has any of the specified roles
     */
    hasAnyRole(...roles) {
        const user = this.getCurrentUser();
        return user && roles.includes(user.role);
    },

    /**
     * Require authentication - redirect to login if not authenticated
     */
    requireAuth() {
        if (!this.isAuthenticated()) {
            window.location.href = 'index.html';
            return false;
        }
        return true;
    },

    /**
     * Require specific role - redirect if user doesn't have it
     */
    requireRole(role) {
        if (!this.requireAuth()) return false;

        if (!this.hasRole(role)) {
            alert('You do not have permission to access this page.');
            window.location.href = 'dashboard.html';
            return false;
        }
        return true;
    },

    /**
     * Get authentication token
     */
    getToken() {
        return localStorage.getItem('authToken');
    }
};

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = Auth;
}
