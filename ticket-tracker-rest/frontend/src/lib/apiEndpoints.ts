export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/ticket-tracker-java/api';
export const API_TIMEOUT = parseInt(import.meta.env.VITE_API_TIMEOUT || '30000', 10);

export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    LOGOUT: '/auth/logout',
    REFRESH: '/auth/refresh',
    ME: '/auth/me',
    CSRF_TOKEN: '/auth/csrf-token',
  },

  USERS: {
    LIST: '/users',
    GET: (id: string) => `/users/${id}`,
    CREATE: '/users',
    UPDATE: (id: string) => `/users/${id}`,
    DELETE: (id: string) => `/users/${id}`,
    ENABLE: (id: string) => `/users/${id}/enable`,
    DISABLE: (id: string) => `/users/${id}/disable`,
    LOCK: (id: string) => `/users/${id}/lock`,
    UNLOCK: (id: string) => `/users/${id}/unlock`,
    RESET_PASSWORD: (id: string) => `/users/reset-password/${id}`,
    ACTIVITY_LOGS: (id: string) => `/users/${id}/activity-logs`,
    AUDIT: (id: string) => `/users/${id}/audit`,
    FILTER: '/users/filter',
  },

  MODULES: {
    LIST: '/modules',
    GET: (id: string) => `/modules/${id}`,
    UPDATE_CONFIG: (id: string) => `/modules/${id}/config`,
  },

  TICKETS: {
    LIST: '/tickets',
    GET: (id: string) => `/tickets/${id}`,
    CREATE: '/tickets',
    UPDATE: (id: string) => `/tickets/${id}`,
    DELETE: (id: string) => `/tickets/${id}`,
    STATUS: (id: string) => `/tickets/${id}/status`,
    BULK_CREATE: '/tickets/bulk',
    AUDIT: (id: string) => `/tickets/${id}/audit`,
    FILES: (id: string) => `/tickets/${id}/files`,
    ACCESSIBLE: '/tickets/accessible',
  },

  WORKFLOW_STEPS: {
    LIST: (ticketId: string) => `/workflow-steps?ticketId=${ticketId}`,
    CREATE: '/workflow-steps',
    BULK_CREATE: '/workflow-steps/bulk',
    GET: (id: string) => `/workflow-steps/${id}`,
    UPDATE: (id: string) => `/workflow-steps/${id}`,
    DELETE: (id: string) => `/workflow-steps/${id}`,
    STATUS: (id: string) => `/workflow-steps/${id}/status`,
    PROGRESS: (id: string) => `/workflow-steps/${id}/progress`,
    FILES: (id: string) => `/workflow-steps/${id}/files`,
    PROGRESS_HISTORY: (id: string) => `/workflow-steps/${id}/progress-history`,
    PROGRESS_DOCUMENTS: (id: string) => `/workflow-steps/${id}/progress-documents`,
  },

  FILES: {
    UPLOAD: '/files/upload',
    GET: (id: string) => `/files/${id}`,
    DOWNLOAD: (id: string) => `/files/${id}?download=true`,
    DELETE: (id: string) => `/files/${id}`,
    PROGRESS_DOCS: '/files/progress-docs',
    COMPLETION_CERT: '/files/completion-cert',
    COMPLETION_CERTIFICATES: '/files/completion-certificates',
    CHECK_COMPLETION_CERT: '/files/completion-certificates/check',
    COPY_ATTACHMENTS: '/files/copy-attachments',
    LIST_BY_TICKET: (ticketId: string) => `/files?ticketId=${ticketId}`,
    LIST_BY_STEP: (stepId: string) => `/files?stepId=${stepId}`,
  },

  FINANCE: {
    SUBMISSIONS: '/finance-approvals',
    PENDING: '/finance-approvals/pending',
    GET: (id: string) => `/finance-approvals/${id}`,
    APPROVE: (id: string) => `/finance-approvals/${id}/approve`,
    REJECT: (id: string) => `/finance-approvals/${id}/reject`,
  },

  DEPENDENCIES: {
    LIST: '/dependencies',
    CREATE: '/dependencies',
    DELETE: (id: string) => `/dependencies/${id}`,
  },

  FIELD_CONFIG: {
    LIST: '/field-config',
    GET: (id: string) => `/field-config/${id}`,
    CREATE: '/field-config',
    UPDATE: (id: string) => `/field-config/${id}`,
    DELETE: (id: string) => `/field-config/${id}`,
    BY_MODULE: (moduleId: string) => `/field-config?moduleId=${moduleId}`,
    REORDER: '/field-config/reorder',
    OPTIONS: (configId: string) => `/field-config/options/${configId}`,
    CREATE_OPTION: '/field-config/options',
    UPDATE_OPTION: (id: string) => `/field-config/options/${id}`,
    DELETE_OPTION: (id: string) => `/field-config/options/${id}`,
  },

  FIELD_VALUES: {
    GET: (entityId: string) => `/field-values/${entityId}`,
    SAVE: (entityId: string) => `/field-values/${entityId}`,
    DELETE: (entityId: string, fieldKey: string) => `/field-values/${entityId}/${fieldKey}`,
  },

  FILE_REFERENCES: {
    TEMPLATES: '/file-reference-templates',
    TEMPLATE: (id: string) => `/file-reference-templates/${id}`,
    BY_STEP: (stepId: string) => `/file-references/step/${stepId}`,
    UPDATE_STATUS: (id: string) => `/file-references/${id}/status`,
  },

  USER_PREFERENCES: {
    GET: '/user-preferences',
    SAVE: '/user-preferences',
    RESET: '/user-preferences',
  },

  AUDIT: {
    CREATE: '/audit-logs',
    LIST_BY_TICKET: (ticketId: string) => `/audit-logs?ticketId=${ticketId}`,
    LIST_BY_STEP: (stepId: string) => `/audit-logs?stepId=${stepId}`,
  },

  WORKFLOW_COMMENTS: {
    LIST: (stepId: string) => `/workflow-comments?stepId=${stepId}`,
    CREATE: '/workflow-comments',
    UPDATE: (commentId: string) => `/workflow-comments/${commentId}`,
    DELETE: (commentId: string) => `/workflow-comments/${commentId}`,
    DOWNLOAD_ATTACHMENT: (commentId: string) => `/workflow-comments/${commentId}/attachment`,
  },

  MASTER_DATA: {
    CATEGORIES: '/master-data/categories',
    DEPARTMENTS: '/master-data/departments',
    LOCATIONS: '/master-data/locations',
    CONFIG: '/master-data/config',
    MODULE_CODES: '/master-data/module-codes',
  },
};
