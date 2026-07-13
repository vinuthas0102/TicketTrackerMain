import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS } from '../lib/apiEndpoints';
import { UserDisplayPreferences, IconDisplayType, IconSize } from '../types';

interface ApiPreferencesResponse {
  userId?: string;
  iconDisplayType?: string;
  iconDisplayMode?: string;
  iconSize?: string;
  showLabels?: boolean;
  groupByCategory?: boolean;
  animationEnabled?: boolean;
}

interface SaveResult {
  success: boolean;
  data?: UserDisplayPreferences;
  error?: string;
}

const iconDisplayModeMap: Record<string, IconDisplayType> = {
  'dropdown-menu': 'dropdown_menu',
  'vertical-sidebar': 'vertical_sidebar',
  'horizontal-toolbar': 'horizontal_toolbar',
  'floating-action': 'floating_action',
  'grid': 'grid',
  'carousel': 'carousel',
};

function mapApiResponse(raw: ApiPreferencesResponse): UserDisplayPreferences {
  const rawType = raw.iconDisplayType || raw.iconDisplayMode || 'dropdown_menu';
  const iconDisplayType: IconDisplayType = (iconDisplayModeMap[rawType] || rawType) as IconDisplayType;

  return {
    id: '',
    userId: raw.userId || '',
    iconDisplayType,
    iconSize: (raw.iconSize || 'medium') as IconSize,
    showLabels: raw.showLabels !== undefined ? raw.showLabels : true,
    groupByCategory: raw.groupByCategory !== undefined ? raw.groupByCategory : false,
    animationEnabled: raw.animationEnabled !== undefined ? raw.animationEnabled : true,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

let cache: UserDisplayPreferences | null = null;

export class UserPreferencesService {
  static getDefaultPreferences(): UserDisplayPreferences {
    return {
      id: '',
      userId: '',
      iconDisplayType: 'dropdown_menu',
      iconSize: 'medium',
      showLabels: true,
      groupByCategory: false,
      animationEnabled: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    };
  }

  static async getUserPreferences(userId: string): Promise<UserDisplayPreferences | null> {
    if (cache) return cache;

    try {
      const raw = await apiClient.get<ApiPreferencesResponse>(
        API_ENDPOINTS.USER_PREFERENCES.GET,
        { userId }
      );

      if (!raw) return null;

      const prefs = mapApiResponse({ ...raw, userId });
      cache = prefs;
      return prefs;
    } catch (error) {
      console.error('Error fetching user preferences:', error);
      return null;
    }
  }

  static async initializeDefaultPreferences(userId: string): Promise<UserDisplayPreferences | null> {
    const defaults = UserPreferencesService.getDefaultPreferences();
    defaults.userId = userId;

    try {
      await apiClient.post(API_ENDPOINTS.USER_PREFERENCES.SAVE, {
        userId,
        iconDisplayType: defaults.iconDisplayType,
        iconSize: defaults.iconSize,
        showLabels: defaults.showLabels,
        groupByCategory: defaults.groupByCategory,
        animationEnabled: defaults.animationEnabled,
      });

      cache = defaults;
      return defaults;
    } catch {
      cache = defaults;
      return defaults;
    }
  }

  static async saveUserPreferences(
    userId: string,
    preferences: Partial<Omit<UserDisplayPreferences, 'id' | 'userId' | 'createdAt' | 'updatedAt'>>
  ): Promise<SaveResult> {
    try {
      const current = cache || UserPreferencesService.getDefaultPreferences();
      const updated: UserDisplayPreferences = {
        ...current,
        ...preferences,
        userId,
        updatedAt: new Date(),
      };

      await apiClient.post(API_ENDPOINTS.USER_PREFERENCES.SAVE, {
        userId,
        iconDisplayType: updated.iconDisplayType,
        iconSize: updated.iconSize,
        showLabels: updated.showLabels,
        groupByCategory: updated.groupByCategory,
        animationEnabled: updated.animationEnabled,
      });

      cache = updated;
      return { success: true, data: updated };
    } catch (error) {
      console.error('Error saving user preferences:', error);
      return { success: false, error: 'Failed to save preferences' };
    }
  }

  static clearCache(): void {
    cache = null;
  }
}
