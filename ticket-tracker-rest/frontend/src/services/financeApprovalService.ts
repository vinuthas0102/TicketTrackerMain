import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS } from '../lib/apiEndpoints';

export interface FinanceApprovalRequest {
  ticketId: string;
  requestedBy: string;
  estimatedCost: number;
  remarks: string;
  documents?: File[];
}

export interface FinanceApprovalDecision {
  submissionId: string;
  decidedBy: string;
  remarks: string;
  documents?: File[];
}

export class FinanceApprovalService {
  static async sendToFinance(request: FinanceApprovalRequest): Promise<string> {
    try {
      if (request.documents && request.documents.length > 0) {
        const formData = new FormData();
        formData.append('ticketId', request.ticketId);
        formData.append('requestedBy', request.requestedBy);
        formData.append('estimatedCost', request.estimatedCost.toString());
        formData.append('remarks', request.remarks);

        request.documents.forEach((file, index) => {
          formData.append(`document_${index}`, file);
        });

        const response = await apiClient.post<{ id: string }>(
          API_ENDPOINTS.FINANCE.SUBMISSIONS,
          formData
        );

        return response.id;
      } else {
        const response = await apiClient.post<{ id: string }>(
          API_ENDPOINTS.FINANCE.SUBMISSIONS,
          {
            ticketId: request.ticketId,
            requestedBy: request.requestedBy,
            estimatedCost: request.estimatedCost,
            remarks: request.remarks,
          }
        );

        return response.id;
      }
    } catch (error) {
      console.error('Error sending to finance:', error);
      throw error;
    }
  }

  static async approveFinanceSubmission(decision: FinanceApprovalDecision): Promise<void> {
    try {
      if (decision.documents && decision.documents.length > 0) {
        for (const file of decision.documents) {
          await apiClient.uploadFile(
            API_ENDPOINTS.FINANCE.APPROVE(decision.submissionId),
            file,
            {
              decidedBy: decision.decidedBy,
              remarks: decision.remarks,
            }
          );
        }
      } else {
        await apiClient.post(API_ENDPOINTS.FINANCE.APPROVE(decision.submissionId), {
          decidedBy: decision.decidedBy,
          remarks: decision.remarks,
        });
      }
    } catch (error) {
      console.error('Error approving finance submission:', error);
      throw error;
    }
  }

  static async rejectFinanceSubmission(decision: FinanceApprovalDecision): Promise<void> {
    try {
      if (decision.documents && decision.documents.length > 0) {
        for (const file of decision.documents) {
          await apiClient.uploadFile(
            API_ENDPOINTS.FINANCE.REJECT(decision.submissionId),
            file,
            {
              decidedBy: decision.decidedBy,
              remarks: decision.remarks,
            }
          );
        }
      } else {
        await apiClient.post(API_ENDPOINTS.FINANCE.REJECT(decision.submissionId), {
          decidedBy: decision.decidedBy,
          remarks: decision.remarks,
        });
      }
    } catch (error) {
      console.error('Error rejecting finance submission:', error);
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
}
