import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS } from '../lib/apiEndpoints';

export interface UserDisplayPreferences {
  userId: string;
  iconDisplayMode: 'horizontal-toolbar' | 'vertical-sidebar' | 'dropdown-menu' | 'floating-action' | 'grid' | 'carousel';
}

export class UserPreferencesService {
  static async getUserPreferences(userId: string): Promise<UserDisplayPreferences | null> {
    try {
      const prefs = await apiClient.get<UserDisplayPreferences>(
        API_ENDPOINTS.USER_PREFERENCES.GET,
        { userId }
      );

      return prefs;
    } catch (error) {
      console.error('Error fetching user preferences:', error);
      return null;
    }
  }

  static async saveUserPreferences(preferences: UserDisplayPreferences): Promise<void> {
    try {
      await apiClient.post(API_ENDPOINTS.USER_PREFERENCES.SAVE, preferences);
    } catch (error) {
      console.error('Error saving user preferences:', error);
      throw error;
    }
  }
}
