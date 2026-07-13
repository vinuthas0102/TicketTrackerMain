import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS, API_BASE_URL } from '../lib/apiEndpoints';

export interface FileUploadOptions {
  file: File;
  stepId?: string;
  ticketId: string;
  userId: string;
  isMandatory: boolean;
  isCompletionCertificate?: boolean;
  fileReferenceId?: string;
}

export interface FileUploadProgress {
  loaded: number;
  total: number;
  percentage: number;
}

export interface DocumentMetadata {
  id: string;
  name: string;
  type: string;
  size: number;
  url: string | null;
  storagePath: string | null;
  uploadedBy: string;
  uploadedAt: Date;
  isMandatory: boolean;
  isCompletionCertificate?: boolean;
  stepId: string;
}

export interface ProgressDocumentMetadata {
  id: string;
  stepId: string;
  ticketId: string;
  auditLogId?: string;
  fileName: string;
  filePath: string;
  fileSize: number;
  fileType: string;
  uploadedBy: string;
  uploadedAt: Date;
  isDeleted: boolean;
  deletedAt?: Date;
  deletedBy?: string;
  deleteReason?: string;
}

const MAX_FILE_SIZE = 5 * 1024 * 1024;

const ALLOWED_FILE_TYPES = [
  'application/pdf',
  'image/jpeg',
  'image/jpg',
  'image/png',
  'image/gif',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
];

export class FileService {
  static validateFile(file: File): { valid: boolean; error?: string } {
    if (file.size > MAX_FILE_SIZE) {
      return {
        valid: false,
        error: `File size exceeds 5MB limit. Your file is ${(file.size / (1024 * 1024)).toFixed(2)}MB`,
      };
    }

    if (!ALLOWED_FILE_TYPES.includes(file.type)) {
      return {
        valid: false,
        error: `File type ${file.type} is not supported. Allowed types: PDF, Images (JPG, PNG, GIF), Word (DOC, DOCX), Excel (XLS, XLSX)`,
      };
    }

    return { valid: true };
  }

  static async uploadStepDocument(
    options: FileUploadOptions,
    onProgress?: (progress: FileUploadProgress) => void
  ): Promise<DocumentMetadata> {
    const { file, stepId, ticketId, userId, isMandatory, isCompletionCertificate = false, fileReferenceId } = options;

    console.log('[FileService] uploadStepDocument called with:', {
      fileName: file.name,
      fileSize: file.size,
      fileType: file.type,
      ticketId,
      stepId,
      userId,
      isMandatory,
      isCompletionCertificate,
      fileReferenceId
    });

    const validation = this.validateFile(file);
    if (!validation.valid) {
      console.error('[FileService] File validation failed:', validation.error);
      throw new Error(validation.error);
    }

    console.log('[FileService] File validation passed');

    try {
      if (onProgress) {
        onProgress({ loaded: 0, total: file.size, percentage: 0 });
      }

      console.log('[FileService] Calling API endpoint:', API_ENDPOINTS.FILES.UPLOAD);

      const additionalData: Record<string, any> = {
        ticketId,
        userId,
        isMandatory: isMandatory.toString(),
        isCompletionCertificate: isCompletionCertificate.toString(),
      };

      if (stepId) {
        additionalData.stepId = stepId;
      }

      if (fileReferenceId) {
        additionalData.fileReferenceId = fileReferenceId;
      }

      const response = await apiClient.uploadFile<DocumentMetadata>(
        API_ENDPOINTS.FILES.UPLOAD,
        file,
        additionalData
      );

      console.log('[FileService] Upload successful:', response);

      if (onProgress) {
        onProgress({ loaded: file.size, total: file.size, percentage: 100 });
      }

      const raw = response as any;
      return {
        id: raw.id,
        name: raw.name || raw.fileName,
        type: raw.type || raw.fileType,
        size: raw.size || raw.fileSize,
        url: raw.url || null,
        storagePath: raw.storagePath || raw.storage_path || null,
        uploadedBy: raw.uploadedBy,
        uploadedAt: new Date(raw.uploadedAt),
        isMandatory: raw.isMandatory ?? raw.mandatory ?? false,
        isCompletionCertificate: raw.isCompletionCertificate ?? raw.completionCertificate ?? false,
        stepId: raw.stepId,
      };
    } catch (error) {
      console.error('[FileService] File upload failed:', error);
      throw error;
    }
  }

