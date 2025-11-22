/**
 * Authentication Manager
 *
 * Handles user authentication, session management, and login/logout
 */

export class AuthManager {
    constructor() {
        this.currentUser = null;
        this.apiBaseUrl = 'http://localhost:8080/ticket-tracker/api';
    }

    async login(email, password) {
        try {
            // In a real implementation, this would call the backend auth endpoint
            // For now, using a simple validation
            const response = await fetch(`${this.apiBaseUrl}/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify({ email, password })
            });

            if (response.ok) {
                const user = await response.json();
                this.currentUser = user;
                sessionStorage.setItem('user', JSON.stringify(user));

                return {
                    success: true,
                    user: user
                };
            } else {
                return {
                    success: false,
                    error: 'Invalid credentials'
                };
            }
        } catch (error) {
            console.error('Login error:', error);
            return {
                success: false,
                error: 'An error occurred during login'
            };
        }
    }

    async logout() {
        try {
            await fetch(`${this.apiBaseUrl}/auth/logout`, {
                method: 'POST',
                credentials: 'include'
            });
        } catch (error) {
            console.error('Logout error:', error);
        } finally {
            this.currentUser = null;
            sessionStorage.removeItem('user');
        }
    }

    async checkAuth() {
        const userStr = sessionStorage.getItem('user');

        if (userStr) {
            try {
                this.currentUser = JSON.parse(userStr);
                return true;
            } catch (error) {
                console.error('Error parsing user data:', error);
                sessionStorage.removeItem('user');
                return false;
            }
        }

        return false;
    }

    getCurrentUser() {
        return this.currentUser;
    }

    hasRole(role) {
        return this.currentUser && this.currentUser.role === role;
    }

    isAuthenticated() {
        return this.currentUser !== null;
    }
}
