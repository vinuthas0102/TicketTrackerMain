import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS } from '../lib/apiEndpoints';
import type { User, FinanceApproval, FinanceApprovalDecision, FinanceApprovalRequest } from '../types';

export class FinanceApprovalService {
  static async submitToFinance(request: FinanceApprovalRequest, submittedBy: string): Promise<string> {
    try {
      const response = await apiClient.post<{ id: string }>(
        API_ENDPOINTS.FINANCE.SUBMISSIONS,
        {
          ticketId: request.ticketId,
          requestedBy: submittedBy,
          tentativeCost: request.tentativeCost,
          costDeductedFrom: request.costDeductedFrom,
          financeOfficerId: request.financeOfficerId,
          remarks: request.remarks,
        }
      );

      return response.id;
    } catch (error) {
      console.error('Error sending to finance:', error);
      throw error;
    }
  }

  static async getFinanceApprovalHistory(ticketId: string): Promise<FinanceApproval[]> {
    try {
      const raw = await apiClient.get<any[]>(API_ENDPOINTS.FINANCE.SUBMISSIONS, { ticketId });
      const list = Array.isArray(raw) ? raw : [];
      return list.map(mapRawToFinanceApproval);
    } catch (error) {
      console.error('Error fetching finance approval history:', error);
      return [];
    }
  }

  static async approveFinanceRequest(decision: FinanceApprovalDecision, _decidedBy: string): Promise<void> {
    if (!decision.approvalId || decision.approvalId.trim() === '') {
      throw { code: 'VALIDATION_ERROR', message: 'Approval ID is missing. Please refresh and try again.', status: 400 };
    }
    try {
      if (decision.approvalDocumentFile) {
        await apiClient.uploadFile(
          API_ENDPOINTS.FINANCE.APPROVE(decision.approvalId),
          decision.approvalDocumentFile,
          { remarks: decision.remarks }
        );
      } else {
        await apiClient.post(API_ENDPOINTS.FINANCE.APPROVE(decision.approvalId), {
          remarks: decision.remarks,
        });
      }
    } catch (error) {
      console.error('Error approving finance request:', error);
      throw error;
    }
  }

  static async rejectFinanceRequest(decision: FinanceApprovalDecision, _decidedBy: string): Promise<void> {
    if (!decision.approvalId || decision.approvalId.trim() === '') {
      throw { code: 'VALIDATION_ERROR', message: 'Approval ID is missing. Please refresh and try again.', status: 400 };
    }
    try {
      if (decision.approvalDocumentFile) {
        await apiClient.uploadFile(
          API_ENDPOINTS.FINANCE.REJECT(decision.approvalId),
          decision.approvalDocumentFile,
          { rejectionReason: decision.rejectionReason }
        );
      } else {
        await apiClient.post(API_ENDPOINTS.FINANCE.REJECT(decision.approvalId), {
          rejectionReason: decision.rejectionReason,
        });
      }
    } catch (error) {
      console.error('Error rejecting finance request:', error);
      throw error;
    }
  }

  static async getFinanceSubmissions(ticketId?: string): Promise<any[]> {
    try {
      const params = ticketId ? { ticketId } : {};
      const submissions = await apiClient.get<any[]>(API_ENDPOINTS.FINANCE.SUBMISSIONS, params);
      return submissions;
    } catch (error) {
      console.error('Error fetching finance submissions:', error);
      return [];
    }
  }

  static async getFinanceSubmissionById(submissionId: string): Promise<any | null> {
    try {
      const submission = await apiClient.get<any>(API_ENDPOINTS.FINANCE.GET(submissionId));
      return submission;
    } catch (error) {
      console.error('Error fetching finance submission:', error);
      return null;
    }
  }

  static async getFinanceOfficers(): Promise<Array<{ id: string; name: string; email: string; department: string }>> {
    try {
      const users = await apiClient.get<User[]>(API_ENDPOINTS.USERS.LIST);

      const financeOfficers = users
        .filter(user => user.role === 'FINANCE' || user.role === 'finance')
        .map(user => ({
          id: user.id,
          name: user.name,
          email: user.email,
          department: user.department || ''
        }))
        .sort((a, b) => a.name.localeCompare(b.name));

      return financeOfficers;
    } catch (error) {
      console.error('Error fetching finance officers:', error);
      throw error;
    }
  }
}

function mapRawToFinanceApproval(raw: any): FinanceApproval {
  return {
    id: raw.id ?? '',
    ticketId: raw.ticketId ?? raw.ticket_id ?? '',
    tentativeCost: Number(raw.tentativeCost ?? raw.tentative_cost ?? 0),
    costDeductedFrom: raw.costDeductedFrom ?? raw.cost_deducted_from ?? '',
    remarks: raw.remarks ?? '',
    financeOfficerId: raw.financeOfficerId ?? raw.finance_officer_id ?? '',
    status: raw.status ?? 'pending',
    rejectionReason: raw.rejectionReason ?? raw.rejection_reason ?? undefined,
    approvalRemarks: raw.approvalRemarks ?? raw.approval_remarks ?? undefined,
    approvalDocumentFileName: raw.approvalDocumentFileName ?? raw.approval_document_file_name ?? undefined,
    approvalDocumentFilePath: raw.approvalDocumentFilePath ?? raw.approval_document_file_path ?? undefined,
    approvalDocumentFileSize: raw.approvalDocumentFileSize ?? raw.approval_document_file_size ?? undefined,
    approvalDocumentFileType: raw.approvalDocumentFileType ?? raw.approval_document_file_type ?? undefined,
    approvalDocumentUploadedAt: raw.approvalDocumentUploadedAt ?? raw.approval_document_uploaded_at
      ? new Date(raw.approvalDocumentUploadedAt ?? raw.approval_document_uploaded_at)
      : undefined,
    submittedBy: raw.submittedBy ?? raw.submitted_by ?? '',
    submittedAt: new Date(raw.submittedAt ?? raw.submitted_at),
    decidedAt: raw.decidedAt ?? raw.decided_at ? new Date(raw.decidedAt ?? raw.decided_at) : undefined,
    createdAt: new Date(raw.createdAt ?? raw.created_at),
    updatedAt: new Date(raw.updatedAt ?? raw.updated_at),
  };
}
