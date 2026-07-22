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

  static async updateTicketFieldValue(
    ticketId: string,
    fieldKey: string,
    fieldValue: any
  ): Promise<void> {
    try {
      await apiClient.post(API_ENDPOINTS.FIELD_VALUES.SAVE(ticketId), {
        values: { [fieldKey]: fieldValue },
      });
    } catch (error) {
      console.error('Error updating ticket field value:', error);
      throw error;
    }
  }

  static async deleteTicketFieldValue(ticketId: string, fieldKey: string): Promise<void> {
    try {
      await apiClient.delete(API_ENDPOINTS.FIELD_VALUES.DELETE(ticketId, fieldKey));
    } catch (error) {
      console.error('Error deleting ticket field value:', error);
      throw error;
    }
  }

  static async getWorkflowStepFieldValues(stepId: string): Promise<Record<string, any>> {
    try {
      const values = await apiClient.get<Record<string, any>>(
        API_ENDPOINTS.FIELD_VALUES.GET(stepId)
      );
      return values || {};
    } catch (error) {
      console.error('Error fetching workflow step field values:', error);
      return {};
    }
  }

  static async saveWorkflowStepFieldValues(
    stepId: string,
    values: Record<string, any>
  ): Promise<void> {
    try {
      await apiClient.post(API_ENDPOINTS.FIELD_VALUES.SAVE(stepId), { values });
    } catch (error) {
      console.error('Error saving workflow step field values:', error);
      throw error;
    }
  }

  static async updateWorkflowStepFieldValue(
    stepId: string,
    fieldKey: string,
    fieldValue: any
  ): Promise<void> {
    try {
      await apiClient.post(API_ENDPOINTS.FIELD_VALUES.SAVE(stepId), {
        values: { [fieldKey]: fieldValue },
      });
    } catch (error) {
      console.error('Error updating workflow step field value:', error);
      throw error;
    }
  }

  static async deleteWorkflowStepFieldValue(stepId: string, fieldKey: string): Promise<void> {
    try {
      await apiClient.delete(API_ENDPOINTS.FIELD_VALUES.DELETE(stepId, fieldKey));
    } catch (error) {
      console.error('Error deleting workflow step field value:', error);
      throw error;
    }
  }

  static async getBatchTicketFieldValues(
    ticketIds: string[]
  ): Promise<Record<string, Record<string, any>>> {
    try {
      const result: Record<string, Record<string, any>> = {};
      await Promise.all(
        ticketIds.map(async id => {
          result[id] = await this.getFieldValues(id);
        })
      );
      return result;
    } catch (error) {
      console.error('Error fetching batch ticket field values:', error);
      return {};
    }
  }

  static async getBatchWorkflowStepFieldValues(
    stepIds: string[]
  ): Promise<Record<string, Record<string, any>>> {
    try {
      const result: Record<string, Record<string, any>> = {};
      await Promise.all(
        stepIds.map(async id => {
          result[id] = await this.getWorkflowStepFieldValues(id);
        })
      );
      return result;
    } catch (error) {
      console.error('Error fetching batch workflow step field values:', error);
      return {};
    }
  }
}
