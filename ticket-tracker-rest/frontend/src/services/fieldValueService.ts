import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS } from '../lib/apiEndpoints';

export class FieldValueService {
  static async getFieldValues(ticketId: string): Promise<Record<string, any>> {
    try {
      const values = await apiClient.get<Record<string, any>>(
        API_ENDPOINTS.FIELD_VALUES.GET(ticketId)
      );

      return values || {};
    } catch (error) {
      console.error('Error fetching field values:', error);
      return {};
    }
  }

  static async saveFieldValues(
    ticketId: string,
    values: Record<string, any>,
    userId: string
  ): Promise<void> {
    try {
      await apiClient.post(API_ENDPOINTS.FIELD_VALUES.SAVE(ticketId), {
        values,
        userId,
      });
    } catch (error) {
      console.error('Error saving field values:', error);
      throw error;
    }
  }
}
