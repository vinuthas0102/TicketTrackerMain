import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS } from '../lib/apiEndpoints';
import {
  transformTicketFromBackend,
  transformWorkflowStepFromBackend,
  statusToBackend,
  safeParseDate
} from '../lib/transformers/dataTransformer';
import {
  Ticket,
  StatusTransitionRequest,
  WorkflowStep,
  BulkStepInput,
  BulkOperationResult,
  BulkTicketInput,
  BulkTicketOperationResult,
  AuditActionCategory
} from '../types';

export class TicketService {
  static async getTicketWorkflow(ticketId: string): Promise<WorkflowStep[]> {
    try {
      const steps = await apiClient.get<any[]>(API_ENDPOINTS.WORKFLOW_STEPS.LIST(ticketId));
      return steps.map(transformWorkflowStepFromBackend);
    } catch (error) {
      console.error('Error fetching ticket workflow:', error);
      return [];
    }
  }

  static async getTicketDocuments(ticketId: string): Promise<any[]> {
    try {
      const docs = await apiClient.get<any[]>(API_ENDPOINTS.FILES.LIST_BY_TICKET(ticketId));
      return docs.map((doc: any) => ({
        id: doc.id,
        name: doc.name,
        type: doc.type,
        size: doc.size,
        url: doc.url,
        uploadedBy: doc.uploaded_by,
        uploadedAt: safeParseDate(doc.uploaded_at) || new Date(),
      }));
    } catch (error) {
      console.error('Error fetching ticket documents:', error);
      return [];
    }
  }

  static async getTicketAuditTrail(ticketId: string): Promise<any[]> {
    try {
      const audits = await apiClient.get<any[]>(API_ENDPOINTS.AUDIT.LIST_BY_TICKET(ticketId));
      return audits.map((auditResponse: any) => {
        const audit = auditResponse.auditLog || auditResponse;
        const progressDocs = auditResponse.progressDocs || [];

        return {
          id: audit.id,
          ticketId: audit.ticket_id || audit.ticketId,
          stepId: audit.step_id || audit.stepId,
          userId: audit.performed_by || audit.performedBy,
          action: audit.action,
          actionCategory: audit.action_category || audit.actionCategory,
          oldValue: audit.old_data || audit.oldData,
          newValue: audit.new_data || audit.newData,
          remarks: audit.description || '',
          metadata: audit.metadata || {},
          timestamp: safeParseDate(audit.performed_at || audit.performedAt) || new Date(),
          progressDocs: progressDocs.map((doc: any) => ({
            id: doc.id,
            stepId: doc.step_id || doc.stepId,
            ticketId: doc.ticket_id || doc.ticketId,
            auditLogId: doc.audit_log_id || doc.auditLogId,
            fileName: doc.file_name || doc.fileName,
            filePath: doc.file_path || doc.filePath,
            fileSize: doc.file_size || doc.fileSize,
            fileType: doc.file_type || doc.fileType,
            uploadedBy: doc.uploaded_by || doc.uploadedBy,
            uploadedAt: safeParseDate(doc.uploaded_at || doc.uploadedAt) || new Date(),
            isDeleted: doc.is_deleted || doc.isDeleted || false,
            deletedAt: safeParseDate(doc.deleted_at || doc.deletedAt),
            deletedBy: doc.deleted_by || doc.deletedBy,
            deleteReason: doc.delete_reason || doc.deleteReason,
          })),
        };
      });
    } catch (error) {
      console.error('Error fetching ticket audit trail:', error);
      return [];
    }
  }

