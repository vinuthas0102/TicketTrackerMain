import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS } from '../lib/apiEndpoints';

export interface FileReferenceTemplate {
  id: string;
  name: string;
  description?: string;
  references: FileReferenceItem[];
  created_at: Date;
  updated_at: Date;
}

export interface FileReferenceItem {
  referenceName: string;
  isMandatory: boolean;
}

export interface WorkflowStepFileReference {
  id: string;
  step_id: string;
  template_id: string;
  reference_name: string;
  is_mandatory: boolean;
  uploaded_file_id?: string;
  uploaded_at?: Date;
  uploaded_by?: string;
  status: 'pending' | 'uploaded' | 'approved' | 'rejected';
}

export class FileReferenceService {
  static validateTemplateJSON(jsonContent: any): { valid: boolean; error?: string } {
    try {
      if (typeof jsonContent !== 'object' || jsonContent === null) {
        return { valid: false, error: 'JSON content must be an object' };
      }

      if (!jsonContent.fileReferences) {
        return { valid: false, error: "JSON content must have 'fileReferences' field" };
      }

      if (!Array.isArray(jsonContent.fileReferences)) {
        return { valid: false, error: "'fileReferences' must be an array" };
      }

      if (jsonContent.fileReferences.length === 0) {
        return { valid: false, error: "'fileReferences' array must not be empty" };
      }

      for (let i = 0; i < jsonContent.fileReferences.length; i++) {
        const ref = jsonContent.fileReferences[i];
        if (typeof ref !== 'string' || ref.trim() === '') {
          return { valid: false, error: `File reference at index ${i} must be a non-empty string` };
        }
      }

      if (jsonContent.mandatoryFlags !== undefined) {
        if (!Array.isArray(jsonContent.mandatoryFlags)) {
          return { valid: false, error: "'mandatoryFlags' must be an array" };
        }

        if (jsonContent.mandatoryFlags.length !== jsonContent.fileReferences.length) {
          return { valid: false, error: "'mandatoryFlags' array length must match 'fileReferences' array length" };
        }

        for (let i = 0; i < jsonContent.mandatoryFlags.length; i++) {
          const flag = jsonContent.mandatoryFlags[i];
          if (typeof flag !== 'boolean') {
            return { valid: false, error: `Mandatory flag at index ${i} must be a boolean` };
          }
        }
      }

      return { valid: true };
    } catch (error) {
      return { valid: false, error: `Invalid JSON format: ${error}` };
    }
  }

  static async getAllTemplates(activeOnly: boolean = false): Promise<FileReferenceTemplate[]> {
    try {
      const url = activeOnly
        ? `${API_ENDPOINTS.FILE_REFERENCES.TEMPLATES}?activeOnly=true`
        : API_ENDPOINTS.FILE_REFERENCES.TEMPLATES;

      const templates = await apiClient.get<any[]>(url);

      return templates.map(template => {
        const jsonContent = typeof template.jsonContent === 'string'
          ? JSON.parse(template.jsonContent)
          : template.jsonContent;

        const fileReferences = jsonContent.fileReferences || [];
        const mandatoryFlags = jsonContent.mandatoryFlags || [];

        const references: FileReferenceItem[] = fileReferences.map((referenceName: string, index: number) => ({
          referenceName,
          isMandatory: mandatoryFlags[index] !== undefined ? mandatoryFlags[index] : false,
        }));

        return {
          id: template.id,
          name: template.templateName || template.name,
          description: template.description,
          references,
          created_at: new Date(template.createdAt || template.created_at),
          updated_at: new Date(template.updatedAt || template.updated_at),
        };
      });
    } catch (error) {
      console.error('Error fetching file reference templates:', error);
      return [];
    }
  }

  static async getTemplates(): Promise<FileReferenceTemplate[]> {
    return this.getAllTemplates(false);
  }

