import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS, API_BASE_URL } from '../lib/apiEndpoints';
import { setAuthToken, setRefreshToken, saveUser } from '../lib/authToken';
import { setCsrfToken } from '../lib/csrfToken';
import { transformUserFromBackend } from '../lib/transformers/dataTransformer';
import { User, Module } from '../types';

const USERS_CACHE_KEY = 'cached_users';
const USERS_CACHE_TIMESTAMP_KEY = 'cached_users_timestamp';
const CACHE_DURATION = 5 * 60 * 1000;

export class AuthService {
  private static getCachedUsers(): User[] | null {
    try {
      const cached = localStorage.getItem(USERS_CACHE_KEY);
      const timestamp = localStorage.getItem(USERS_CACHE_TIMESTAMP_KEY);

      if (!cached || !timestamp) {
        return null;
      }

      const cacheAge = Date.now() - parseInt(timestamp, 10);
      if (cacheAge > CACHE_DURATION) {
        return null;
      }

      return JSON.parse(cached);
    } catch (error) {
      console.warn('Error reading cached users:', error);
      return null;
    }
  }

  private static setCachedUsers(users: User[]): void {
    try {
      localStorage.setItem(USERS_CACHE_KEY, JSON.stringify(users));
      localStorage.setItem(USERS_CACHE_TIMESTAMP_KEY, Date.now().toString());
    } catch (error) {
      console.warn('Error caching users:', error);
    }
  }
  static async fetchCsrfToken(): Promise<void> {
    try {
      const response = await apiClient.get<{ csrfToken: string }>(API_ENDPOINTS.AUTH.CSRF_TOKEN);
      if (response && response.csrfToken) {
        setCsrfToken(response.csrfToken);
      } else {
        console.warn('[AuthService] CSRF token not found in response:', response);
      }
    } catch (error) {
      console.error('[AuthService] Error fetching CSRF token:', error);
    }
  }

  static async login(usernameOrEmail: string, password: string): Promise<User | null> {
    try {
      const response = await apiClient.post<any>(API_ENDPOINTS.AUTH.LOGIN, {
        username: usernameOrEmail,
        password: password,
      });

      if (response.token) {
        setAuthToken(response.token);
        if (response.refreshToken) {
          setRefreshToken(response.refreshToken);
        }
      }

      if (response.user) {
        const user = transformUserFromBackend(response.user);
        saveUser(user);

        await this.fetchCsrfToken();

        return {
          ...user,
          lastLogin: new Date(),
        };
      }

      return null;
    } catch (error: any) {
      console.error('Login error:', error);
      return null;
    }
  }

  static async logout(): Promise<void> {
    try {
      await apiClient.post(API_ENDPOINTS.AUTH.LOGOUT);
    } catch (error) {
      console.error('Logout error:', error);
    }
  }

  static async getAllUsers(): Promise<User[]> {
    try {
      const users = await apiClient.get<any[]>(API_ENDPOINTS.USERS.LIST);
      if (!users || users.length === 0) {
        console.warn('No users returned from API. Users list is empty.');
        const cachedUsers = this.getCachedUsers();
        if (cachedUsers && cachedUsers.length > 0) {
          console.log(`Using ${cachedUsers.length} cached users as fallback`);
          return cachedUsers;
        }
      } else {
        const transformedUsers = users.map(transformUserFromBackend);
        this.setCachedUsers(transformedUsers);
        return transformedUsers;
      }
      return [];
    } catch (error: any) {
      console.error('Error fetching users:', error);
      const cachedUsers = this.getCachedUsers();
      if (cachedUsers && cachedUsers.length > 0) {
        return cachedUsers;
      }

      return [];
    }
  }

  static async getAvailableModules(): Promise<Module[]> {
    try {
      const modules = await apiClient.get<any[]>(API_ENDPOINTS.MODULES.LIST);

      return modules.map(module => {
        let parsedConfig = module.config;

        if (typeof module.config === 'string') {
          try {
            parsedConfig = JSON.parse(module.config);
            console.warn('Config was received as string, parsed successfully for module:', module.name);
          } catch (e) {
            console.error('Failed to parse config string for module:', module.name, e);
            parsedConfig = {};
          }
        }

        return {
          id: module.id,
          name: module.name,
          description: module.description || '',
          icon: module.icon || 'FileText',
          color: module.color || 'from-blue-500 to-indigo-500',
          schema_id: module.schema_id,
          config: {
            categories: Array.isArray(parsedConfig?.categories)
              ? parsedConfig.categories
              : ['General'],
            requestTypes: Array.isArray(parsedConfig?.requestTypes)
              ? parsedConfig.requestTypes
              : [],
            ...parsedConfig
          },
          active: module.active || true,
          created_at: new Date(module.created_at),
          updated_at: new Date(module.updated_at)
        };
      });
    } catch (error) {
      console.error('Error fetching modules:', error);
      return [];
    }
  }
}
