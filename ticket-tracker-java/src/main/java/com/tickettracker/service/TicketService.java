package com.tickettracker.service;

import com.tickettracker.dao.*;
import com.tickettracker.exception.*;
import com.tickettracker.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    private final TicketDAO ticketDAO;
    private final WorkflowStepDAO workflowStepDAO;
    private final AuditLogDAO auditLogDAO;
    private final UserDAO userDAO;

    public TicketService() {
        this.ticketDAO = new TicketDAO();
        this.workflowStepDAO = new WorkflowStepDAO();
        this.auditLogDAO = new AuditLogDAO();
        this.userDAO = new UserDAO();
    }

    public Ticket createTicket(Ticket ticket, byte[] currentUserId) throws TicketTrackerException {
        try {
            validateTicket(ticket);

            ticket.setCreatedBy(currentUserId);

            if (ticket.getStatus() == null || ticket.getStatus().trim().isEmpty()) {
                ticket.setStatus("draft");
            } else {
                ticket.setStatus(ticket.getStatus().toLowerCase());
            }

            if (ticket.getTicketNumber() == null || ticket.getTicketNumber().isEmpty()) {
                ticket.setTicketNumber(generateTicketNumber());
            }

            Ticket createdTicket = ticketDAO.create(ticket);

            createAuditLog(createdTicket.getId(), null, currentUserId, "Ticket created",
                    null, "Ticket created successfully", "ticket_action");

            logger.info("Ticket created: {} by user: {}",
                    createdTicket.getTicketNumber(), bytesToHex(currentUserId));

            return createdTicket;
        } catch (SQLException e) {
            logger.error("Error creating ticket", e);
            throw new DatabaseException("Failed to create ticket", e);
        }
    }

    public List<Ticket> createTicketsBulk(List<Ticket> tickets, byte[] currentUserId)
            throws TicketTrackerException {
        List<Ticket> createdTickets = new ArrayList<>();

        try {
            for (Ticket ticket : tickets) {
                validateTicket(ticket);

                ticket.setCreatedBy(currentUserId);

                if (ticket.getStatus() == null || ticket.getStatus().trim().isEmpty()) {
                    ticket.setStatus("draft");
                } else {
                    ticket.setStatus(ticket.getStatus().toLowerCase());
                }

                if (ticket.getTicketNumber() == null || ticket.getTicketNumber().isEmpty()) {
                    ticket.setTicketNumber(generateTicketNumber());
                }

                Ticket createdTicket = ticketDAO.create(ticket);
                createdTickets.add(createdTicket);

                createAuditLog(createdTicket.getId(), null, currentUserId, "Ticket created",
                        null, "Ticket created via bulk operation", "ticket_action");
            }

            logger.info("Bulk ticket creation: {} tickets created by user: {}",
                    createdTickets.size(), bytesToHex(currentUserId));

            return createdTickets;
        } catch (SQLException e) {
            logger.error("Error creating tickets in bulk", e);
            throw new DatabaseException("Failed to create tickets in bulk", e);
        }
    }

    public Ticket getTicket(byte[] ticketId) throws TicketTrackerException {
        try {
            Ticket ticket = ticketDAO.findById(ticketId);
            if (ticket == null) {
                throw new ResourceNotFoundException("Ticket", bytesToHex(ticketId));
            }
            return ticket;
        } catch (SQLException e) {
            logger.error("Error fetching ticket", e);
            throw new DatabaseException("Failed to fetch ticket", e);
        }
    }

    public Ticket getTicketByNumber(String ticketNumber) throws TicketTrackerException {
        try {
            Ticket ticket = ticketDAO.findByTicketNumber(ticketNumber);
            if (ticket == null) {
                throw new ResourceNotFoundException("Ticket", ticketNumber);
            }
            return ticket;
        } catch (SQLException e) {
            logger.error("Error fetching ticket by number", e);
            throw new DatabaseException("Failed to fetch ticket", e);
        }
    }

    public List<Ticket> getAllTickets() throws TicketTrackerException {
        try {
            return ticketDAO.findAll();
        } catch (SQLException e) {
            logger.error("Error fetching all tickets", e);
            throw new DatabaseException("Failed to fetch tickets", e);
        }
    }

    public List<Ticket> getAccessibleTickets(byte[] userId) throws TicketTrackerException {
        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                throw new ResourceNotFoundException("User", bytesToHex(userId));
            }

            return ticketDAO.findAccessibleTickets(userId, user.getRole());
        } catch (SQLException e) {
            logger.error("Error fetching accessible tickets", e);
            throw new DatabaseException("Failed to fetch accessible tickets", e);
        }
    }

    public List<Ticket> getTicketsByModule(byte[] moduleId) throws TicketTrackerException {
        try {
            return ticketDAO.findByModuleId(moduleId);
        } catch (SQLException e) {
            logger.error("Error fetching tickets by module", e);
            throw new DatabaseException("Failed to fetch tickets", e);
        }
    }

    public List<Ticket> getTicketsByStatus(String status) throws TicketTrackerException {
        try {
            return ticketDAO.findByStatus(status);
        } catch (SQLException e) {
            logger.error("Error fetching tickets by status", e);
            throw new DatabaseException("Failed to fetch tickets", e);
        }
    }

    public List<Ticket> getTicketsByAssignedUser(byte[] userId) throws TicketTrackerException {
        try {
            return ticketDAO.findByAssignedTo(userId);
        } catch (SQLException e) {
            logger.error("Error fetching tickets by assigned user", e);
            throw new DatabaseException("Failed to fetch tickets", e);
        }
    }

    public Ticket updateTicket(Ticket ticket, byte[] currentUserId) throws TicketTrackerException {
        try {
            Ticket existingTicket = getTicket(ticket.getId());

            validateTicket(ticket);

            Ticket updatedTicket = ticketDAO.update(ticket);

            String changes = buildChangeDescription(existingTicket, ticket);
            createAuditLog(ticket.getId(), null, currentUserId, "Ticket updated",
                    changes, "Ticket updated successfully", "ticket_action");

            logger.info("Ticket updated: {} by user: {}",
                    ticket.getTicketNumber(), bytesToHex(currentUserId));

            return updatedTicket;
        } catch (SQLException e) {
            logger.error("Error updating ticket", e);
            throw new DatabaseException("Failed to update ticket", e);
        }
    }

    public void updateTicketStatus(byte[] ticketId, String newStatus, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            Ticket ticket = getTicket(ticketId);
            String oldStatus = ticket.getStatus();

            ticket.setStatus(newStatus);
            ticketDAO.update(ticket);

            String description = String.format("Status changed from '%s' to '%s'", oldStatus, newStatus);
            createAuditLog(ticketId, null, currentUserId, "Status changed",
                    description, description, "status_change");

            logger.info("Ticket status updated: {} from {} to {}",
                    ticket.getTicketNumber(), oldStatus, newStatus);

        } catch (SQLException e) {
            logger.error("Error updating ticket status", e);
            throw new DatabaseException("Failed to update ticket status", e);
        }
    }

    public void assignTicket(byte[] ticketId, byte[] userId, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            Ticket ticket = getTicket(ticketId);

            User assignee = userDAO.findById(userId);
            if (assignee == null) {
                throw new ResourceNotFoundException("User", bytesToHex(userId));
            }

            byte[] oldAssignee = ticket.getAssignedTo();
            ticket.setAssignedTo(userId);
            ticketDAO.update(ticket);

            String description = String.format("Ticket assigned to %s", assignee.getName());
            createAuditLog(ticketId, null, currentUserId, "Ticket assigned",
                    description, description, "assignment_change");

            logger.info("Ticket assigned: {} to user: {}",
                    ticket.getTicketNumber(), assignee.getName());

        } catch (SQLException e) {
            logger.error("Error assigning ticket", e);
            throw new DatabaseException("Failed to assign ticket", e);
        }
    }

    public void deleteTicket(byte[] ticketId, byte[] currentUserId) throws TicketTrackerException {
        try {
            Ticket ticket = getTicket(ticketId);

            createAuditLog(ticketId, null, currentUserId, "Ticket deleted",
                    null, "Ticket deleted", "ticket_action");

            boolean deleted = ticketDAO.delete(ticketId);
            if (!deleted) {
                throw new DatabaseException("Failed to delete ticket");
            }

            logger.info("Ticket deleted: {} by user: {}",
                    ticket.getTicketNumber(), bytesToHex(currentUserId));

        } catch (SQLException e) {
            logger.error("Error deleting ticket", e);
            throw new DatabaseException("Failed to delete ticket", e);
        }
    }

    public List<Ticket> searchTickets(String searchTerm) throws TicketTrackerException {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return getAllTickets();
            }
            return ticketDAO.searchTickets(searchTerm);
        } catch (SQLException e) {
            logger.error("Error searching tickets", e);
            throw new DatabaseException("Failed to search tickets", e);
        }
    }

    public int getTicketCountByStatus(String status) throws TicketTrackerException {
        try {
            return ticketDAO.countByStatus(status);
        } catch (SQLException e) {
            logger.error("Error counting tickets by status", e);
            throw new DatabaseException("Failed to count tickets", e);
        }
    }

    private void validateTicket(Ticket ticket) throws ValidationException {
        logger.info("Validating ticket: {}", ticket);
        logger.info("  - Title: '{}'", ticket.getTitle());
        logger.info("  - Description: '{}'", ticket.getDescription());
        logger.info("  - ModuleId: {}", ticket.getModuleId() != null ? "present" : "NULL");
        logger.info("  - Department: '{}'", ticket.getDepartment());
        logger.info("  - Category: {}", ticket.getCategory() != null ? "'" + ticket.getCategory() + "'" : "NULL");

        ValidationException validation = new ValidationException("Ticket validation failed");

        if (ticket.getTitle() == null || ticket.getTitle().trim().isEmpty()) {
            validation.addError("Title is required");
        }

        if (ticket.getModuleId() == null) {
            validation.addError("Module is required");
        }

        if (ticket.getDepartment() != null && ticket.getDepartment().trim().isEmpty()) {
            validation.addError("Department cannot be empty if provided");
        }

        if (ticket.getPropertyId() != null && ticket.getPropertyId().trim().isEmpty()) {
            validation.addError("Property ID cannot be empty if provided");
        }

        if (ticket.getPropertyLocation() != null && ticket.getPropertyLocation().trim().isEmpty()) {
            validation.addError("Property Location cannot be empty if provided");
        }

        if (validation.getValidationErrors().size() > 0) {
            logger.error("Ticket validation failed with {} error(s)", validation.getValidationErrors().size());
            throw validation;
        }

        logger.info("Ticket validation passed");
    }

    private void createAuditLog(byte[] ticketId, byte[] stepId, byte[] performedBy,
                                String action, String oldData, String description,
                                String category) throws SQLException {
        AuditLog auditLog = new AuditLog();
        auditLog.setTicketId(ticketId);
        auditLog.setStepId(stepId);
        auditLog.setPerformedBy(performedBy);
        auditLog.setAction(action);
        auditLog.setOldData(oldData);
        auditLog.setDescription(description);
        auditLog.setActionCategory(category);
        auditLogDAO.create(auditLog);
    }

    private String buildChangeDescription(Ticket oldTicket, Ticket newTicket) {
        StringBuilder changes = new StringBuilder();

        if (!oldTicket.getTitle().equals(newTicket.getTitle())) {
            changes.append("Title changed; ");
        }
        if (!oldTicket.getStatus().equals(newTicket.getStatus())) {
            changes.append(String.format("Status: %s -> %s; ",
                    oldTicket.getStatus(), newTicket.getStatus()));
        }
        if ((oldTicket.getPriority() == null && newTicket.getPriority() != null) ||
            (oldTicket.getPriority() != null && !oldTicket.getPriority().equals(newTicket.getPriority()))) {
            changes.append("Priority changed; ");
        }

        return changes.length() > 0 ? changes.toString() : "Minor updates";
    }

    private String generateTicketNumber() throws SQLException {
        return "TKT-" + System.currentTimeMillis();
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