  static async getTicketsByModule(
    moduleId: string,
    userId?: string,
    userRole?: string,
    loadRelatedData: boolean = false
  ): Promise<Ticket[]> {
    try {
      const params: any = { moduleId };
      if (userId) params.userId = userId;
      if (userRole) params.userRole = userRole;

      const tickets = await apiClient.get<any[]>(API_ENDPOINTS.TICKETS.LIST, params);

      const basicTickets = tickets.map(ticket => ({
        ...transformTicketFromBackend(ticket),
        workflow: (ticket.workflow || []).map(transformWorkflowStepFromBackend),
        attachments: (ticket.attachments || []).map((doc: any) => ({
          id: doc.id,
          name: doc.name,
          type: doc.type,
          size: doc.size,
          url: doc.url,
          uploadedBy: doc.uploaded_by,
          uploadedAt: safeParseDate(doc.uploaded_at) || new Date(),
        })),
        auditTrail: (ticket.auditLog || []).map((audit: any) => ({
          id: audit.id,
          ticketId: audit.ticketId,
          stepId: audit.stepId,
          userId: audit.performedBy,
          action: audit.action,
          actionCategory: audit.actionCategory,
          oldValue: audit.oldData,
          newValue: audit.newData,
          remarks: audit.description || '',
          metadata: audit.metadata || {},
          timestamp: safeParseDate(audit.performed_at) || new Date(),
          progressDocs: (audit.progress_docs || []).map((doc: any) => ({
            id: doc.id,
            stepId: doc.step_id,
            ticketId: doc.ticket_id,
            auditLogId: doc.audit_log_id,
            fileName: doc.file_name,
            filePath: doc.file_path,
            fileSize: doc.file_size,
            fileType: doc.file_type,
            uploadedBy: doc.uploaded_by,
            uploadedAt: safeParseDate(doc.uploaded_at) || new Date(),
            isDeleted: doc.is_deleted,
            deletedAt: safeParseDate(doc.deleted_at),
            deletedBy: doc.deleted_by,
            deleteReason: doc.delete_reason,
          })),
        })),
      }));

      if (!loadRelatedData) {
        return basicTickets;
      }

      const enrichedTickets = await Promise.all(
        basicTickets.map(async (ticket) => {
          const [workflow, attachments, auditTrail] = await Promise.all([
            this.getTicketWorkflow(ticket.id),
            this.getTicketDocuments(ticket.id),
            this.getTicketAuditTrail(ticket.id),
          ]);

          return {
            ...ticket,
            workflow,
            attachments,
            auditTrail,
          };
        })
      );

      return enrichedTickets;
    } catch (error) {
      console.error('Error fetching tickets:', error);
      throw error;
    }
  }

  static async createTicket(ticketData: any, copiedFromTicketId?: string): Promise<string> {
    try {
      const payload = {
        moduleId: ticketData.moduleId,
        title: ticketData.title,
        description: ticketData.description,
        status: statusToBackend(ticketData.status),
        priority: ticketData.priority,
        createdBy: ticketData.createdBy,
        assignedTo: ticketData.assignedTo,
        dueDate: ticketData.dueDate,
        propertyId: ticketData.propertyId || 'PROP001',
        propertyLocation: ticketData.propertyLocation || 'Location01',
        data: ticketData.data || {},
        copiedFromTicketId,
      };

      const response = await apiClient.post<any>(API_ENDPOINTS.TICKETS.CREATE, payload);

      console.log('[TicketService] createTicket response type:', typeof response);
      console.log('[TicketService] createTicket response:', response);

      let ticketId: string | undefined;

      if (!response) {
        console.error('[TicketService] Received null/undefined response');
        throw new Error('Server returned empty response');
      }

      if (typeof response === 'string') {
        if (response.length > 0 && response !== '{}') {
          console.warn('[TicketService] Received string response, treating as ticket ID:', response);
          ticketId = response;
        } else {
          console.error('[TicketService] Received invalid string response:', response);
          throw new Error('Server returned invalid response format');
        }
      } else if (typeof response === 'object') {
        if (response.id) {
          ticketId = response.id;
        } else if (response.data?.id) {
          ticketId = response.data.id;
        } else if (Object.keys(response).length === 0) {
          console.error('[TicketService] Received empty object {}');
          throw new Error('Server returned empty object - ticket creation may have failed');
        } else {
          console.error('[TicketService] Response has no id field:', Object.keys(response));
          throw new Error('Server response missing ticket ID');
        }
      }

      if (!ticketId) {
        console.error('[TicketService] Failed to extract ticket ID from response:', response);
        throw new Error('Could not determine ticket ID from server response');
      }

      console.log('[TicketService] Successfully created ticket with ID:', ticketId);
      return ticketId;
    } catch (error) {
      console.error('Error creating ticket:', error);
      throw error;
    }
  }

