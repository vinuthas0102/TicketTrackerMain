import { getAuthToken, clearAuthToken } from './authToken';
import { getCsrfToken, setCsrfToken } from './csrfToken';
import { API_BASE_URL, API_TIMEOUT } from './apiEndpoints';
import { logApiRequest, logApiResponse, logApiError } from './apiLogger';

export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
    details?: any;
  };
}

export interface ApiError {
  code: string;
  message: string;
  status?: number;
  details?: any;
}

class ApiClient {
  private baseURL: string;
  private timeout: number;
  private isRefreshingCsrf: boolean = false;

  constructor() {
    this.baseURL = API_BASE_URL;
    this.timeout = API_TIMEOUT;
  }

  private async refreshCsrfToken(): Promise<boolean> {
    if (this.isRefreshingCsrf) {
      return false;
    }

    this.isRefreshingCsrf = true;
    try {
      const url = `${this.baseURL}/auth/csrf-token`;
      const token = getAuthToken();

      const headers: HeadersInit = {};
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await fetch(url, {
        method: 'GET',
        headers,
        credentials: 'include',
      });

      if (response.ok) {
        const data = await response.json();
        if (data && data.csrfToken) {
          setCsrfToken(data.csrfToken);
          return true;
        } else {
          console.warn('[ApiClient] CSRF token not found in refresh response:', data);
        }
      }
      return false;
    } catch (error) {
      console.error('[ApiClient] Failed to refresh CSRF token:', error);
      return false;
    } finally {
      this.isRefreshingCsrf = false;
    }
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {},
    isRetry: boolean = false
  ): Promise<T> {
    const url = `${this.baseURL}${endpoint}`;
    const token = getAuthToken();

    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    if (token && !endpoint.startsWith('/auth')) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const method = options.method?.toUpperCase();
    const requiresCsrf = method === 'POST' || method === 'PUT' || method === 'DELETE';
    const isLoginOrLogout = endpoint === '/auth/login' || endpoint === '/auth/logout';

    if (requiresCsrf && !isLoginOrLogout) {
      const csrfToken = getCsrfToken();
      if (csrfToken) {
        headers['X-CSRF-Token'] = csrfToken;
      }
    }

    const config: RequestInit = {
      ...options,
      headers,
    };

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.timeout);

    logApiRequest(options.method || 'GET', url, options.body);

    try {
      const response = await fetch(url, {
        ...config,
        headers,
        credentials: 'include',
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      if (response.status === 401) {
        clearAuthToken();
        window.location.href = '/';
        throw new Error('Session expired. Please login again.');
      }

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));

        if (response.status === 403 && requiresCsrf && !isRetry) {
          const refreshed = await this.refreshCsrfToken();
          if (refreshed) {
            return this.request<T>(endpoint, options, true);
          }
        }

        const error: ApiError = {
          code: errorData.error?.code || 'API_ERROR',
          message: errorData.error?.message || `HTTP ${response.status}: ${response.statusText}`,
          status: response.status,
          details: errorData.error?.details,
        };
        logApiError(error);
        throw error;
      }

      const data = await response.json();
      logApiResponse(response.status, data);

      if (data.success === false) {
        const error: ApiError = {
          code: data.error?.code || 'API_ERROR',
          message: data.error?.message || 'An error occurred',
          details: data.error?.details,
        };
        logApiError(error);
        throw error;
      }

      if (typeof data === 'object' && data !== null && 'data' in data) {
        if ('success' in data || 'error' in data) {
          return data.data !== undefined ? data.data : data;
        }
      }
      return data;
    } catch (error: any) {
      clearTimeout(timeoutId);

      if (error.name === 'AbortError') {
        const timeoutError: ApiError = {
          code: 'TIMEOUT',
          message: 'Request timeout. Please try again.',
        };
        logApiError(timeoutError);
        throw timeoutError;
      }

      if (error.code) {
        throw error;
      }

      const networkError: ApiError = {
        code: 'NETWORK_ERROR',
        message: 'Cannot connect to server. Please check your internet connection.',
        details: error.message,
      };
      logApiError(networkError);
      throw networkError;
    }
  }

  async get<T>(endpoint: string, params?: Record<string, any>): Promise<T> {
    const queryString = params
      ? '?' + new URLSearchParams(params).toString()
      : '';
    return this.request<T>(endpoint + queryString, {
      method: 'GET',
    });
  }

  async post<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async put<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'DELETE',
    });
  }

  async uploadFile<T>(
    endpoint: string,
    file: File,
    additionalData?: Record<string, any>
  ): Promise<T> {
    const url = `${this.baseURL}${endpoint}`;
    const token = getAuthToken();
    const csrfToken = getCsrfToken();

    const formData = new FormData();
    formData.append('file', file);

    if (additionalData) {
      Object.keys(additionalData).forEach((key) => {
        const value = additionalData[key];
        if (value !== null && value !== undefined && value !== 'null' && value !== 'undefined') {
          formData.append(key, value);
        } else {
        }
      });
    }

    const headers: HeadersInit = {};
    if (csrfToken) {
      headers['X-CSRF-Token'] = csrfToken;
    }

    logApiRequest('POST', url, { file: file.name, ...additionalData });

    try {
      const response = await fetch(url, {
        method: 'POST',
        credentials: 'include',
        headers,
        body: formData,
      });

      if (response.status === 401) {
        clearAuthToken();
        window.location.href = '/';
        throw new Error('Session expired. Please login again.');
      }

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        const error: ApiError = {
          code: errorData.error?.code || 'UPLOAD_ERROR',
          message: errorData.error?.message || 'File upload failed',
          status: response.status,
        };
        logApiError(error);
        throw error;
      }

      const data = await response.json();
      logApiResponse(response.status, data);

      if (typeof data === 'object' && data !== null && 'data' in data) {
        if ('success' in data || 'error' in data) {
          return data.data !== undefined ? data.data : data;
        }
      }
      return data;
    } catch (error: any) {
      console.error('[ApiClient] Upload error:', error);
      if (error.code) {
        throw error;
      }

      const uploadError: ApiError = {
        code: 'UPLOAD_ERROR',
        message: 'File upload failed. Please try again.',
        details: error.message,
      };
      logApiError(uploadError);
      throw uploadError;
    }
  }

  async downloadFile(endpoint: string, filename: string): Promise<void> {
    const url = `${this.baseURL}${endpoint}`;
    const token = getAuthToken();

    const headers: HeadersInit = {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const response = await fetch(url, { headers });

      if (!response.ok) {
        throw new Error('Download failed');
      }

      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(downloadUrl);
    } catch (error) {
      const downloadError: ApiError = {
        code: 'DOWNLOAD_ERROR',
        message: 'File download failed. Please try again.',
      };
      logApiError(downloadError);
      throw downloadError;
    }
  }
}

export const apiClient = new ApiClient();