  static async getStepDocuments(stepId: string): Promise<DocumentMetadata[]> {
    try {
      const response = await apiClient.get<DocumentMetadata[]>(
        API_ENDPOINTS.WORKFLOW_STEPS.FILES(stepId)
      );

      return response.map((doc: any) => ({
        id: doc.id,
        name: doc.name || doc.fileName,
        type: doc.type || doc.fileType || 'application/octet-stream',
        size: doc.size || doc.fileSize,
        url: doc.url || null,
        storagePath: doc.storagePath || doc.storage_path || null,
        uploadedBy: doc.uploadedBy,
        uploadedAt: new Date(doc.uploadedAt),
        isMandatory: doc.isMandatory ?? doc.mandatory ?? false,
        isCompletionCertificate: doc.isCompletionCertificate ?? doc.completionCertificate ?? false,
        stepId: doc.stepId,
      }));
    } catch (error) {
      console.error('Failed to fetch step documents:', error);
      return [];
    }
  }

  static async getTicketAttachments(ticketId: string): Promise<DocumentMetadata[]> {
    try {
      const response = await apiClient.get<DocumentMetadata[]>(
        API_ENDPOINTS.TICKETS.FILES(ticketId)
      );

      return response.map((doc: any) => ({
        id: doc.id,
        name: doc.name || doc.fileName,
        type: doc.type || doc.fileType || 'application/octet-stream',
        size: doc.size || doc.fileSize,
        url: doc.url || null,
        storagePath: doc.storagePath || doc.storage_path || null,
        uploadedBy: doc.uploadedBy,
        uploadedAt: new Date(doc.uploadedAt),
        isMandatory: doc.isMandatory ?? doc.mandatory ?? false,
        isCompletionCertificate: doc.isCompletionCertificate ?? doc.completionCertificate ?? false,
        stepId: doc.stepId,
      }));
    } catch (error) {
      console.error('Failed to fetch ticket attachments:', error);
      return [];
    }
  }

  static async getFileUrl(documentId: string, _expiresIn: number = 3600): Promise<string> {
    return `${API_BASE_URL}${API_ENDPOINTS.FILES.DOWNLOAD(documentId)}`;
  }

  static async deleteDocument(documentId: string, userId: string): Promise<void> {
    try {
      await apiClient.delete(API_ENDPOINTS.FILES.DELETE(documentId) + `?userId=${userId}`);
    } catch (error) {
      console.error('Delete document failed:', error);
      throw error;
    }
  }

  static async copyTicketAttachments(
    sourceTicketId: string,
    targetTicketId: string,
    userId: string,
    attachmentIds?: string[]
  ): Promise<{ successCount: number; failedCount: number; errors: string[] }> {
    try {
      const response = await apiClient.post<{ successCount: number; failedCount: number; errors: string[] }>(
        `/tickets/${targetTicketId}/copy-attachments`,
        {
          sourceTicketId,
          userId,
          attachmentIds: attachmentIds || [],
        }
      );

      return response;
    } catch (error) {
      console.error('Copy attachments failed:', error);
      return {
        successCount: 0,
        failedCount: 0,
        errors: [error instanceof Error ? error.message : 'Copy failed'],
      };
    }
  }

  static getFileIcon(fileType: string | null | undefined): string {
    if (!fileType) return '📎';
    if (fileType.startsWith('image/')) return '�️';
    if (fileType === 'application/pdf') return '�';
    if (fileType.includes('word')) return '�';
    if (fileType.includes('excel') || fileType.includes('spreadsheet')) return '�';
    return '�';
  }

