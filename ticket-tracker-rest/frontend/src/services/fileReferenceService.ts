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
  stepId: string;
  templateId: string;
  referenceName: string;
  isMandatory: boolean;
  documentId?: string;
  uploadedBy?: string;
  uploadedAt?: Date;
  documentName?: string;
  documentSize?: number;
  createdAt?: Date;
  updatedAt?: Date;
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
        id: ref.id,
        stepId: ref.stepId || ref.step_id,
        templateId: ref.templateId || ref.template_id,
        referenceName: ref.referenceName || ref.reference_name,
        isMandatory: ref.isMandatory !== undefined ? ref.isMandatory : ref.is_mandatory,
        documentId: ref.documentId || ref.uploaded_file_id || ref.document_id,
        uploadedBy: ref.uploadedBy || ref.uploaded_by,
        uploadedAt: ref.uploadedAt || ref.uploaded_at ? new Date(ref.uploadedAt || ref.uploaded_at) : undefined,
        documentName: ref.documentName || ref.document_name,
        documentSize: ref.documentSize || ref.document_size,
        createdAt: ref.createdAt ? new Date(ref.createdAt) : undefined,
        updatedAt: ref.updatedAt ? new Date(ref.updatedAt) : undefined,
      }));
    } catch (error) {
      console.error('Error fetching step file references:', error);
      return [];
    }
  }

  static async updateStepFileReference(
    referenceId: string,
    input: { documentId?: string; uploadedBy?: string; uploadedAt?: Date }
  ): Promise<void> {
    try {
      await apiClient.put(API_ENDPOINTS.FILE_REFERENCES.UPDATE_STATUS(referenceId), {
        documentId: input.documentId,
        uploadedBy: input.uploadedBy,
        uploadedAt: input.uploadedAt?.toISOString(),
        status: 'uploaded',
      });
    } catch (error) {
      console.error('Error updating step file reference:', error);
      throw error;
    }
  }

  static async checkMandatoryReferencesComplete(stepId: string): Promise<boolean> {
    try {
      const refs = await this.getStepFileReferences(stepId);
      return refs.filter(ref => ref.isMandatory).every(ref => !!ref.documentId);
    } catch (error) {
      console.error('Error checking mandatory references:', error);
      return false;
    }
  }

  static async getIncompleteReferences(stepId: string): Promise<WorkflowStepFileReference[]> {
    try {
      const refs = await this.getStepFileReferences(stepId);
      return refs.filter(ref => ref.isMandatory && !ref.documentId);
    } catch (error) {
      console.error('Error getting incomplete references:', error);
      return [];
    }
  }

  static validateTemplateJSON(json: string): { valid: boolean; error?: string } {
    try {
      const parsed = JSON.parse(json);
      if (!parsed.fileReferences || !Array.isArray(parsed.fileReferences)) {
        return { valid: false, error: 'fileReferences array is required' };
      }
      for (const ref of parsed.fileReferences) {
        if (!ref.referenceName || typeof ref.referenceName !== 'string') {
          return { valid: false, error: 'Each reference must have a referenceName' };
        }
        if (typeof ref.isMandatory !== 'boolean') {
          return { valid: false, error: 'Each reference must have isMandatory boolean' };
        }
      }
      return { valid: true };
    } catch (e) {
      return { valid: false, error: 'Invalid JSON format' };
    }
  }

  static async createStepFileReferences(
    stepId: string,
    templateId: string,
    userId: string
  ): Promise<WorkflowStepFileReference[]> {
    try {
      const references = await apiClient.post<WorkflowStepFileReference[]>(
        API_ENDPOINTS.FILE_REFERENCES.BY_STEP(stepId),
        { templateId, userId }
      );
      return references || [];
    } catch (error) {
      console.error('Error creating step file references:', error);
      throw error;
    }
  }
}
