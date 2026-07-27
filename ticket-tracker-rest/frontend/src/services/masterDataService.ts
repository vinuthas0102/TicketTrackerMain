import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS } from '../lib/apiEndpoints';

export interface MasterItem {
  id: string;
  name: string;
  is_active: boolean;
  display_order: number;
  created_at: string;
  updated_at: string;
}

export interface MasterConfig {
  key: string;
  value: string;
  description: string;
}

export type MasterDataType = 'categories' | 'departments' | 'locations' | 'properties';

const ENDPOINT_MAP: Record<MasterDataType, string> = {
  categories: API_ENDPOINTS.MASTER_DATA.CATEGORIES,
  departments: API_ENDPOINTS.MASTER_DATA.DEPARTMENTS,
  locations: API_ENDPOINTS.MASTER_DATA.LOCATIONS,
  properties: API_ENDPOINTS.MASTER_DATA.PROPERTIES,
};

const FALLBACK_CATEGORIES = [
  'Civil Maintenance',
  'Electrical Maintenance',
  'Plumbing & Sanitary',
  'Carpentry',
  'HVAC / Air Conditioning',
  'Water Supply',
  'Sewage & Drainage',
  'Road & External Area',
  'Housekeeping, Fire & Safety',
  'Security Systems',
  'Street Lighting',
  'Utility Services',
];

const FALLBACK_LOCATIONS = ['Location01', 'Location02'];
const FALLBACK_PROPERTIES = ['PROP001', 'PROP002'];

export class MasterDataService {
  static async getAll(type: MasterDataType, moduleId?: string): Promise<MasterItem[]> {
    try {
      let url = ENDPOINT_MAP[type];
      if (moduleId && type === 'categories') {
        url += `?moduleId=${encodeURIComponent(moduleId)}`;
      }
      return await apiClient.get<MasterItem[]>(url);
    } catch (error) {
      console.error(`Error fetching master ${type}:`, error);
      return this.getFallback(type);
    }
  }

  static async getActive(type: MasterDataType, moduleId?: string): Promise<string[]> {
    const items = await this.getAll(type, moduleId);
    return items.filter(i => i.is_active).map(i => i.name);
  }

  static async add(type: MasterDataType, name: string, moduleId?: string): Promise<MasterItem | null> {
    try {
      const payload: Record<string, unknown> = { name };
      if (moduleId && type === 'categories') {
        payload.moduleId = moduleId;
      }
      return await apiClient.post<MasterItem>(ENDPOINT_MAP[type], payload);
    } catch (error) {
      console.error(`Error adding master ${type}:`, error);
      throw error;
    }
  }

  static async remove(type: MasterDataType, id: string): Promise<void> {
    try {
      await apiClient.delete(`${ENDPOINT_MAP[type]}/${id}`);
    } catch (error) {
      console.error(`Error removing master ${type}:`, error);
      throw error;
    }
  }

  static async toggleActive(type: MasterDataType, id: string, isActive: boolean): Promise<void> {
    try {
      await apiClient.put(`${ENDPOINT_MAP[type]}/${id}`, { is_active: isActive });
    } catch (error) {
      console.error(`Error toggling master ${type}:`, error);
      throw error;
    }
  }

  static async getConfig(key: string): Promise<string | null> {
    try {
      const result = await apiClient.get<MasterConfig>(`${API_ENDPOINTS.MASTER_DATA.CONFIG}?key=${key}`);
      return result?.value || null;
    } catch (error) {
      console.error(`Error fetching config ${key}:`, error);
      return null;
    }
  }

  static async setConfig(key: string, value: string, description?: string): Promise<void> {
    try {
      await apiClient.post(API_ENDPOINTS.MASTER_DATA.CONFIG, { key, value, description });
    } catch (error) {
      console.error(`Error setting config ${key}:`, error);
      throw error;
    }
  }

  static async getModuleCode(moduleId: string): Promise<string> {
    try {
      const result = await apiClient.get<{ moduleId: string; moduleCode: string }>(`${API_ENDPOINTS.MASTER_DATA.MODULE_CODES}?moduleId=${moduleId}`);
      return result?.moduleCode || 'TKT';
    } catch (error) {
      console.error('Error fetching module code:', error);
      return 'TKT';
    }
  }

  static async setModuleCode(moduleId: string, moduleCode: string): Promise<void> {
    try {
      await apiClient.post(API_ENDPOINTS.MASTER_DATA.MODULE_CODES, { moduleId, moduleCode });
    } catch (error) {
      console.error('Error setting module code:', error);
      throw error;
    }
  }

  static async generateTicketNumber(locationPrefix: string, moduleCode: string): Promise<string> {
    try {
      const result = await apiClient.post<{ ticketNumber: string }>('/master-data/generate-ticket-number', {
        locationPrefix: locationPrefix || '',
        moduleCode,
      });
      return result?.ticketNumber || `TKT-${Date.now()}`;
    } catch (error) {
      console.error('Error generating ticket number:', error);
      return `TKT-${Date.now()}`;
    }
  }

  private static getFallback(type: MasterDataType): MasterItem[] {
    let names: string[] = [];
    if (type === 'categories') names = FALLBACK_CATEGORIES;
    else if (type === 'locations') names = FALLBACK_LOCATIONS;
    else if (type === 'properties') names = FALLBACK_PROPERTIES;

    return names.map((name, i) => ({
      id: `fallback-${type}-${i}`,
      name,
      is_active: true,
      display_order: i + 1,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    }));
  }
}
