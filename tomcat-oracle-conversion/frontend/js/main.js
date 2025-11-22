/**
 * Ticket Tracker - Main Application Entry Point
 *
 * This is the main JavaScript file that initializes the application.
 * It handles routing, authentication, and application state.
 */

import { AuthManager } from './auth/AuthManager.js';
import { Router } from './core/Router.js';
import { ApiClient } from './api/ApiClient.js';
import { StateManager } from './core/StateManager.js';

class TicketTrackerApp {
    constructor() {
        this.authManager = new AuthManager();
        this.router = new Router();
        this.apiClient = new ApiClient();
        this.stateManager = new StateManager();
    }

    async init() {
        console.log('Initializing Ticket Tracker Application...');

        // Initialize API client with base URL
        this.apiClient.setBaseUrl('http://localhost:8080/ticket-tracker/api');

        // Check authentication status
        const isAuthenticated = await this.authManager.checkAuth();

        if (isAuthenticated) {
            // User is authenticated, show dashboard
            await this.loadDashboard();
        } else {
            // User is not authenticated, show login
            await this.loadLogin();
        }

        // Initialize router
        this.router.init();

        // Set up event listeners
        this.setupEventListeners();
    }

    async loadLogin() {
        console.log('Loading login page...');
        const appContainer = document.getElementById('app');
        appContainer.innerHTML = `
            <div class="login-container">
                <div class="login-card">
                    <h1>Ticket Tracker</h1>
                    <p class="subtitle">Enterprise Workflow Management</p>

                    <form id="login-form">
                        <div class="form-group">
                            <label for="email">Email</label>
                            <input type="email" id="email" name="email" required placeholder="Enter your email">
                        </div>

                        <div class="form-group">
                            <label for="password">Password</label>
                            <input type="password" id="password" name="password" required placeholder="Enter your password">
                        </div>

                        <button type="submit" class="btn btn-primary btn-block">Login</button>

                        <div id="error-message" class="error-message" style="display: none;"></div>
                    </form>

                    <div class="test-credentials">
                        <p><strong>Test Credentials:</strong></p>
                        <ul>
                            <li>Admin: admin@company.com / admin</li>
                            <li>Manager: manager@company.com / manager</li>
                            <li>Employee: john@company.com / user</li>
                        </ul>
                    </div>
                </div>
            </div>
        `;

        // Set up login form handler
        const loginForm = document.getElementById('login-form');
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleLogin(e);
        });
    }

    async handleLogin(event) {
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const errorMessage = document.getElementById('error-message');

        try {
            const result = await this.authManager.login(email, password);

            if (result.success) {
                // Store user data
                this.stateManager.setState('user', result.user);

                // Redirect to dashboard
                await this.loadDashboard();
            } else {
                errorMessage.textContent = 'Invalid email or password';
                errorMessage.style.display = 'block';
            }
        } catch (error) {
            console.error('Login error:', error);
            errorMessage.textContent = 'An error occurred. Please try again.';
            errorMessage.style.display = 'block';
        }
    }

    async loadDashboard() {
        console.log('Loading dashboard...');
        const appContainer = document.getElementById('app');

        // Get current user
        const user = this.stateManager.getState('user');

        appContainer.innerHTML = `
            <div class="dashboard-container">
                <header class="app-header">
                    <div class="header-content">
                        <h1>Ticket Tracker</h1>
                        <div class="user-menu">
                            <span class="user-name">${user?.name || 'User'}</span>
                            <button id="logout-btn" class="btn btn-secondary">Logout</button>
                        </div>
                    </div>
                </header>

                <main class="app-main">
                    <div class="sidebar">
                        <nav class="main-nav">
                            <a href="#dashboard" class="nav-link active">Dashboard</a>
                            <a href="#tickets" class="nav-link">Tickets</a>
                            <a href="#modules" class="nav-link">Modules</a>
                            <a href="#reports" class="nav-link">Reports</a>
                        </nav>
                    </div>

                    <div class="content">
                        <h2>Dashboard</h2>
                        <p>Welcome to Ticket Tracker!</p>

                        <div class="status-cards">
                            <div class="card">
                                <h3>Total Tickets</h3>
                                <p class="stat-number">0</p>
                            </div>
                            <div class="card">
                                <h3>Active Tickets</h3>
                                <p class="stat-number">0</p>
                            </div>
                            <div class="card">
                                <h3>Completed</h3>
                                <p class="stat-number">0</p>
                            </div>
                        </div>

                        <div class="ticket-list">
                            <h3>Recent Tickets</h3>
                            <p>No tickets found.</p>
                        </div>
                    </div>
                </main>
            </div>
        `;

        // Set up logout handler
        const logoutBtn = document.getElementById('logout-btn');
        logoutBtn.addEventListener('click', async () => {
            await this.authManager.logout();
            await this.loadLogin();
        });
    }

    setupEventListeners() {
        // Global error handler
        window.addEventListener('error', (event) => {
            console.error('Global error:', event.error);
        });

        // Handle unhandled promise rejections
        window.addEventListener('unhandledrejection', (event) => {
            console.error('Unhandled promise rejection:', event.reason);
        });
    }
}

// Initialize application when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    const app = new TicketTrackerApp();
    app.init();
});

export default TicketTrackerApp;
