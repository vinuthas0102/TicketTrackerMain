import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS } from '../lib/apiEndpoints';
import { transformUserFromBackend, transformUserToBackend } from '../lib/transformers/dataTransformer';
import { User } from '../types';

export class UserManagementService {
  static async getAllUsers(): Promise<User[]> {
    try {
      const users = await apiClient.get<any[]>(API_ENDPOINTS.USERS.LIST);
      return users.map(transformUserFromBackend);
    } catch (error) {
      console.error('Error fetching users:', error);
      return [];
    }
  }

  static async getUserById(userId: string): Promise<User | null> {
    try {
      const user = await apiClient.get<any>(API_ENDPOINTS.USERS.GET(userId));
      return transformUserFromBackend(user);
    } catch (error) {
      console.error('Error fetching user:', error);
      return null;
    }
  }

  static async createUser(userData: Partial<User>, createdBy: string): Promise<string> {
    try {
      const payload = {
        ...transformUserToBackend(userData),
        createdBy,
      };

      const response = await apiClient.post<{ id: string }>(API_ENDPOINTS.USERS.CREATE, payload);
      return response.id;
    } catch (error) {
      console.error('Error creating user:', error);
      throw error;
    }
  }

  static async updateUser(userId: string, updates: Partial<User>, updatedBy: string): Promise<void> {
    try {
      const payload = {
        ...transformUserToBackend(updates),
        updatedBy,
      };

      await apiClient.put(API_ENDPOINTS.USERS.UPDATE(userId), payload);
    } catch (error) {
      console.error('Error updating user:', error);
      throw error;
    }
  }

  static async deleteUser(userId: string, deletedBy: string): Promise<void> {
    try {
      await apiClient.delete(API_ENDPOINTS.USERS.DELETE(userId) + `?deletedBy=${deletedBy}`);
    } catch (error) {
      console.error('Error deleting user:', error);
      throw error;
    }
  }

  static async enableUser(userId: string, enabledBy: string): Promise<void> {
    try {
      await apiClient.put(API_ENDPOINTS.USERS.ENABLE(userId), { enabledBy });
    } catch (error) {
      console.error('Error enabling user:', error);
      throw error;
    }
  }

  static async disableUser(userId: string, disabledBy: string, reason?: string): Promise<void> {
    try {
      await apiClient.put(API_ENDPOINTS.USERS.DISABLE(userId), { disabledBy, reason });
    } catch (error) {
      console.error('Error disabling user:', error);
      throw error;
    }
  }

  static async lockUser(userId: string, lockedBy: string, lockDuration: number): Promise<void> {
    try {
      await apiClient.put(API_ENDPOINTS.USERS.LOCK(userId), {
        lockedBy,
        lockDuration,
      });
    } catch (error) {
      console.error('Error locking user:', error);
      throw error;
    }
  }

  static async unlockUser(userId: string, unlockedBy: string): Promise<void> {
    try {
      await apiClient.put(API_ENDPOINTS.USERS.UNLOCK(userId), { unlockedBy });
    } catch (error) {
      console.error('Error unlocking user:', error);
      throw error;
    }
  }
}
