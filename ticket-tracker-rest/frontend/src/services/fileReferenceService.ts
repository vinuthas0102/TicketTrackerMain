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

function parseJsonContent(jsonContent: any): any {
  if (typeof jsonContent === 'string') {
    try {
      return JSON.parse(jsonContent);
    } catch (error) {
      console.error('Failed to parse jsonContent string:', error);
      return { fileReferences: [], mandatoryFlags: [] };
    }
  }

  if (typeof jsonContent === 'object' && jsonContent !== null) {
    return jsonContent;
  }

  return { fileReferences: [], mandatoryFlags: [] };
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
  static async getTemplates(): Promise<FileReferenceTemplate[]> {
    try {
      const templates = await apiClient.get<any[]>(API_ENDPOINTS.FILE_REFERENCES.TEMPLATES);

      return templates.map(template => ({
        ...template,
        jsonContent: parseJsonContent(template.jsonContent),
        created_at: new Date(template.created_at || template.createdAt),
        updated_at: new Date(template.updated_at || template.updatedAt),
      }));
    } catch (error) {
      console.error('Error fetching file reference templates:', error);
      return [];
    }
  }

  static async getAllTemplates(activeOnly?: boolean): Promise<FileReferenceTemplate[]> {
    try {
      const queryParam = activeOnly !== undefined ? `?activeOnly=${activeOnly}` : '';
      const templates = await apiClient.get<any[]>(API_ENDPOINTS.FILE_REFERENCES.TEMPLATES + queryParam);

      return templates.map(template => ({
        ...template,
        jsonContent: parseJsonContent(template.jsonContent),
        created_at: new Date(template.created_at || template.createdAt),
        updated_at: new Date(template.updated_at || template.updatedAt),
      }));
    } catch (error) {
      console.error('Error fetching file reference templates:', error);
      return [];
    }
  }

  static async getTemplateById(templateId: string): Promise<FileReferenceTemplate | null> {
    try {
      const template = await apiClient.get<any>(API_ENDPOINTS.FILE_REFERENCES.TEMPLATE(templateId));

      return {
        ...template,
        jsonContent: parseJsonContent(template.jsonContent),
        created_at: new Date(template.created_at || template.createdAt),
        updated_at: new Date(template.updated_at || template.updatedAt),
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
      const response = await apiClient.post<{ id: string }>(API_ENDPOINTS.FILE_REFERENCES.TEMPLATES, {
        ...templateData,
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
      await apiClient.put(API_ENDPOINTS.FILE_REFERENCES.TEMPLATE(templateId), {
        ...updates,
        updatedBy,
      });
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
