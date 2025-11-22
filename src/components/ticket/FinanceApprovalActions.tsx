import React, { useState } from 'react';
import { CheckCircle, XCircle, IndianRupee, Calendar, User, FileText, AlertCircle } from 'lucide-react';
import { FinanceApproval, FinanceApprovalDecision } from '../../types';
import { FinanceApprovalService } from '../../services/financeApprovalService';
import { useAuth } from '../../context/AuthContext';

interface FinanceApprovalActionsProps {
  approval: FinanceApproval;
  onActionComplete: () => void;
}

const FinanceApprovalActions: React.FC<FinanceApprovalActionsProps> = ({
  approval,
  onActionComplete
}) => {
  const { user } = useAuth();
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');
  const [loading, setLoading] = useState(false);

  const canTakeAction = user && user.id === approval.financeOfficerId && approval.status === 'pending';

  const handleApprove = async () => {
    if (!user || !canTakeAction) return;

    if (!confirm(`Are you sure you want to approve this request for Rs ${approval.tentativeCost.toLocaleString('en-IN')}?`)) {
      return;
    }

    setLoading(true);
    try {
      const decision: FinanceApprovalDecision = {
        approvalId: approval.id,
        ticketId: approval.ticketId,
        decision: 'approved'
      };

      await FinanceApprovalService.approveFinanceRequest(decision, user.id);
      alert('Finance approval granted successfully');
      onActionComplete();
    } catch (error) {
      console.error('Error approving request:', error);
      if (error instanceof Error) {
        alert(`Error: ${error.message}`);
      } else {
        alert('Failed to approve request. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async () => {
    if (!user || !canTakeAction) return;

    if (rejectionReason.trim().length < 20) {
      alert('Rejection reason must be at least 20 characters');
      return;
    }

    setLoading(true);
    try {
      const decision: FinanceApprovalDecision = {
        approvalId: approval.id,
        ticketId: approval.ticketId,
        decision: 'rejected',
        rejectionReason: rejectionReason.trim()
      };

      await FinanceApprovalService.rejectFinanceRequest(decision, user.id);
      alert('Finance approval rejected');
      setRejectionReason('');
      setShowRejectModal(false);
      onActionComplete();
    } catch (error) {
      console.error('Error rejecting request:', error);
      if (error instanceof Error) {
        alert(`Error: ${error.message}`);
      } else {
        alert('Failed to reject request. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (date: Date) => {
    return new Intl.DateTimeFormat('en-IN', {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(date);
  };

  const getStatusBadge = () => {
    switch (approval.status) {
      case 'pending':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
            <Calendar className="w-3 h-3 mr-1" />
            Pending Review
          </span>
        );
      case 'approved':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
            <CheckCircle className="w-3 h-3 mr-1" />
            Approved
          </span>
        );
      case 'rejected':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
            <XCircle className="w-3 h-3 mr-1" />
            Rejected
          </span>
        );
    }
  };

  return (
    <>
      <div className="bg-white rounded-lg border border-gray-200 p-4 space-y-3">
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <div className="flex items-center space-x-2 mb-2">
              <IndianRupee className="w-4 h-4 text-gray-600" />
              <span className="text-lg font-bold text-gray-900">
                Rs {approval.tentativeCost.toLocaleString('en-IN')}
              </span>
              {getStatusBadge()}
            </div>

            <div className="space-y-2 text-sm">
              <div className="flex items-start space-x-2">
                <FileText className="w-4 h-4 text-gray-400 mt-0.5 flex-shrink-0" />
                <div>
                  <span className="font-medium text-gray-700">Cost Bearer:</span>{' '}
                  <span className="text-gray-600">{approval.costDeductedFrom}</span>
                </div>
              </div>

              <div className="flex items-start space-x-2">
                <Calendar className="w-4 h-4 text-gray-400 mt-0.5 flex-shrink-0" />
                <div>
                  <span className="font-medium text-gray-700">Submitted:</span>{' '}
                  <span className="text-gray-600">{formatDate(approval.submittedAt)}</span>
                </div>
              </div>

              {approval.decidedAt && (
                <div className="flex items-start space-x-2">
                  <Calendar className="w-4 h-4 text-gray-400 mt-0.5 flex-shrink-0" />
                  <div>
                    <span className="font-medium text-gray-700">Decided:</span>{' '}
                    <span className="text-gray-600">{formatDate(approval.decidedAt)}</span>
                  </div>
                </div>
              )}

              <div className="flex items-start space-x-2">
                <FileText className="w-4 h-4 text-gray-400 mt-0.5 flex-shrink-0" />
                <div className="flex-1">
                  <span className="font-medium text-gray-700">Remarks:</span>
                  <p className="text-gray-600 mt-1 text-xs leading-relaxed">{approval.remarks}</p>
                </div>
              </div>

              {approval.rejectionReason && (
                <div className="flex items-start space-x-2 p-2 bg-red-50 rounded border border-red-200">
                  <AlertCircle className="w-4 h-4 text-red-600 mt-0.5 flex-shrink-0" />
                  <div className="flex-1">
                    <span className="font-medium text-red-800">Rejection Reason:</span>
                    <p className="text-red-700 mt-1 text-xs leading-relaxed">{approval.rejectionReason}</p>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {canTakeAction && (
          <div className="flex items-center space-x-2 pt-3 border-t border-gray-200">
            <button
              onClick={handleApprove}
              disabled={loading}
              className="flex-1 inline-flex items-center justify-center px-4 py-2 border border-transparent text-sm font-medium rounded-lg text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              <CheckCircle className="w-4 h-4 mr-2" />
              {loading ? 'Processing...' : 'Approve'}
            </button>
            <button
              onClick={() => setShowRejectModal(true)}
              disabled={loading}
              className="flex-1 inline-flex items-center justify-center px-4 py-2 border border-transparent text-sm font-medium rounded-lg text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              <XCircle className="w-4 h-4 mr-2" />
              Reject
            </button>
          </div>
        )}

        {!canTakeAction && approval.status === 'pending' && (
          <div className="pt-3 border-t border-gray-200">
            <p className="text-xs text-amber-600 flex items-center">
              <AlertCircle className="w-3 h-3 mr-1" />
              Only the assigned finance officer can approve or reject this request
            </p>
          </div>
        )}
      </div>

      {showRejectModal && (
        <div className="fixed inset-0 z-[110] overflow-y-auto">
          <div className="flex min-h-screen items-center justify-center p-4">
            <div className="fixed inset-0 bg-black bg-opacity-75 transition-opacity" onClick={() => !loading && setShowRejectModal(false)}></div>

            <div className="relative bg-white rounded-lg shadow-2xl max-w-md w-full border-2 border-red-200">
              <div className="flex items-center justify-between p-4 border-b border-gray-200 bg-red-50">
                <h3 className="text-lg font-bold text-red-800">Reject Finance Request</h3>
                <button
                  onClick={() => setShowRejectModal(false)}
                  className="text-gray-400 hover:text-gray-600"
                  disabled={loading}
                >
                  <XCircle className="w-5 h-5" />
                </button>
              </div>

              <div className="p-4">
                <div className="mb-4">
                  <p className="text-sm text-gray-700 mb-2">
                    Please provide a detailed reason for rejecting this finance approval request.
                  </p>
                  <div className="p-3 bg-gray-50 rounded border border-gray-200 text-sm mb-3">
                    <p><strong>Cost:</strong> Rs {approval.tentativeCost.toLocaleString('en-IN')}</p>
                    <p><strong>Bearer:</strong> {approval.costDeductedFrom}</p>
                  </div>
                </div>

                <div className="mb-4">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Rejection Reason * (minimum 20 characters)
                  </label>
                  <textarea
                    value={rejectionReason}
                    onChange={(e) => setRejectionReason(e.target.value)}
                    rows={4}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-red-500"
                    placeholder="Explain why this request is being rejected..."
                    disabled={loading}
                  />
                  <p className={`text-xs mt-1 ${rejectionReason.length >= 20 ? 'text-green-600' : 'text-gray-500'}`}>
                    {rejectionReason.length}/20 characters minimum
                  </p>
                </div>

                <div className="flex justify-end space-x-2">
                  <button
                    onClick={() => setShowRejectModal(false)}
                    className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200"
                    disabled={loading}
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleReject}
                    disabled={loading || rejectionReason.trim().length < 20}
                    className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {loading ? 'Rejecting...' : 'Confirm Rejection'}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default FinanceApprovalActions;
