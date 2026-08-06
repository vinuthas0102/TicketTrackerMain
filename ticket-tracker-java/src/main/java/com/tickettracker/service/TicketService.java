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
    private final MasterDataService masterDataService;
    private final ModuleDAO moduleDAO;

    public TicketService() {
        this.ticketDAO = new TicketDAO();
        this.workflowStepDAO = new WorkflowStepDAO();
        this.auditLogDAO = new AuditLogDAO();
        this.userDAO = new UserDAO();
        this.masterDataService = new MasterDataService();
        this.moduleDAO = new ModuleDAO();
    }

    public Ticket createTicket(Ticket ticket, byte[] currentUserId) throws TicketTrackerException {
        try {
            validateTicket(ticket);

            ticket.setCreatedBy(currentUserId);

            if ((ticket.getDepartment() == null || ticket.getDepartment().trim().isEmpty())
                    && currentUserId != null) {
                try {
                    User creator = userDAO.findById(currentUserId);
                    if (creator != null && creator.getDepartment() != null
                            && !creator.getDepartment().trim().isEmpty()) {
                        ticket.setDepartment(creator.getDepartment());
                        logger.info("Auto-filled department '{}' from creating user",
                                creator.getDepartment());
                    }
                } catch (SQLException e) {
                    logger.warn("Could not look up creating user for department auto-fill: {}",
                            e.getMessage());
                }
            }

            if (ticket.getStatus() == null || ticket.getStatus().trim().isEmpty()) {
                ticket.setStatus("draft");
            } else {
                ticket.setStatus(ticket.getStatus().toLowerCase());
            }

            if (ticket.getPropertyId() == null || ticket.getPropertyId().trim().isEmpty()) {
                ticket.setPropertyId("PROP001");
            }

            if (ticket.getPropertyLocation() == null || ticket.getPropertyLocation().trim().isEmpty()) {
                List<String> activeLocations = masterDataService.getActiveLocationNames();
                ticket.setPropertyLocation(activeLocations.isEmpty() ? "LOC" : activeLocations.get(0));
            }

            if (ticket.getTicketNumber() == null || ticket.getTicketNumber().isEmpty()) {
                String moduleCode = masterDataService.getModuleCode(ticket.getModuleId());
                ticket.setTicketNumber(masterDataService.generateTicketNumber(ticket.getPropertyLocation(), moduleCode));
            }

            if (ticket.getModuleId() != null) {
                try {
                    Module module = moduleDAO.findById(ticket.getModuleId());
                    if (module != null && module.getConfig() != null) {
                        String config = module.getConfig();
                        if (config.contains("\"requiresFinanceApproval\":true") ||
                            config.contains("\"requiresFinanceApproval\": true")) {
                            ticket.setRequiresFinanceApproval(true);
                        } else {
                            ticket.setRequiresFinanceApproval(false);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to load module config for finance approval: {}", e.getMessage());
                }
            }

            Ticket createdTicket = ticketDAO.create(ticket);

            createAuditLog(createdTicket.getId(), null, currentUserId, "Ticket created",
                    null, "Ticket created successfully", "ticket_action");

            logger.info("Ticket created: {} by user: {}",
                    createdTicket.getTicketNumber(), bytesToHex(currentUserId));

            createCEInspectionStepsIfRequired(createdTicket, currentUserId);

            return createdTicket;
        } catch (SQLException e) {
            logger.error("Error creating ticket: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to create ticket: " + e.getMessage(), e);
        }
    }

    private void createCEInspectionStepsIfRequired(Ticket ticket, byte[] currentUserId) {
        String requestType = ticket.getRequestType();
        if (requestType == null || requestType.trim().isEmpty()) return;

        requestType = requestType.trim();
        if (!requestType.equals("Vacation Handover") && !requestType.equals("Annual Maintenance")) return;

        logger.info("Auto-creating Civil and Electrical inspection steps for ticket: {} with request type: {}",
                ticket.getTicketNumber(), requestType);

        try {
            logger.debug("Searching for Civil Manager department user");
            User civilManager = userDAO.findByDepartment("Civil Manager");
            if (civilManager == null) {
                logger.warn("Civil Manager not found in users table - Civil inspection step will be created without assignment");
            } else {
                logger.debug("Found Civil Manager: {} (ID: {})", civilManager.getName(),
                        UuidUtil.bytesToUuid(civilManager.getId()));
            }

            logger.debug("Searching for Electrical Manager department user");
            User electricalManager = userDAO.findByDepartment("Electrical Manager");
            if (electricalManager == null) {
                logger.warn("Electrical Manager not found in users table - Electrical inspection step will be created without assignment");
            } else {
                logger.debug("Found Electrical Manager: {} (ID: {})", electricalManager.getName(),
                        UuidUtil.bytesToUuid(electricalManager.getId()));
            }

            WorkflowStep civilStep = new WorkflowStep();
            civilStep.setTicketId(ticket.getId());
            civilStep.setTitle("Civil Inspection");
            civilStep.setDescription("Auto-generated Civil inspection step");
            civilStep.setStatus("pending");
            civilStep.setStepType("civil_inspection");
            civilStep.setCompletionCertificateRequired(true);
            if (civilManager != null) {
                civilStep.setAssignedTo(civilManager.getId());
            }

            List<WorkflowStep> existing = workflowStepDAO.findByTicketId(ticket.getId());
            int nextLevel1 = existing.size() + 1;
            civilStep.setLevel1(nextLevel1);
            civilStep.setLevel2(0);
            civilStep.setLevel3(0);
            civilStep.setStepNumber(nextLevel1 + ".0.0");
            civilStep.setCreatedBy(currentUserId);
            workflowStepDAO.create(civilStep);
            logger.info("Created Civil inspection step (step number: {}) for ticket: {}",
                    civilStep.getStepNumber(), ticket.getTicketNumber());

            WorkflowStep electricalStep = new WorkflowStep();
            electricalStep.setTicketId(ticket.getId());
            electricalStep.setTitle("Electrical Inspection");
            electricalStep.setDescription("Auto-generated Electrical inspection step");
            electricalStep.setStatus("pending");
            electricalStep.setStepType("electrical_inspection");
            electricalStep.setCompletionCertificateRequired(true);
            if (electricalManager != null) {
                electricalStep.setAssignedTo(electricalManager.getId());
            }

            existing = workflowStepDAO.findByTicketId(ticket.getId());
            int nextLevel1E = existing.size() + 1;
            electricalStep.setLevel1(nextLevel1E);
            electricalStep.setLevel2(0);
            electricalStep.setLevel3(0);
            electricalStep.setStepNumber(nextLevel1E + ".0.0");
            electricalStep.setCreatedBy(currentUserId);
            workflowStepDAO.create(electricalStep);
            logger.info("Created Electrical inspection step (step number: {}) for ticket: {}",
                    electricalStep.getStepNumber(), ticket.getTicketNumber());

            logger.info("Successfully auto-created Civil and Electrical inspection steps for ticket: {}",
                    ticket.getTicketNumber());
        } catch (Exception e) {
            logger.error("Failed to create C&E inspection steps for ticket: {} - Error: {}",
                    ticket.getTicketNumber(), e.getMessage(), e);
        }
    }

    public BulkTicketOperationResult createTicketsBulk(List<Ticket> tickets, byte[] currentUserId)
            throws TicketTrackerException {
        BulkTicketOperationResult result = new BulkTicketOperationResult(tickets.size());

        for (int i = 0; i < tickets.size(); i++) {
            Ticket ticket = tickets.get(i);
            try {
                validateTicket(ticket);

                ticket.setCreatedBy(currentUserId);

                if ((ticket.getDepartment() == null || ticket.getDepartment().trim().isEmpty())
                        && currentUserId != null) {
                    try {
                        User creator = userDAO.findById(currentUserId);
                        if (creator != null && creator.getDepartment() != null
                                && !creator.getDepartment().trim().isEmpty()) {
                            ticket.setDepartment(creator.getDepartment());
                        }
                    } catch (SQLException e) {
                        logger.warn("Could not look up creating user for department auto-fill: {}",
                                e.getMessage());
                    }
                }

                if (ticket.getStatus() == null || ticket.getStatus().trim().isEmpty()) {
                    ticket.setStatus("draft");
                } else {
                    ticket.setStatus(ticket.getStatus().toLowerCase());
                }

                if (ticket.getPropertyId() == null || ticket.getPropertyId().trim().isEmpty()) {
                    ticket.setPropertyId("PROP001");
                }

                if (ticket.getPropertyLocation() == null || ticket.getPropertyLocation().trim().isEmpty()) {
                    List<String> activeLocations = masterDataService.getActiveLocationNames();
                    ticket.setPropertyLocation(activeLocations.isEmpty() ? "LOC" : activeLocations.get(0));
                }

                if (ticket.getTicketNumber() == null || ticket.getTicketNumber().isEmpty()) {
                    String moduleCode = masterDataService.getModuleCode(ticket.getModuleId());
                    ticket.setTicketNumber(masterDataService.generateTicketNumber(ticket.getPropertyLocation(), moduleCode));
                }

                Ticket createdTicket = ticketDAO.create(ticket);
                result.addSuccess(createdTicket.getId());

                createAuditLog(createdTicket.getId(), null, currentUserId, "Ticket created",
                        null, "Ticket created via bulk operation", "ticket_action");

                logger.info("Bulk operation: Created ticket {} (index {})",
                        createdTicket.getTicketNumber(), i);

            } catch (ValidationException e) {
                logger.error("Validation failed for ticket at index {}: {}", i, e.getMessage());
                result.addFailure(i, ticket.getTitle(), e.getMessage());
            } catch (SQLException e) {
                logger.error("Database error creating ticket at index {}: {}", i, e.getMessage());
                result.addFailure(i, ticket.getTitle(), "Database error: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error creating ticket at index {}: {}", i, e.getMessage(), e);
                result.addFailure(i, ticket.getTitle(), "Error: " + e.getMessage());
            }
        }

        logger.info("Bulk ticket creation completed: {} success, {} failed, {} total by user: {}",
                result.getSuccessCount(), result.getFailedCount(), result.getTotalCount(),
                bytesToHex(currentUserId));

        return result;
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

    public List<Ticket> getAccessibleTicketsByModule(byte[] userId, byte[] moduleId) throws TicketTrackerException {
        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                throw new ResourceNotFoundException("User", bytesToHex(userId));
            }

            return ticketDAO.findAccessibleTicketsByModule(userId, user.getRole(), moduleId);
        } catch (SQLException e) {
            logger.error("Error fetching accessible tickets by module", e);
            throw new DatabaseException("Failed to fetch accessible tickets by module", e);
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

            if ("COMPLETED".equals(existingTicket.getStatus())) {
                throw new ForbiddenException("Cannot update a completed ticket");
            }

            User currentUser = userDAO.findById(currentUserId);
            if (currentUser != null && "eo".equalsIgnoreCase(currentUser.getRole())
                    && existingTicket.getCreatedBy() != null
                    && !java.util.Arrays.equals(existingTicket.getCreatedBy(), currentUserId)) {
                if (ticket.getTitle() != null && !ticket.getTitle().equals(existingTicket.getTitle())) {
                    throw new ForbiddenException("EO users can only modify Priority and Category on tickets raised by other users");
                }
                if (ticket.getDescription() != null && !ticket.getDescription().equals(existingTicket.getDescription())) {
                    throw new ForbiddenException("EO users can only modify Priority and Category on tickets raised by other users");
                }
            }

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

            String effectiveStatus = newStatus;
            if ("submitted".equalsIgnoreCase(newStatus)) {
                User currentUser = userDAO.findById(currentUserId);
                if (currentUser != null && "eo".equalsIgnoreCase(currentUser.getRole())) {
                    effectiveStatus = "active";
                }
            }

            ticket.setStatus(effectiveStatus);
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

            if ("COMPLETED".equals(ticket.getStatus())) {
                throw new ForbiddenException("Cannot delete a completed ticket");
            }

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

    public boolean canUserAccessTicket(byte[] userId, byte[] ticketId) throws TicketTrackerException {
        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                return false;
            }

            Ticket ticket = ticketDAO.findById(ticketId);
            if (ticket == null) {
                return false;
            }

            String userRole = user.getRole();
            if (userRole == null) {
                return false;
            }

            if ("admin".equalsIgnoreCase(userRole)) {
                return true;
            }

            if ("finance".equalsIgnoreCase(userRole)) {
                if (ticket.getFinanceOfficerId() != null && java.util.Arrays.equals(ticket.getFinanceOfficerId(), userId)) {
                    return true;
                }
                String ticketStatus = ticket.getStatus();
                if (ticketStatus != null && ("sent_to_finance".equalsIgnoreCase(ticketStatus)
                        || "approved_by_finance".equalsIgnoreCase(ticketStatus)
                        || "rejected_by_finance".equalsIgnoreCase(ticketStatus))) {
                    return true;
                }
                return false;
            }

            if ("eo".equalsIgnoreCase(userRole)) {
                if (ticket.getCreatedBy() != null && java.util.Arrays.equals(ticket.getCreatedBy(), userId)) {
                    return true;
                }
                if (ticket.getAssignedTo() != null && java.util.Arrays.equals(ticket.getAssignedTo(), userId)) {
                    return true;
                }
                List<WorkflowStep> steps = workflowStepDAO.findByTicketId(ticketId);
                for (WorkflowStep step : steps) {
                    if (step.getAssignedTo() != null && java.util.Arrays.equals(step.getAssignedTo(), userId)) {
                        return true;
                    }
                }
                List<String> userRegions = userDAO.findRegionsByUserId(userId);
                if (userRegions == null || userRegions.isEmpty()) {
                    return false;
                }
                return userRegions.contains(ticket.getPropertyLocation());
            }

            if ("dept_officer".equalsIgnoreCase(userRole) || "DO".equalsIgnoreCase(userRole)
                    || "technician".equalsIgnoreCase(userRole) || "TECHNICIAN".equalsIgnoreCase(userRole)) {
                if (ticket.getCreatedBy() != null && java.util.Arrays.equals(ticket.getCreatedBy(), userId)) {
                    return true;
                }
                if (ticket.getAssignedTo() != null && java.util.Arrays.equals(ticket.getAssignedTo(), userId)) {
                    return true;
                }
                List<WorkflowStep> steps = workflowStepDAO.findByTicketId(ticketId);
                for (WorkflowStep step : steps) {
                    if (step.getAssignedTo() != null && java.util.Arrays.equals(step.getAssignedTo(), userId)) {
                        return true;
                    }
                }
                List<String> userRegions = userDAO.findRegionsByUserId(userId);
                if (userRegions == null || userRegions.isEmpty() || !userRegions.contains(ticket.getPropertyLocation())) {
                    return false;
                }
                return false;
            }

            if ("employee".equalsIgnoreCase(userRole) || "vendor".equalsIgnoreCase(userRole)) {
                if (ticket.getCreatedBy() != null && java.util.Arrays.equals(ticket.getCreatedBy(), userId)) {
                    return true;
                }
                List<WorkflowStep> steps = workflowStepDAO.findByTicketId(ticketId);
                for (WorkflowStep step : steps) {
                    if (step.getAssignedTo() != null && java.util.Arrays.equals(step.getAssignedTo(), userId)) {
                        return true;
                    }
                }
                List<String> userRegions = userDAO.findRegionsByUserId(userId);
                if (userRegions == null || userRegions.isEmpty() || !userRegions.contains(ticket.getPropertyLocation())) {
                    return false;
                }
                return false;
            }

            return false;
        } catch (SQLException e) {
            logger.error("Error checking ticket access permissions", e);
            throw new DatabaseException("Failed to check ticket access", e);
        }
    }

    public List<byte[]> getAccessibleTicketIdsForUser(byte[] userId) throws TicketTrackerException {
        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                return new ArrayList<>();
            }

            String userRole = user.getRole();
            if (userRole == null) {
                return new ArrayList<>();
            }

            if ("admin".equalsIgnoreCase(userRole)) {
                List<Ticket> allTickets = ticketDAO.findAll();
                List<byte[]> ticketIds = new ArrayList<>();
                for (Ticket ticket : allTickets) {
                    ticketIds.add(ticket.getId());
                }
                return ticketIds;
            }

            if ("finance".equalsIgnoreCase(userRole)) {
                List<Ticket> allTickets = ticketDAO.findAll();
                List<byte[]> ticketIds = new ArrayList<>();
                for (Ticket ticket : allTickets) {
                    boolean isFinanceOfficer = ticket.getFinanceOfficerId() != null
                            && java.util.Arrays.equals(ticket.getFinanceOfficerId(), userId);
                    String ticketStatus = ticket.getStatus();
                    boolean isFinanceTicket = ticketStatus != null && (
                            "sent_to_finance".equalsIgnoreCase(ticketStatus)
                            || "approved_by_finance".equalsIgnoreCase(ticketStatus)
                            || "rejected_by_finance".equalsIgnoreCase(ticketStatus));
                    if (isFinanceOfficer || isFinanceTicket) {
                        ticketIds.add(ticket.getId());
                    }
                }
                return ticketIds;
            }

            if ("eo".equalsIgnoreCase(userRole)) {
                List<String> userRegions = userDAO.findRegionsByUserId(userId);
                List<Ticket> allTickets = ticketDAO.findAll();
                List<byte[]> ticketIds = new ArrayList<>();
                for (Ticket ticket : allTickets) {
                    boolean isOwner = (ticket.getCreatedBy() != null && java.util.Arrays.equals(ticket.getCreatedBy(), userId))
                            || (ticket.getAssignedTo() != null && java.util.Arrays.equals(ticket.getAssignedTo(), userId));
                    boolean hasAssignedStep = false;
                    List<WorkflowStep> steps = workflowStepDAO.findByTicketId(ticket.getId());
                    for (WorkflowStep step : steps) {
                        if (step.getAssignedTo() != null && java.util.Arrays.equals(step.getAssignedTo(), userId)) {
                            hasAssignedStep = true;
                            break;
                        }
                    }
                    if (isOwner || hasAssignedStep || (userRegions != null && userRegions.contains(ticket.getPropertyLocation()))) {
                        ticketIds.add(ticket.getId());
                    }
                }
                return ticketIds;
            }

            if ("dept_officer".equalsIgnoreCase(userRole) || "DO".equalsIgnoreCase(userRole)
                    || "technician".equalsIgnoreCase(userRole) || "TECHNICIAN".equalsIgnoreCase(userRole)) {
                List<String> userRegions = userDAO.findRegionsByUserId(userId);
                List<Ticket> accessibleTickets = ticketDAO.findAccessibleTickets(userId, userRole);
                List<byte[]> ticketIds = new ArrayList<>();
                for (Ticket ticket : accessibleTickets) {
                    boolean isOwner = (ticket.getCreatedBy() != null && java.util.Arrays.equals(ticket.getCreatedBy(), userId))
                            || (ticket.getAssignedTo() != null && java.util.Arrays.equals(ticket.getAssignedTo(), userId));
                    if (isOwner || (userRegions != null && userRegions.contains(ticket.getPropertyLocation()))) {
                        ticketIds.add(ticket.getId());
                    }
                }
                return ticketIds;
            }

            if ("employee".equalsIgnoreCase(userRole) || "vendor".equalsIgnoreCase(userRole)) {
                List<String> userRegions = userDAO.findRegionsByUserId(userId);
                List<WorkflowStep> assignedSteps = workflowStepDAO.findByAssignedTo(userId);
                List<byte[]> ticketIds = new ArrayList<>();
                for (WorkflowStep step : assignedSteps) {
                    if (!ticketIds.contains(step.getTicketId())) {
                        Ticket ticket = ticketDAO.findById(step.getTicketId());
                        if (ticket != null) {
                            boolean isOwner = ticket.getCreatedBy() != null && java.util.Arrays.equals(ticket.getCreatedBy(), userId);
                            if (isOwner || (userRegions != null && userRegions.contains(ticket.getPropertyLocation()))) {
                                ticketIds.add(step.getTicketId());
                            }
                        }
                    }
                }

                return ticketIds;
            }

            return new ArrayList<>();
        } catch (SQLException e) {
            logger.error("Error getting accessible ticket IDs for user", e);
            throw new DatabaseException("Failed to get accessible tickets", e);
        }
    }

    private void validateTicket(Ticket ticket) throws ValidationException {
        logger.info("Validating ticket: {}", ticket);
        logger.info("  - Title: '{}'", ticket.getTitle());
        logger.info("  - Description: '{}'", ticket.getDescription());
        logger.info("  - ModuleId: {}", ticket.getModuleId() != null ? bytesToHex(ticket.getModuleId()) : "NULL");
        logger.info("  - Department: '{}'", ticket.getDepartment());
        logger.info("  - Category: {}", ticket.getCategory() != null ? "'" + ticket.getCategory() + "'" : "NULL");

        ValidationException validation = new ValidationException("Ticket validation failed");

        if (ticket.getTitle() == null || ticket.getTitle().trim().isEmpty()) {
            validation.addError("Title is required");
        }

        if (ticket.getModuleId() == null) {
            validation.addError("Module is required");
        }

        if (validation.getValidationErrors().size() > 0) {
            logger.error("Ticket validation failed with {} error(s)", validation.getValidationErrors().size());
            for (String error : validation.getValidationErrors()) {
                logger.error("  - {}", error);
            }
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

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
