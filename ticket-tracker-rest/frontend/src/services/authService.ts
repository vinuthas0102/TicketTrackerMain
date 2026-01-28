import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS, API_BASE_URL } from '../lib/apiEndpoints';
import { setAuthToken, setRefreshToken, saveUser } from '../lib/authToken';
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
        console.log(`Successfully loaded ${users.length} users from API`);
        const transformedUsers = users.map(transformUserFromBackend);
        this.setCachedUsers(transformedUsers);
        return transformedUsers;
      }
      return [];
    } catch (error: any) {
      console.error('Error fetching users:', error);
      console.error('Full API URL:', `${API_BASE_URL}${API_ENDPOINTS.USERS.LIST}`);
      console.error('API Base URL:', API_BASE_URL);
      console.error('API Endpoint:', API_ENDPOINTS.USERS.LIST);
      console.error('Error details:', {
        code: error.code,
        message: error.message,
        status: error.status,
      });
      console.error('Please verify your backend is running and accessible at the API URL above');

      const cachedUsers = this.getCachedUsers();
      if (cachedUsers && cachedUsers.length > 0) {
        console.warn(`API request failed. Using ${cachedUsers.length} cached users as fallback`);
        return cachedUsers;
      }

      return [];
    }
  }

  static async getAvailableModules(): Promise<Module[]> {
    try {
      const modules = await apiClient.get<any[]>(API_ENDPOINTS.MODULES.LIST);

      return modules.map(module => ({
        id: module.id,
        name: module.name,
        description: module.description || '',
        icon: module.icon || 'FileText',
        color: module.color || 'from-blue-500 to-indigo-500',
        schema_id: module.schema_id,
        config: {
          categories: Array.isArray(module.config?.categories)
            ? module.config.categories
            : ['General'],
          ...module.config
        },
        active: module.active || true,
        created_at: new Date(module.created_at),
        updated_at: new Date(module.updated_at)
      }));
    } catch (error) {
      console.error('Error fetching modules:', error);
      return [];
    }
  }
}
