package com.tickettracker.service;

import com.tickettracker.dao.FinanceApprovalDAO;
import com.tickettracker.dao.FinanceApprovalDAO.FinanceApproval;
import com.tickettracker.dao.TicketDAO;
import com.tickettracker.dao.UserDAO;
import com.tickettracker.exception.*;
import com.tickettracker.model.Ticket;
import com.tickettracker.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class FinanceApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(FinanceApprovalService.class);
    private final FinanceApprovalDAO financeApprovalDAO;
    private final TicketDAO ticketDAO;
    private final UserDAO userDAO;

    public FinanceApprovalService() {
        this.financeApprovalDAO = new FinanceApprovalDAO();
        this.ticketDAO = new TicketDAO();
        this.userDAO = new UserDAO();
    }

    public FinanceApproval getFinanceApprovalById(byte[] approvalId) throws TicketTrackerException {
        try {
            FinanceApproval approval = financeApprovalDAO.findById(approvalId);
            if (approval == null) {
                throw new ResourceNotFoundException("Finance Approval", bytesToHex(approvalId));
            }
            return approval;
        } catch (SQLException e) {
            logger.error("Error fetching finance approval by ID", e);
            throw new DatabaseException("Failed to fetch finance approval", e);
        }
    }

    public List<FinanceApproval> getFinanceApprovalsByTicketId(byte[] ticketId)
            throws TicketTrackerException {
        try {
            return financeApprovalDAO.findByTicketId(ticketId);
        } catch (SQLException e) {
            logger.error("Error fetching finance approvals by ticket ID", e);
            throw new DatabaseException("Failed to fetch finance approvals", e);
        }
    }

    public FinanceApproval getLatestFinanceApprovalByTicketId(byte[] ticketId)
            throws TicketTrackerException {
        try {
            FinanceApproval approval = financeApprovalDAO.findLatestByTicketId(ticketId);
            if (approval == null) {
                throw new ResourceNotFoundException("Finance Approval for ticket", bytesToHex(ticketId));
            }
            return approval;
        } catch (SQLException e) {
            logger.error("Error fetching latest finance approval by ticket ID", e);
            throw new DatabaseException("Failed to fetch finance approval", e);
        }
    }

    public List<FinanceApproval> getFinanceApprovalsByFinanceOfficer(byte[] financeOfficerId)
            throws TicketTrackerException {
        try {
            return financeApprovalDAO.findByFinanceOfficerId(financeOfficerId);
        } catch (SQLException e) {
            logger.error("Error fetching finance approvals by finance officer", e);
            throw new DatabaseException("Failed to fetch finance approvals", e);
        }
    }

    public List<FinanceApproval> getFinanceApprovalsByStatus(String status)
            throws TicketTrackerException {
        try {
            return financeApprovalDAO.findByStatus(status);
        } catch (SQLException e) {
            logger.error("Error fetching finance approvals by status", e);
            throw new DatabaseException("Failed to fetch finance approvals", e);
        }
    }

    public List<FinanceApproval> getPendingFinanceApprovals() throws TicketTrackerException {
        return getFinanceApprovalsByStatus("pending");
    }

    public FinanceApproval createFinanceApproval(FinanceApproval approval, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            validateFinanceApproval(approval);

            Ticket ticket = ticketDAO.findById(approval.getTicketId());
            if (ticket == null) {
                throw new ResourceNotFoundException("Ticket", bytesToHex(approval.getTicketId()));
            }

            if (approval.getFinanceOfficerId() != null) {
                User financeOfficer = userDAO.findById(approval.getFinanceOfficerId());
                if (financeOfficer == null) {
                    throw new ResourceNotFoundException("Finance Officer",
                        bytesToHex(approval.getFinanceOfficerId()));
                }
                if (!"finance".equalsIgnoreCase(financeOfficer.getRole())) {
                    throw new ValidationException("financeOfficerId",
                        "User is not a finance officer");
                }
            }

            approval.setSubmittedBy(currentUserId);
            approval.setStatus("pending");

            FinanceApproval createdApproval = financeApprovalDAO.create(approval);

            ticket.setStatus("sent_to_finance");
            ticket.setFinanceOfficerId(approval.getFinanceOfficerId());
            ticket.setFinanceSubmissionCount(ticket.getFinanceSubmissionCount() + 1);
            ticket.setLatestFinanceStatus("pending");
            ticketDAO.update(ticket);

            logger.info("Finance approval created for ticket {} by user: {}",
                bytesToHex(approval.getTicketId()), bytesToHex(currentUserId));

            return createdApproval;
        } catch (SQLException e) {
            logger.error("Error creating finance approval", e);
            throw new DatabaseException("Failed to create finance approval", e);
        }
    }

    public FinanceApproval approveFinanceApproval(byte[] approvalId, String approvalRemarks, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            FinanceApproval approval = getFinanceApprovalById(approvalId);

            User currentUser = userDAO.findById(currentUserId);
            if (currentUser == null || !"finance".equalsIgnoreCase(currentUser.getRoleInternal())) {
                throw new ForbiddenException("Only finance officers can approve finance requests");
            }

            if (!"pending".equalsIgnoreCase(approval.getStatus())) {
                throw new ValidationException("status",
                    "Only pending approvals can be approved");
            }

            FinanceApproval updatedApproval = financeApprovalDAO.approve(approvalId, approvalRemarks);

            Ticket ticket = ticketDAO.findById(approval.getTicketId());
            if (ticket != null) {
                ticket.setStatus("approved_by_finance");
                ticket.setLatestFinanceStatus("approved");
                ticketDAO.update(ticket);
            }

            logger.info("Finance approval {} approved by user: {}",
                bytesToHex(approvalId), bytesToHex(currentUserId));

            return updatedApproval;
        } catch (SQLException e) {
            logger.error("Error approving finance approval", e);
            throw new DatabaseException("Failed to approve finance approval", e);
        }
    }

    public FinanceApproval rejectFinanceApproval(byte[] approvalId, String rejectionReason,
                                                  byte[] currentUserId) throws TicketTrackerException {
        try {
            FinanceApproval approval = getFinanceApprovalById(approvalId);

            User currentUser = userDAO.findById(currentUserId);
            if (currentUser == null || !"finance".equalsIgnoreCase(currentUser.getRoleInternal())) {
                throw new ForbiddenException("Only finance officers can reject finance requests");
            }

            if (!"pending".equalsIgnoreCase(approval.getStatus())) {
                throw new ValidationException("status",
                    "Only pending approvals can be rejected");
            }

            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new ValidationException("rejectionReason",
                    "Rejection reason is required");
            }

            FinanceApproval updatedApproval = financeApprovalDAO.reject(approvalId, rejectionReason);

            Ticket ticket = ticketDAO.findById(approval.getTicketId());
            if (ticket != null) {
                ticket.setStatus("rejected_by_finance");
                ticket.setLatestFinanceStatus("rejected");
                ticketDAO.update(ticket);
            }

            logger.info("Finance approval {} rejected by user: {}",
                bytesToHex(approvalId), bytesToHex(currentUserId));

            return updatedApproval;
        } catch (SQLException e) {
            logger.error("Error rejecting finance approval", e);
            throw new DatabaseException("Failed to reject finance approval", e);
        }
    }

    public void deleteFinanceApproval(byte[] approvalId, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            FinanceApproval approval = getFinanceApprovalById(approvalId);

            boolean deleted = financeApprovalDAO.delete(approvalId);
            if (!deleted) {
                throw new DatabaseException("Failed to delete finance approval");
            }

            logger.info("Finance approval {} deleted by user: {}",
                bytesToHex(approvalId), bytesToHex(currentUserId));

        } catch (SQLException e) {
            logger.error("Error deleting finance approval", e);
            throw new DatabaseException("Failed to delete finance approval", e);
        }
    }

    private void validateFinanceApproval(FinanceApproval approval) throws ValidationException {
        ValidationException validation = new ValidationException("Finance approval validation failed");

        if (approval.getTicketId() == null) {
            validation.addError("Ticket ID is required");
        }

        if (approval.getTentativeCost() == null ||
            approval.getTentativeCost().compareTo(BigDecimal.ZERO) <= 0) {
            validation.addError("Tentative cost must be greater than 0");
        }

        if (approval.getCostDeductedFrom() == null ||
            approval.getCostDeductedFrom().trim().isEmpty()) {
            validation.addError("Cost deducted from is required");
        }

        if (validation.getValidationErrors().size() > 0) {
            throw validation;
        }
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
