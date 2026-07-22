import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS } from '../lib/apiEndpoints';

export interface FieldConfig {
  id: string;
  module_id: string;
  field_name: string;
  field_type: string;
  label: string;
  placeholder?: string;
  required: boolean;
  options?: string[];
  validation_rules?: any;
  display_order: number;
  is_active: boolean;
  created_at: Date;
  updated_at: Date;
}

export class FieldConfigService {
  static async getFieldConfigs(moduleId?: string): Promise<FieldConfig[]> {
    try {
      const endpoint = moduleId
        ? API_ENDPOINTS.FIELD_CONFIG.BY_MODULE(moduleId)
        : API_ENDPOINTS.FIELD_CONFIG.LIST;

      const configs = await apiClient.get<any[]>(endpoint);

      return configs.map(config => ({
        ...config,
        created_at: new Date(config.created_at),
        updated_at: new Date(config.updated_at),
      }));
    } catch (error) {
      console.error('Error fetching field configs:', error);
      return [];
    }
  }

  static async getFieldConfigById(configId: string): Promise<FieldConfig | null> {
    try {
      const config = await apiClient.get<any>(API_ENDPOINTS.FIELD_CONFIG.GET(configId));

      return {
        ...config,
        created_at: new Date(config.created_at),
        updated_at: new Date(config.updated_at),
      };
    } catch (error) {
      console.error('Error fetching field config:', error);
      return null;
    }
  }

  static async createFieldConfig(configData: Partial<FieldConfig>, createdBy: string): Promise<string> {
    try {
      const response = await apiClient.post<{ id: string }>(API_ENDPOINTS.FIELD_CONFIG.CREATE, {
        ...configData,
        createdBy,
      });

      return response.id;
    } catch (error) {
      console.error('Error creating field config:', error);
      throw error;
    }
  }

  static async updateFieldConfig(
    configId: string,
    updates: Partial<FieldConfig>,
    updatedBy: string
  ): Promise<void> {
    try {
      await apiClient.put(API_ENDPOINTS.FIELD_CONFIG.UPDATE(configId), {
        ...updates,
        updatedBy,
      });
    } catch (error) {
      console.error('Error updating field config:', error);
      throw error;
    }
  }

  static async deleteFieldConfig(configId: string, deletedBy: string): Promise<void> {
    try {
      await apiClient.delete(API_ENDPOINTS.FIELD_CONFIG.DELETE(configId) + `?deletedBy=${deletedBy}`);
    } catch (error) {
      console.error('Error deleting field config:', error);
      throw error;
    }
  }

  static async reorderFields(moduleId: string, orderedIds: string[]): Promise<void> {
    try {
      await apiClient.put(`${API_ENDPOINTS.FIELD_CONFIG.REORDER}?moduleId=${moduleId}`, orderedIds);
    } catch (error) {
      console.error('Error reordering fields:', error);
      throw error;
    }
  }

  static async getFieldDropdownOptions(fieldConfigId: string): Promise<any[]> {
    try {
      return await apiClient.get<any[]>(API_ENDPOINTS.FIELD_CONFIG.OPTIONS(fieldConfigId));
    } catch (error) {
      console.error('Error fetching dropdown options:', error);
      return [];
    }
  }

  static async createDropdownOption(option: any): Promise<any> {
    try {
      return await apiClient.post(API_ENDPOINTS.FIELD_CONFIG.CREATE_OPTION, option);
    } catch (error) {
      console.error('Error creating dropdown option:', error);
      throw error;
    }
  }

  static async updateDropdownOption(optionId: string, updates: any): Promise<void> {
    try {
      await apiClient.put(API_ENDPOINTS.FIELD_CONFIG.UPDATE_OPTION(optionId), updates);
    } catch (error) {
      console.error('Error updating dropdown option:', error);
      throw error;
    }
  }

  static async deleteDropdownOption(optionId: string): Promise<void> {
    try {
      await apiClient.delete(API_ENDPOINTS.FIELD_CONFIG.DELETE_OPTION(optionId));
    } catch (error) {
      console.error('Error deleting dropdown option:', error);
      throw error;
    }
  }

  static validateFieldValue(value: any, config: FieldConfig): { valid: boolean; error?: string } {
    if (config.required && (value === null || value === undefined || value === '')) {
      return { valid: false, error: `${config.label} is required` };
    }
    return { valid: true };
  }
}