  static async createTicketsBulk(
    ticketsData: BulkTicketInput[],
    moduleId: string,
    createdBy: string
  ): Promise<BulkTicketOperationResult> {
    try {
      const payload = {
        tickets: ticketsData.map(ticket => ({
          title: ticket.title,
          description: ticket.description || '',
          status: statusToBackend(ticket.status),
          priority: ticket.priority,
          assignedTo: ticket.assignedTo || null,
          dueDate: ticket.dueDate || null,
          propertyId: ticket.propertyId,
          propertyLocation: ticket.propertyLocation,
          department: ticket.department,
        })),
        moduleId,
        createdBy,
      };

      const result = await apiClient.post<BulkTicketOperationResult>(
        API_ENDPOINTS.TICKETS.BULK_CREATE,
        payload
      );

      return result;
    } catch (error) {
      console.error('Error in bulk ticket creation:', error);
      throw error;
    }
  }

  static async updateTicket(id: string, updates: Partial<Ticket>, userId: string): Promise<void> {
    try {
      const payload: any = {};

      if (updates.title !== undefined) payload.title = updates.title;
      if (updates.description !== undefined) payload.description = updates.description;
      if (updates.status !== undefined) payload.status = statusToBackend(updates.status);
      if (updates.priority !== undefined) payload.priority = updates.priority;
      if (updates.assignedTo !== undefined) payload.assignedTo = updates.assignedTo;
      if (updates.dueDate !== undefined) payload.dueDate = updates.dueDate;

      payload.userId = userId;

      await apiClient.put(API_ENDPOINTS.TICKETS.UPDATE(id), payload);
    } catch (error) {
      console.error('Error updating ticket:', error);
      throw error;
    }
  }

  static async changeTicketStatus(request: StatusTransitionRequest, userId: string): Promise<void> {
    try {
      const payload = {
        newStatus: statusToBackend(request.newStatus),
        currentStatus: statusToBackend(request.currentStatus),
        remarks: request.remarks,
        userId,
        completionCertificateFile: request.completionCertificateFile,
      };

      if (request.completionCertificateFile) {
        await apiClient.uploadFile(
          API_ENDPOINTS.TICKETS.STATUS(request.ticketId),
          request.completionCertificateFile,
          {
            newStatus: payload.newStatus,
            currentStatus: payload.currentStatus,
            remarks: payload.remarks,
            userId,
          }
        );
      } else {
        await apiClient.put(API_ENDPOINTS.TICKETS.STATUS(request.ticketId), payload);
      }
    } catch (error) {
      console.error('Error changing ticket status:', error);
      throw error;
    }
  }

  static async deleteTicket(id: string): Promise<void> {
    try {
      await apiClient.delete(API_ENDPOINTS.TICKETS.DELETE(id));
    } catch (error) {
      console.error('Error deleting ticket:', error);
      throw error;
    }
  }

  static async addWorkflowStep(ticketId: string, stepData: any, userId: string): Promise<string> {
    try {
      const payload = {
        ticketId,
        title: stepData.title,
        description: stepData.description || '',
        status: statusToBackend(stepData.status || 'not_started'),
        assignedTo: stepData.assignedTo,
        parentStepId: stepData.parentStepId || null,
        dependencies: stepData.dependencies || [],
        isParallel: stepData.is_parallel !== undefined ? stepData.is_parallel : true,
        dependencyMode: stepData.dependency_mode || 'all',
        progress: stepData.progress !== undefined ? stepData.progress : 0,
        mandatoryDocuments: stepData.mandatory_documents || [],
        optionalDocuments: stepData.optional_documents || [],
        dueDate: stepData.dueDate,
        startDate: stepData.startDate,
        dependentOnStepIds: stepData.dependentOnStepIds || [],
        fileReferenceTemplateId: stepData.fileReferenceTemplateId,
        selectedFileReferences: stepData.selectedFileReferences || [],
        stepNumber : stepData.stepNumber,
        userId,
      };

      const response = await apiClient.post<{ id: string }>(
        API_ENDPOINTS.WORKFLOW_STEPS.CREATE,
        payload
      );

      return response.id;
    } catch (error) {
      console.error('Error adding step:', error);
      throw error;
    }
  }