  static formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }

  static isImageFile(fileType: string): boolean {
    return fileType.startsWith('image/');
  }

  static isPDFFile(fileType: string): boolean {
    return fileType === 'application/pdf';
  }

  static canPreview(fileType: string): boolean {
    return this.isImageFile(fileType) || this.isPDFFile(fileType);
  }

  static async uploadFile(
    file: File,
    ticketId: string,
    userId: string,
    stepId?: string
  ): Promise<string> {
    try {
      const response = await apiClient.uploadFile<{ id: string }>(
        API_ENDPOINTS.FILES.UPLOAD,
        file,
        {
          ticketId,
          userId,
          stepId: stepId || null,
        }
      );

      return response.id;
    } catch (error) {
      console.error('Error uploading file:', error);
      throw error;
    }
  }

  static async uploadProgressDocument(
    file: File,
    stepId: string,
    ticketId: string,
    userId: string,
    auditLogId: string
  ): Promise<string> {
    try {
      const response = await apiClient.uploadFile<{ id: string }>(
        API_ENDPOINTS.FILES.PROGRESS_DOCS,
        file,
        {
          stepId,
          ticketId,
          userId,
          auditLogId,
        }
      );

      return response.id;
    } catch (error) {
      console.error('Error uploading progress document:', error);
      throw error;
    }
  }

  static async getProgressDocuments(stepId: string, includeDeleted: boolean = false): Promise<ProgressDocumentMetadata[]> {
    try {
      console.log('[ProgressDocumentService] Fetching progress documents for step:', stepId);
      const response = await apiClient.get<ProgressDocumentMetadata[]>(
        `${API_ENDPOINTS.WORKFLOW_STEPS.PROGRESS_DOCUMENTS(stepId)}?includeDeleted=${includeDeleted}`
      );
      console.log('[ProgressDocumentService] Received response:', {
        isArray: Array.isArray(response),
        count: Array.isArray(response) ? response.length : 'N/A',
        data: response
      });

      return response.map((doc) => ({
        ...doc,
        uploadedAt: new Date(doc.uploadedAt),
        deletedAt: doc.deletedAt ? new Date(doc.deletedAt) : undefined,
      }));
    } catch (error) {
      console.error('Failed to fetch progress documents:', error);
      return [];
    }
  }

  static async getProgressDocumentUrl(documentId: string, _expiresIn: number = 3600): Promise<string> {
    return `${API_BASE_URL}${API_ENDPOINTS.FILES.DOWNLOAD(documentId)}`;
  }

  static async updateProgressDocumentComment(documentId: string, userId: string, newComment: string): Promise<void> {
    try {
      await apiClient.put(`/files/progress-docs/${documentId}/comment`, {
        userId,
        comment: newComment,
      });
    } catch (error) {
      console.error('Update progress comment failed:', error);
      throw error;
    }
  }

  static async uploadCompletionCertificate(
    file: File,
    ticketId: string,
    userId: string
  ): Promise<string> {
    try {
      const response = await apiClient.uploadFile<{ id: string }>(
        API_ENDPOINTS.FILES.COMPLETION_CERT,
        file,
        {
          ticketId,
          userId,
        }
      );

      return response.id;
    } catch (error) {
      console.error('Error uploading completion certificate:', error);
      throw error;
    }
  }

  static async hasTicketCompletionCertificate(ticketId: string): Promise<boolean> {
    try {
      const response = await apiClient.get<{ hasCompletionCert: boolean }>(
        `/tickets/${ticketId}/completion-certificate/exists`
      );
      return response.hasCompletionCert;
    } catch (error) {
      console.error('Error checking completion certificate:', error);
      return false;
    }
  }

  static async downloadFile(fileId: string, fileName: string): Promise<void> {
    try {
      await apiClient.downloadFile(API_ENDPOINTS.FILES.DOWNLOAD(fileId), fileName);
    } catch (error) {
      console.error('Error downloading file:', error);
      throw error;
    }
  }

  static async deleteFile(fileId: string, userId: string): Promise<void> {
    try {
      await apiClient.delete(API_ENDPOINTS.FILES.DELETE(fileId) + `?userId=${userId}`);
    } catch (error) {
      console.error('Error deleting file:', error);
      throw error;
    }
  }

  static async deleteProgressDocument(
    documentId: string,
    userId: string,
    reason: string
  ): Promise<void> {
    try {
      await apiClient.delete(
        `/files/progress-docs/${documentId}?userId=${userId}&reason=${encodeURIComponent(reason)}`
      );
    } catch (error) {
      console.error('Error deleting progress document:', error);
      throw error;
    }
  }
}

