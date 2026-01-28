import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS } from '../lib/apiEndpoints';
import {
  transformTicketFromBackend,
  transformWorkflowStepFromBackend,
  statusToBackend
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
  static async getTicketsByModule(moduleId: string, userId?: string, userRole?: string): Promise<Ticket[]> {
    try {
      const params: any = { moduleId };
      if (userId) params.userId = userId;
      if (userRole) params.userRole = userRole;

      const tickets = await apiClient.get<any[]>(API_ENDPOINTS.TICKETS.LIST, params);

      return tickets.map(ticket => ({
        ...transformTicketFromBackend(ticket),
        workflow: (ticket.workflow || []).map(transformWorkflowStepFromBackend),
        attachments: (ticket.attachments || []).map((doc: any) => ({
          id: doc.id,
          name: doc.name,
          type: doc.type,
          size: doc.size,
          url: doc.url,
          uploadedBy: doc.uploaded_by,
          uploadedAt: new Date(doc.uploaded_at),
        })),
        auditTrail: (ticket.audit_trail || []).map((audit: any) => ({
          id: audit.id,
          ticketId: audit.ticket_id,
          stepId: audit.step_id,
          userId: audit.performed_by,
          action: audit.action,
          actionCategory: audit.action_category,
          oldValue: audit.old_data,
          newValue: audit.new_data,
          remarks: audit.description || '',
          metadata: audit.metadata || {},
          timestamp: new Date(audit.performed_at),
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
            uploadedAt: new Date(doc.uploaded_at),
            isDeleted: doc.is_deleted,
            deletedAt: doc.deleted_at ? new Date(doc.deleted_at) : undefined,
            deletedBy: doc.deleted_by,
            deleteReason: doc.delete_reason,
          })),
        })),
      }));
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

      const response = await apiClient.post<{ id: string }>(API_ENDPOINTS.TICKETS.CREATE, payload);
      return response.id;
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
        is_parallel: stepData.is_parallel !== undefined ? stepData.is_parallel : true,
        dependency_mode: stepData.dependency_mode || 'all',
        progress: stepData.progress !== undefined ? stepData.progress : 0,
        mandatory_documents: stepData.mandatory_documents || [],
        optional_documents: stepData.optional_documents || [],
        dueDate: stepData.dueDate,
        startDate: stepData.startDate,
        dependentOnStepIds: stepData.dependentOnStepIds || [],
        fileReferenceTemplateId: stepData.fileReferenceTemplateId,
        selectedFileReferences: stepData.selectedFileReferences || [],
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
      if (updates.is_parallel !== undefined) payload.is_parallel = updates.is_parallel;
      if (updates.progress !== undefined) payload.progress = updates.progress;
      if (updates.dependency_mode !== undefined) payload.dependency_mode = updates.dependency_mode;
      if (updates.mandatory_documents !== undefined) payload.mandatory_documents = updates.mandatory_documents;
      if (updates.optional_documents !== undefined) payload.optional_documents = updates.optional_documents;

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

  static async getStepComments(stepId: string) {
    try {
      const comments = await apiClient.get<any[]>(API_ENDPOINTS.WORKFLOW_COMMENTS.LIST(stepId));

      return comments.map(comment => ({
        id: comment.id,
        stepId: comment.stepId || comment.step_id,
        content: comment.content,
        createdBy: comment.createdBy || comment.created_by,
        createdByName: comment.createdByName || comment.created_by_name,
        createdByRole: comment.createdByRole || comment.created_by_role,
        createdAt: new Date(comment.createdAt || comment.created_at),
        updatedAt: comment.updatedAt || comment.updated_at ? new Date(comment.updatedAt || comment.updated_at) : undefined,
      }));
    } catch (error) {
      console.error('Error fetching step comments:', error);
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
      await apiClient.delete(API_ENDPOINTS.WORKFLOW_COMMENTS.DELETE(commentId) + `?userId=${userId}`);
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