  static async updateWorkflowStep(
    ticketId: string,
    stepId: string,
    updates: Partial<WorkflowStep>,
    userId: string,
    remarks?: string
  ): Promise<void> {
    try {
      const payload: any = { userId, remarks };

      if (updates.title !== undefined) payload.title = updates.title;
      if (updates.description !== undefined) payload.description = updates.description;
      if (updates.status !== undefined) payload.status = statusToBackend(updates.status);
      if (updates.assignedTo !== undefined) payload.assignedTo = updates.assignedTo;
      if (updates.dueDate !== undefined) payload.dueDate = updates.dueDate;
      if (updates.startDate !== undefined) payload.startDate = updates.startDate;
      if (updates.is_parallel !== undefined) payload.isParallel = updates.is_parallel;
      if (updates.progress !== undefined) payload.progress = updates.progress;
      if (updates.dependency_mode !== undefined) payload.dependencyMode = updates.dependency_mode;
      if (updates.mandatory_documents !== undefined) payload.mandatoryDocuments = updates.mandatory_documents;
      if (updates.optional_documents !== undefined) payload.optionalDocuments = updates.optional_documents;
      if (stepId!== undefined) payload.id = stepId;
      if (stepId!== undefined) payload.stepId = stepId;
      if (updates.stepNumber !== undefined) payload.stepNumber = updates.stepNumber;
      if (ticketId!== undefined) payload.ticketId = ticketId;
      await apiClient.put(API_ENDPOINTS.WORKFLOW_STEPS.UPDATE(stepId), payload);
    } catch (error) {
      console.error('Error updating step:', error);
      throw error;
    }
  }

  static async deleteWorkflowStep(stepId: string, ticketId: string, userId: string): Promise<void> {
    try {
      await apiClient.delete(API_ENDPOINTS.WORKFLOW_STEPS.DELETE(stepId) + `?userId=${userId}`);
    } catch (error) {
      console.error('Error deleting step:', error);
      throw error;
    }
  }

  static async getWorkflowSteps(ticketId: string): Promise<WorkflowStep[]> {
    try {
      const steps = await apiClient.get<any[]>(API_ENDPOINTS.WORKFLOW_STEPS.LIST(ticketId));
      return steps.map(transformWorkflowStepFromBackend);
    } catch (error) {
      console.error('Error fetching workflow steps:', error);
      throw error;
    }
  }

  static async getWorkflowStep(stepId: string): Promise<WorkflowStep> {
    try {
      const step = await apiClient.get<any>(API_ENDPOINTS.WORKFLOW_STEPS.GET(stepId));
      return transformWorkflowStepFromBackend(step);
    } catch (error) {
      console.error('Error fetching workflow step:', error);
      throw error;
    }
  }

  static async addStep(ticketId: string, stepData: any, userId: string): Promise<string> {
    return this.addWorkflowStep(ticketId, stepData, userId);
  }

  static async updateStep(
    ticketId: string,
    stepId: string,
    updates: Partial<WorkflowStep>,
    userId: string,
    remarks?: string
  ): Promise<void> {
    return this.updateWorkflowStep(ticketId, stepId, updates, userId, remarks);
  }

  static async deleteStep(stepId: string, ticketId: string, userId: string): Promise<void> {
    return this.deleteWorkflowStep(stepId, ticketId, userId);
  }

  static async addStepsBulk(
    ticketId: string,
    steps: BulkStepInput[],
    userId: string,
    parentStepId?: string
  ): Promise<BulkOperationResult> {
    try {
      const payload = {
        ticketId,
        steps: steps.map(step => ({
          title: step.title,
          description: step.description || '',
          status: statusToBackend(step.status || 'not_started'),
          assignedTo: step.assignedTo || null,
          startDate: step.startDate || null,
          dueDate: step.dueDate || null,
          is_parallel: step.is_parallel !== undefined ? step.is_parallel : true,
          dependency_mode: step.dependency_mode || 'all',
          progress: step.progress !== undefined ? step.progress : 0,
          mandatory_documents: step.mandatory_documents || [],
          optional_documents: step.optional_documents || [],
          dependentOnStepIds: step.dependentOnStepIds || [],
          fileReferenceTemplateId: step.fileReferenceTemplateId,
          selectedFileReferences: step.selectedFileReferences || [],
        })),
        userId,
        parentStepId,
      };

      const result = await apiClient.post<BulkOperationResult>(
        API_ENDPOINTS.WORKFLOW_STEPS.BULK_CREATE,
        payload
      );

      return result;
    } catch (error) {
      console.error('Error in bulk step creation:', error);
      throw error;
    }
  }

