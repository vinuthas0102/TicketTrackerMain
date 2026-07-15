/**
 * API Client for Ticket Tracker
 * Handles all HTTP requests to the backend REST API
 */

const API = {
    baseURL: '/ticket-tracker/api',

    /**
     * Generic HTTP request method
     */
    async request(method, endpoint, data = null, options = {}) {
        const config = {
            method,
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            }
        };

        // Add authorization header if token exists
        const token = localStorage.getItem('authToken');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }

        // Add body for POST/PUT requests
        if (data && (method === 'POST' || method === 'PUT')) {
            config.body = JSON.stringify(data);
        }

        try {
            const response = await fetch(`${this.baseURL}${endpoint}`, config);

            // Handle non-JSON responses
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const result = await response.json();

                if (!response.ok) {
                    throw new Error(result.message || `HTTP error ${response.status}`);
                }

                return result;
            } else {
                if (!response.ok) {
                    throw new Error(`HTTP error ${response.status}`);
                }
                return await response.text();
            }
        } catch (error) {
            console.error('API request failed:', error);
            throw error;
        }
    },

    // Convenience methods
    get(endpoint, options) {
        return this.request('GET', endpoint, null, options);
    },

    post(endpoint, data, options) {
        return this.request('POST', endpoint, data, options);
    },

    put(endpoint, data, options) {
        return this.request('PUT', endpoint, data, options);
    },

    delete(endpoint, options) {
        return this.request('DELETE', endpoint, null, options);
    },

    // ========== Authentication APIs ==========

    async login(email, password) {
        return this.post('/auth/login', { email, password });
    },

    async logout() {
        return this.post('/auth/logout');
    },

    async getSession() {
        return this.get('/auth/session');
    },

    // ========== User APIs ==========

    async getUsers(params = {}) {
        const queryString = new URLSearchParams(params).toString();
        return this.get(`/users${queryString ? '?' + queryString : ''}`);
    },

    async getUser(id) {
        return this.get(`/users/${id}`);
    },

    async createUser(userData) {
        return this.post('/users', userData);
    },

    async updateUser(id, userData) {
        return this.put(`/users/${id}`, userData);
    },

    async deleteUser(id) {
        return this.delete(`/users/${id}`);
    },

    // ========== Ticket APIs ==========

    async getTickets(params = {}) {
        const queryString = new URLSearchParams(params).toString();
        return this.get(`/tickets${queryString ? '?' + queryString : ''}`);
    },

    async getTicket(id) {
        return this.get(`/tickets/${id}`);
    },

    async createTicket(ticketData) {
        return this.post('/tickets', ticketData);
    },

    async updateTicket(id, ticketData) {
        return this.put(`/tickets/${id}`, ticketData);
    },

    async deleteTicket(id) {
        return this.delete(`/tickets/${id}`);
    },

    async searchTickets(searchTerm) {
        return this.get(`/tickets/search?q=${encodeURIComponent(searchTerm)}`);
    },

    // ========== Workflow Step APIs ==========

    async getSteps(ticketId) {
        return this.get(`/steps?ticketId=${ticketId}`);
    },

    async getStep(id) {
        return this.get(`/steps/${id}`);
    },

    async createStep(stepData) {
        return this.post('/steps', stepData);
    },

    async updateStep(id, stepData) {
        return this.put(`/steps/${id}`, stepData);
    },

    async deleteStep(id) {
        return this.delete(`/steps/${id}`);
    },

    // ========== File APIs ==========

    async uploadFile(formData) {
        // FormData handles its own content type
        const token = localStorage.getItem('authToken');
        const config = {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            },
            body: formData
        };

        const response = await fetch(`${this.baseURL}/files/upload`, config);
        if (!response.ok) {
            throw new Error(`Upload failed: ${response.status}`);
        }
        return response.json();
    },

    async downloadFile(fileId) {
        const token = localStorage.getItem('authToken');
        const response = await fetch(`${this.baseURL}/files/${fileId}/download`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            throw new Error(`Download failed: ${response.status}`);
        }

        const blob = await response.blob();
        return blob;
    },

    async deleteFile(fileId) {
        return this.delete(`/files/${fileId}`);
    },

    // ========== Module APIs ==========

    async getModules() {
        return this.get('/modules');
    },

    async getModule(id) {
        return this.get(`/modules/${id}`);
    },

    // ========== Finance APIs ==========

    async submitForApproval(ticketId, data) {
        return this.post(`/finance/submit/${ticketId}`, data);
    },

    async approveTicket(ticketId, remarks) {
        return this.post(`/finance/approve/${ticketId}`, { remarks });
    },

    async rejectTicket(ticketId, reason) {
        return this.post(`/finance/reject/${ticketId}`, { reason });
    },

    // ========== Workflow Comment (Chat) APIs ==========

    async getStepComments(stepId) {
        return this.get(`/workflow-comments?stepId=${stepId}`);
    },

    async addStepComment(stepId, content, attachmentFile, channel) {
        if (attachmentFile) {
            const formData = new FormData();
            formData.append('stepId', stepId);
            formData.append('content', content);
            formData.append('channel', channel || 'in-app');
            formData.append('file', attachmentFile);
            const token = localStorage.getItem('authToken');
            const response = await fetch(`${this.baseURL}/workflow-comments`, {
                method: 'POST',
                headers: token ? { 'Authorization': `Bearer ${token}` } : {},
                credentials: 'include',
                body: formData
            });
            if (!response.ok) {
                const err = await response.json().catch(() => ({}));
                throw new Error(err.message || `HTTP error ${response.status}`);
            }
            return response.json();
        }
        return this.post('/workflow-comments', { stepId, content, channel: channel || 'in-app' });
    },

    async updateStepComment(commentId, content) {
        return this.put(`/workflow-comments/${commentId}`, { content });
    },

    async deleteStepComment(commentId) {
        return this.delete(`/workflow-comments/${commentId}`);
    },

    // ========== Export APIs ==========

    async exportTicketHtml(ticketId) {
        return this.get(`/export/ticket/${ticketId}/html`);
    },

    async exportTicketPdf(ticketId) {
        const token = localStorage.getItem('authToken');
        const response = await fetch(`${this.baseURL}/export/ticket/${ticketId}/pdf`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            throw new Error(`Export failed: ${response.status}`);
        }

        return response.blob();
    }
};

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = API;
}