  static async getTemplateById(templateId: string): Promise<FileReferenceTemplate | null> {
    try {
      const template = await apiClient.get<any>(API_ENDPOINTS.FILE_REFERENCES.TEMPLATE(templateId));

      const jsonContent = typeof template.jsonContent === 'string'
        ? JSON.parse(template.jsonContent)
        : template.jsonContent;

      const fileReferences = jsonContent.fileReferences || [];
      const mandatoryFlags = jsonContent.mandatoryFlags || [];

      const references: FileReferenceItem[] = fileReferences.map((referenceName: string, index: number) => ({
        referenceName,
        isMandatory: mandatoryFlags[index] !== undefined ? mandatoryFlags[index] : false,
      }));

      return {
        id: template.id,
        name: template.templateName || template.name,
        description: template.description,
        references,
        created_at: new Date(template.createdAt || template.created_at),
        updated_at: new Date(template.updatedAt || template.updated_at),
      };
    } catch (error) {
      console.error('Error fetching file reference template:', error);
      return null;
    }
  }

  static async createTemplate(
    templateData: Partial<FileReferenceTemplate>,
    createdBy: string
  ): Promise<string> {
    try {
      const fileReferences = templateData.references?.map(ref => ref.referenceName) || [];
      const mandatoryFlags = templateData.references?.map(ref => ref.isMandatory) || [];

      const jsonContent = {
        fileReferences,
        mandatoryFlags,
      };

      const validationResult = this.validateTemplateJSON(jsonContent);
      if (!validationResult.valid) {
        throw new Error(validationResult.error);
      }

      const response = await apiClient.post<{ id: string }>(API_ENDPOINTS.FILE_REFERENCES.TEMPLATES, {
        name: templateData.name,
        description: templateData.description,
        jsonContent,
        createdBy,
      });

      return response.id;
    } catch (error) {
      console.error('Error creating file reference template:', error);
      throw error;
    }
  }

  static async updateTemplate(
    templateId: string,
    updates: Partial<FileReferenceTemplate>,
    updatedBy: string
  ): Promise<void> {
    try {
      const payload: any = {
        updatedBy,
      };

      if (updates.name) {
        payload.name = updates.name;
      }

      if (updates.description !== undefined) {
        payload.description = updates.description;
      }

      if (updates.references) {
        const fileReferences = updates.references.map(ref => ref.referenceName);
        const mandatoryFlags = updates.references.map(ref => ref.isMandatory);

        const jsonContent = {
          fileReferences,
          mandatoryFlags,
        };

        const validationResult = this.validateTemplateJSON(jsonContent);
        if (!validationResult.valid) {
          throw new Error(validationResult.error);
        }

        payload.jsonContent = jsonContent;
      }

      await apiClient.put(API_ENDPOINTS.FILE_REFERENCES.TEMPLATE(templateId), payload);
    } catch (error) {
      console.error('Error updating file reference template:', error);
      throw error;
    }
  }

  static async deleteTemplate(templateId: string, deletedBy: string): Promise<void> {
    try {
      await apiClient.delete(
        API_ENDPOINTS.FILE_REFERENCES.TEMPLATE(templateId) + `?deletedBy=${deletedBy}`
      );
    } catch (error) {
      console.error('Error deleting file reference template:', error);
      throw error;
    }
  }

  static async getStepFileReferences(stepId: string): Promise<WorkflowStepFileReference[]> {
    try {
      const references = await apiClient.get<any[]>(API_ENDPOINTS.FILE_REFERENCES.BY_STEP(stepId));

      return references.map(ref => ({
        ...ref,
        uploaded_at: ref.uploaded_at ? new Date(ref.uploaded_at) : undefined,
      }));
    } catch (error) {
      console.error('Error fetching step file references:', error);
      return [];
    }
  }

  static async updateFileReferenceStatus(
    referenceId: string,
    status: string,
    updatedBy: string,
    file?: File
  ): Promise<void> {
    try {
      if (file) {
        await apiClient.uploadFile(
          API_ENDPOINTS.FILE_REFERENCES.UPDATE_STATUS(referenceId),
          file,
          {
            status,
            updatedBy,
          }
        );
      } else {
        await apiClient.put(API_ENDPOINTS.FILE_REFERENCES.UPDATE_STATUS(referenceId), {
          status,
          updatedBy,
        });
      }
    } catch (error) {
      console.error('Error updating file reference status:', error);
      throw error;
    }
  }
}