  static async getStepComments(stepId: string): Promise<any[]> {
    try {
      const comments = await apiClient.get<any[]>(API_ENDPOINTS.WORKFLOW_COMMENTS.LIST(stepId));
      return comments.map((comment: any) => ({
        id: comment.id,
        stepId: comment.stepId || comment.step_id,
        content: comment.content,
        createdBy: comment.createdBy || comment.created_by,
        createdAt: safeParseDate(comment.createdAt || comment.created_at) || new Date(),
        updatedAt: safeParseDate(comment.updatedAt || comment.updated_at),
        createdByName: comment.createdByName || comment.created_by_name,
        createdByRole: comment.createdByRole || comment.created_by_role,
      }));
    } catch (error) {
      console.error('Error fetching step comments:', error);
      return [];
    }
  }

  static async addStepComment(stepId: string, content: string, userId: string): Promise<void> {
    try {
      await apiClient.post(API_ENDPOINTS.WORKFLOW_COMMENTS.CREATE, {
        stepId,
        content: content.trim(),
        userId,
      });
    } catch (error) {
      console.error('Error adding step comment:', error);
      throw error;
    }
  }

  static async updateStepComment(commentId: string, content: string, userId: string): Promise<void> {
    try {
      await apiClient.put(API_ENDPOINTS.WORKFLOW_COMMENTS.UPDATE(commentId), {
        content: content.trim(),
        userId,
      });
    } catch (error) {
      console.error('Error updating step comment:', error);
      throw error;
    }
  }

  static async deleteStepComment(commentId: string, userId: string): Promise<void> {
    try {
      await apiClient.delete(API_ENDPOINTS.WORKFLOW_COMMENTS.DELETE(commentId));
    } catch (error) {
      console.error('Error deleting step comment:', error);
      throw error;
    }
  }

  static async createAuditLog(params: {
    ticketId: string;
    stepId?: string;
    action: string;
    actionCategory: AuditActionCategory;
    description: string;
    performedBy: string;
    oldData?: string;
    newData?: string;
    metadata?: Record<string, any>;
  }): Promise<string | null> {
    try {
      const response = await apiClient.post<{ id: string }>(API_ENDPOINTS.AUDIT.CREATE, {
        ticketId: params.ticketId,
        stepId: params.stepId || null,
        action: params.action,
        actionCategory: params.actionCategory,
        description: params.description,
        performedBy: params.performedBy,
        oldData: params.oldData || null,
        newData: params.newData || null,
        metadata: params.metadata || {},
      });

      return response.id || null;
    } catch (error) {
      console.error('Error creating audit log:', error);
      return null;
    }
  }

  static async updateStepProgressWithFiles(
    ticketId: string,
    stepId: string,
    progress: number,
    progressComment: string,
    userId: string,
    files?: File[]
  ): Promise<void> {
    try {
      if (files && files.length > 0) {
        for (const file of files) {
          await apiClient.uploadFile(
            API_ENDPOINTS.WORKFLOW_STEPS.PROGRESS(stepId),
            file,
            {
              progress,
              comment: progressComment,
              userId,
              ticketId,
            }
          );
        }
      } else {
        await apiClient.put(API_ENDPOINTS.WORKFLOW_STEPS.PROGRESS(stepId), {
          progress,
          comment: progressComment,
          userId,
          ticketId,
        });
      }
    } catch (error) {
      console.error('Error updating step progress with files:', error);
      throw error;
    }
  }

  static async canUserAccessTicket(userId: string, ticketId: string): Promise<boolean> {
    try {
      const response = await apiClient.get<{ canAccess: boolean }>(
        `/tickets/${ticketId}/access?userId=${userId}`
      );
      return response.canAccess;
    } catch (error) {
      console.error('Error checking ticket access:', error);
      return false;
    }
  }

  static async getAccessibleTicketIds(userId: string): Promise<string[]> {
    try {
      const response = await apiClient.get<string[]>(
        API_ENDPOINTS.TICKETS.ACCESSIBLE,
        { userId }
      );
      return response || [];
    } catch (error) {
      console.error('Error getting accessible ticket IDs:', error);
      return [];
    }
  }
}