export interface ProgressHistoryEntry {
  id: string;
  type: 'progress_update' | 'document_upload' | 'completion_certificate' | 'status_change' | 'comment';
  timestamp: Date;
  userId: string;
  userName: string;
  userRole: string;
  progress?: number;
  oldProgress?: number;
  status?: string;
  oldStatus?: string;
  comment?: string;
  documents?: ProgressDocumentMetadata[];
  completionCertificates?: DocumentMetadata[];
  auditLogId?: string;
  metadata?: any;
}

export class ProgressHistoryService {
  static async getStepProgressHistory(stepId: string): Promise<ProgressHistoryEntry[]> {
    try {
      console.log('[ProgressHistoryService] Fetching progress history for step:', stepId);
      console.log('[ProgressHistoryService] Using endpoint:', API_ENDPOINTS.WORKFLOW_STEPS.PROGRESS_HISTORY(stepId));
      const response = await apiClient.get<ProgressHistoryEntry[]>(
        API_ENDPOINTS.WORKFLOW_STEPS.PROGRESS_HISTORY(stepId)
      );

      console.log('[ProgressHistoryService] Raw API response:', {
        type: typeof response,
        isArray: Array.isArray(response),
        count: Array.isArray(response) ? response.length : 'N/A',
        firstEntry: Array.isArray(response) && response.length > 0 ? response[0] : null,
        data: response
      });

      if (!Array.isArray(response)) {
        console.error('[ProgressHistoryService] Response is not an array:', response);
        return [];
      }

      const transformedData = response.map((entry) => ({
        ...entry,
        timestamp: new Date(entry.timestamp),
        documents: entry.documents?.map((doc) => ({
          ...doc,
          uploadedAt: new Date(doc.uploadedAt),
          deletedAt: doc.deletedAt ? new Date(doc.deletedAt) : undefined,
        })),
        completionCertificates: entry.completionCertificates?.map((cert) => ({
          ...cert,
          uploadedAt: new Date(cert.uploadedAt),
        })),
      }));

      console.log('[ProgressHistoryService] Transformed data:', {
        count: transformedData.length,
        types: transformedData.map(e => e.type),
        entries: transformedData
      });

      return transformedData;
    } catch (error) {
      console.error('[ProgressHistoryService] Error fetching progress history:', error);
      console.error('[ProgressHistoryService] Error details:', {
        message: error instanceof Error ? error.message : 'Unknown error',
        stack: error instanceof Error ? error.stack : undefined
      });
      return [];
    }
  }

  static async updateProgressComment(
    documentId: string,
    userId: string,
    newComment: string
  ): Promise<void> {
    await apiClient.put(`/progress-documents/${documentId}/comment`, {
      userId,
      comment: newComment,
    });
  }

  static async deleteProgressEntry(
    documentId: string,
    userId: string,
    reason: string
  ): Promise<void> {
    await apiClient.delete(
      `/progress-documents/${documentId}?userId=${userId}&reason=${encodeURIComponent(reason)}`
    );
  }
}