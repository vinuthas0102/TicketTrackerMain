package com.tickettracker.service;

import com.tickettracker.dao.*;
import com.tickettracker.exception.*;
import com.tickettracker.model.*;
import com.tickettracker.util.ByteArrayUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    private final WorkflowStepDAO workflowStepDAO;
    private final WorkflowStepDependencyDAO dependencyDAO;
    private final WorkflowStepFileReferenceDAO fileReferenceDAO;
    private final AuditLogDAO auditLogDAO;
    private final TicketDAO ticketDAO;

    public WorkflowService() {
        this.workflowStepDAO = new WorkflowStepDAO();
        this.dependencyDAO = new WorkflowStepDependencyDAO();
        this.fileReferenceDAO = new WorkflowStepFileReferenceDAO();
        this.auditLogDAO = new AuditLogDAO();
        this.ticketDAO = new TicketDAO();
    }

    private void ensureTicketReviewed(byte[] ticketId) throws TicketTrackerException {
        try {
            Ticket ticket = ticketDAO.findById(ticketId);
            if (ticket == null) {
                throw new ValidationException("Ticket not found");
            }
            String status = ticket.getStatus() == null ? "" : ticket.getStatus().toUpperCase();
            if (!TASK_ELIGIBLE_STATUSES.contains(status)) {
                throw new ValidationException(
                    "Kindly change the status of the ticket to 'Reviewed' before proceeding further.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to verify ticket status", e);
        }
    }

    private static final java.util.Set<String> TASK_ELIGIBLE_STATUSES = java.util.Set.of(
        "REVIEWED", "APPROVED", "ACTIVE", "SENT_TO_FINANCE",
        "APPROVED_BY_FINANCE", "REJECTED_BY_FINANCE", "COMPLETED", "CLOSED"
    );

    public WorkflowStep createWorkflowStep(WorkflowStep step, byte[] currentUserId) throws TicketTrackerException {
        try {
            validateWorkflowStep(step);
            ensureTicketReviewed(step.getTicketId());

            List<WorkflowStep> existingSteps = workflowStepDAO.findByTicketId(step.getTicketId());
            calculateAndSetStepLevels(step, existingSteps);

            step.setCreatedBy(currentUserId);
            step.setStatus("pending");

            WorkflowStep createdStep = workflowStepDAO.create(step);

            // Create file references from template if provided
            if (step.getFileReferenceTemplateId() != null &&
                step.getSelectedFileReferences() != null &&
                !step.getSelectedFileReferences().isEmpty()) {

                for (SelectedFileReference selectedRef : step.getSelectedFileReferences()) {
                    WorkflowStepFileReference fileRef = new WorkflowStepFileReference();
                    fileRef.setStepId(createdStep.getId());
                    fileRef.setTemplateId(step.getFileReferenceTemplateId());
                    fileRef.setReferenceName(selectedRef.getReferenceName());
                    fileRef.setMandatory(selectedRef.isMandatory());

                    fileReferenceDAO.create(fileRef);
                }

                logger.info("Created {} file references for workflow step {}",
                        step.getSelectedFileReferences().size(), step.getStepNumber());
            }

            createAuditLog(step.getTicketId(), step.getId(), currentUserId,
                    "Workflow step created",
                    String.format("Created step: %s", step.getTitle()),
                    "workflow_action");

            logger.info("Workflow step created: {} by user: {}",
                    step.getStepNumber(), bytesToHex(currentUserId));

            return createdStep;
        } catch (SQLException e) {
            logger.error("Error creating workflow step", e);
            throw new DatabaseException("Failed to create workflow step", e);
        }
    }

    public List<WorkflowStep> createWorkflowStepsBulk(List<WorkflowStep> steps, byte[] currentUserId)
            throws TicketTrackerException {
        List<WorkflowStep> createdSteps = new ArrayList<>();

        try {
            if (steps == null || steps.isEmpty()) {
                return createdSteps;
            }
            ensureTicketReviewed(steps.get(0).getTicketId());

            List<WorkflowStep> stepSnapshot = null;

            for (WorkflowStep step : steps) {
                if (step.getTitle() == null || step.getTitle().trim().isEmpty()) {
                    throw new ValidationException("Title is required for all steps");
                }
                if (step.getTicketId() == null) {
                    throw new ValidationException("Ticket ID is required for all steps");
                }

                if (stepSnapshot == null) {
                    stepSnapshot = new ArrayList<>(workflowStepDAO.findByTicketId(step.getTicketId()));
                }

                calculateAndSetStepLevels(step, stepSnapshot);

                String generatedStepNumber = generateStepNumber(step.getLevel1(), step.getLevel2(), step.getLevel3());
                step.setStepNumber(generatedStepNumber);

                step.setCreatedBy(currentUserId);
                step.setStatus("pending");

                WorkflowStep createdStep = workflowStepDAO.create(step);
                createdSteps.add(createdStep);
                stepSnapshot.add(createdStep);

                // Create file references from template if provided
                if (step.getFileReferenceTemplateId() != null &&
                    step.getSelectedFileReferences() != null &&
                    !step.getSelectedFileReferences().isEmpty()) {

                    for (SelectedFileReference selectedRef : step.getSelectedFileReferences()) {
                        WorkflowStepFileReference fileRef = new WorkflowStepFileReference();
                        fileRef.setStepId(createdStep.getId());
                        fileRef.setTemplateId(step.getFileReferenceTemplateId());
                        fileRef.setReferenceName(selectedRef.getReferenceName());
                        fileRef.setMandatory(selectedRef.isMandatory());

                        fileReferenceDAO.create(fileRef);
                    }

                    logger.info("Created {} file references for workflow step {}",
                            step.getSelectedFileReferences().size(), step.getStepNumber());
                }

                createAuditLog(step.getTicketId(), step.getId(), currentUserId,
                        "Workflow step created",
                        String.format("Created step: %s (bulk operation)", step.getTitle()),
                        "workflow_action");
            }

            logger.info("Bulk workflow step creation: {} steps created by user: {}",
                    createdSteps.size(), bytesToHex(currentUserId));

            return createdSteps;
        } catch (SQLException e) {
            logger.error("Error creating workflow steps in bulk", e);
            throw new DatabaseException("Failed to create workflow steps in bulk", e);
        }
    }

    public WorkflowStep getWorkflowStepById(byte[] stepId) throws TicketTrackerException {
        try {
            WorkflowStep step = workflowStepDAO.findById(stepId);
            if (step == null) {
                throw new ResourceNotFoundException("Workflow step", bytesToHex(stepId));
            }
            return step;
        } catch (SQLException e) {
            logger.error("Error fetching workflow step", e);
            throw new DatabaseException("Failed to fetch workflow step", e);
        }
    }

    public List<WorkflowStep> getWorkflowStepsByTicketId(byte[] ticketId) throws TicketTrackerException {
        try {
            return workflowStepDAO.findByTicketId(ticketId);
        } catch (SQLException e) {
            logger.error("Error fetching workflow steps by ticket", e);
            throw new DatabaseException("Failed to fetch workflow steps", e);
        }
    }

    public List<WorkflowStep> getStepsByAssignedUser(byte[] userId) throws TicketTrackerException {
        try {
            return workflowStepDAO.findByAssignedTo(userId);
        } catch (SQLException e) {
            logger.error("Error fetching workflow steps by assigned user", e);
            throw new DatabaseException("Failed to fetch workflow steps", e);
        }
    }

    public WorkflowStep updateWorkflowStep(WorkflowStepUpdateRequest updateRequest, byte[] currentUserId) throws TicketTrackerException {
        try {
            WorkflowStep existingStep = getWorkflowStepById(updateRequest.getId());
            BigDecimal oldProgress = existingStep.getProgress();

            if ("COMPLETED".equals(existingStep.getStatus())) {
                throw new ForbiddenException("Cannot update a completed workflow step");
            }

            if (updateRequest.getStatus() != null && "completed".equals(updateRequest.getStatus())) {
                boolean allMandatoryFilesComplete = checkMandatoryFileReferencesComplete(updateRequest.getId());
                if (!allMandatoryFilesComplete) {
                    throw new ValidationException("Cannot complete step: All mandatory file references must be uploaded");
                }

                if (updateRequest.getCompletedAt() == null) {
                    updateRequest.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                }
                if (updateRequest.getProgress() == null) {
                    updateRequest.setProgress(new BigDecimal("100"));
                }
            }

            if (updateRequest.getStatus() != null && "wip".equals(updateRequest.getStatus())) {
                if (existingStep.getStartDate() == null && updateRequest.getStartDate() == null) {
                    updateRequest.setStartDate(new Timestamp(System.currentTimeMillis()));
                }
            }

            WorkflowStep updatedStep = workflowStepDAO.updateSelective(updateRequest);

            String changes = buildUpdateChangeDescription(updateRequest);
            String actionCategory = determineActionCategory(updateRequest);
            String newData = updateRequest.getProgress() != null ? updateRequest.getProgress().toPlainString() : null;
            String oldData = oldProgress != null ? oldProgress.toPlainString() : null;
            String metadata = buildUpdateMetadata(updateRequest.getProgress(), updateRequest.getRemarks());

            createAuditLogWithData(updatedStep.getTicketId(), updatedStep.getId(), currentUserId,
                    "WORKFLOW_UPDATED", changes, actionCategory, oldData, newData, metadata);

            logger.info("Workflow step updated: {} by user: {}",
                    updatedStep.getStepNumber(), bytesToHex(currentUserId));

            return updatedStep;
        } catch (SQLException e) {
            logger.error("Error updating workflow step", e);
            throw new DatabaseException("Failed to update workflow step", e);
        }
    }

    public void updateStepProgress(byte[] stepId, double progress, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            if (progress < 0 || progress > 100) {
                throw new ValidationException("progress", "Progress must be between 0 and 100");
            }

            WorkflowStep step = getWorkflowStepById(stepId);
            BigDecimal oldProgress = step.getProgress();

            if ("COMPLETED".equals(step.getStatus())) {
                throw new ForbiddenException("Cannot update progress of a completed workflow step");
            }

            boolean updated = workflowStepDAO.updateProgress(stepId, new BigDecimal(progress));
            if (!updated) {
                throw new DatabaseException("Failed to update progress");
            }

            String description = String.format("Progress updated from %.2f%% to %.2f%%",
                    oldProgress, progress);
            createAuditLog(step.getTicketId(), stepId, currentUserId,
                    "Progress updated", description, "progress_update");

            logger.info("Step progress updated: {} from {}% to {}%",
                    step.getStepNumber(), oldProgress, progress);

        } catch (SQLException e) {
            logger.error("Error updating step progress", e);
            throw new DatabaseException("Failed to update step progress", e);
        }
    }

    public void updateStepStatus(byte[] stepId, String newStatus, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            WorkflowStep step = getWorkflowStepById(stepId);
            String oldStatus = step.getStatus();

            if ("COMPLETED".equals(oldStatus)) {
                throw new ForbiddenException("Cannot change status of a completed workflow step");
            }

            WorkflowStepUpdateRequest updateRequest = new WorkflowStepUpdateRequest();
            updateRequest.setId(stepId);
            updateRequest.setStatus(newStatus);

            if ("completed".equals(newStatus)) {
                updateRequest.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                updateRequest.setProgress(new BigDecimal("100"));
            }

            workflowStepDAO.updateSelective(updateRequest);

            String description = String.format("Status changed from '%s' to '%s'",
                    oldStatus, newStatus);
            createAuditLog(step.getTicketId(), stepId, currentUserId,
                    "Step status changed", description, "status_change");

            logger.info("Step status updated: {} from {} to {}",
                    step.getStepNumber(), oldStatus, newStatus);

        } catch (SQLException e) {
            logger.error("Error updating step status", e);
            throw new DatabaseException("Failed to update step status", e);
        }
    }

    public void deleteWorkflowStep(byte[] stepId, byte[] currentUserId) throws TicketTrackerException {
        try {
            WorkflowStep step = getWorkflowStepById(stepId);

            if ("COMPLETED".equals(step.getStatus())) {
                throw new ForbiddenException("Cannot delete a completed workflow step");
            }

            if ("civil_inspection".equals(step.getStepType()) || "electrical_inspection".equals(step.getStepType())) {
                throw new ForbiddenException("Auto-generated inspection steps cannot be deleted");
            }

            dependencyDAO.deleteByStepId(stepId);

            createAuditLog(step.getTicketId(), stepId, currentUserId,
                    "Workflow step deleted",
                    String.format("Deleted step: %s", step.getTitle()),
                    "workflow_action");

            boolean deleted = workflowStepDAO.delete(stepId);
            if (!deleted) {
                throw new DatabaseException("Failed to delete workflow step");
            }

            logger.info("Workflow step deleted: {} by user: {}",
                    step.getStepNumber(), bytesToHex(currentUserId));

        } catch (SQLException e) {
            logger.error("Error deleting workflow step", e);
            throw new DatabaseException("Failed to delete workflow step", e);
        }
    }

    public void addDependency(byte[] stepId, byte[] dependsOnStepId, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            if (bytesEquals(stepId, dependsOnStepId)) {
                throw new ValidationException("A step cannot depend on itself");
            }

            WorkflowStep step = getWorkflowStepById(stepId);
            WorkflowStep dependsOnStep = getWorkflowStepById(dependsOnStepId);

            if (!bytesEquals(step.getTicketId(), dependsOnStep.getTicketId())) {
                throw new ValidationException("Dependencies must be within the same ticket");
            }

            boolean exists = dependencyDAO.dependencyExists(stepId, dependsOnStepId);
            if (exists) {
                throw new ValidationException("Dependency already exists");
            }

            WorkflowStepDependency dependency = new WorkflowStepDependency();
            dependency.setStepId(stepId);
            dependency.setDependsOnStepId(dependsOnStepId);
            dependency.setCreatedBy(currentUserId);
            dependency.setActive(true);

            dependencyDAO.create(dependency);

            String description = String.format("Added dependency: %s depends on %s",
                    step.getStepNumber(), dependsOnStep.getStepNumber());
            createAuditLog(step.getTicketId(), stepId, currentUserId,
                    "Dependency added", description, "workflow_action");

            logger.info("Dependency added: {} depends on {}", step.getStepNumber(),
                    dependsOnStep.getStepNumber());

        } catch (SQLException e) {
            logger.error("Error adding dependency", e);
            throw new DatabaseException("Failed to add dependency", e);
        }
    }

    public void removeDependency(byte[] dependencyId, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            WorkflowStepDependency dependency = dependencyDAO.findById(dependencyId);
            if (dependency == null) {
                throw new ResourceNotFoundException("Dependency", bytesToHex(dependencyId));
            }

            WorkflowStep step = getWorkflowStepById(dependency.getStepId());

            boolean deleted = dependencyDAO.delete(dependencyId);
            if (!deleted) {
                throw new DatabaseException("Failed to remove dependency");
            }

            createAuditLog(step.getTicketId(), dependency.getStepId(), currentUserId,
                    "Dependency removed", "Dependency removed", "workflow_action");

            logger.info("Dependency removed by user: {}", bytesToHex(currentUserId));

        } catch (SQLException e) {
            logger.error("Error removing dependency", e);
            throw new DatabaseException("Failed to remove dependency", e);
        }
    }

    private void calculateAndSetStepLevels(WorkflowStep step, List<WorkflowStep> existingSteps) throws TicketTrackerException {
        int level1 = 0;
        int level2 = 0;
        int level3 = 0;

        if (step.getParentStepId() == null) {
            int maxLevel1 = 0;
            for (WorkflowStep existingStep : existingSteps) {
                if (existingStep.getLevel1() != null && existingStep.getLevel1() > maxLevel1) {
                    maxLevel1 = existingStep.getLevel1();
                }
            }
            level1 = maxLevel1 + 1;
            level2 = 0;
            level3 = 0;
        } else {
            WorkflowStep parentStep = null;
            for (WorkflowStep existingStep : existingSteps) {
                if (bytesEquals(existingStep.getId(), step.getParentStepId())) {
                    parentStep = existingStep;
                    break;
                }
            }

            if (parentStep == null) {
                throw new ValidationException("Parent step not found");
            }

            Integer parentLevel1 = parentStep.getLevel1() != null ? parentStep.getLevel1() : 0;
            Integer parentLevel2 = parentStep.getLevel2() != null ? parentStep.getLevel2() : 0;
            Integer parentLevel3 = parentStep.getLevel3() != null ? parentStep.getLevel3() : 0;

            if (parentLevel2 == 0 && parentLevel3 == 0) {
                int maxLevel2 = 0;
                for (WorkflowStep existingStep : existingSteps) {
                    Integer stepLevel1 = existingStep.getLevel1() != null ? existingStep.getLevel1() : 0;
                    Integer stepLevel2 = existingStep.getLevel2() != null ? existingStep.getLevel2() : 0;
                    Integer stepLevel3 = existingStep.getLevel3() != null ? existingStep.getLevel3() : 0;

                    if (stepLevel1.equals(parentLevel1) && stepLevel2 > 0 && stepLevel3 == 0) {
                        if (stepLevel2 > maxLevel2) {
                            maxLevel2 = stepLevel2;
                        }
                    }
                }
                level1 = parentLevel1;
                level2 = maxLevel2 + 1;
                level3 = 0;
            } else if (parentLevel2 > 0 && parentLevel3 == 0) {
                int maxLevel3 = 0;
                for (WorkflowStep existingStep : existingSteps) {
                    Integer stepLevel1 = existingStep.getLevel1() != null ? existingStep.getLevel1() : 0;
                    Integer stepLevel2 = existingStep.getLevel2() != null ? existingStep.getLevel2() : 0;
                    Integer stepLevel3 = existingStep.getLevel3() != null ? existingStep.getLevel3() : 0;

                    if (stepLevel1.equals(parentLevel1) && stepLevel2.equals(parentLevel2) && stepLevel3 > 0) {
                        if (stepLevel3 > maxLevel3) {
                            maxLevel3 = stepLevel3;
                        }
                    }
                }
                level1 = parentLevel1;
                level2 = parentLevel2;
                level3 = maxLevel3 + 1;
            } else {
                throw new ValidationException("Maximum hierarchy depth (3 levels) reached. Cannot add sub-step to a level 3 step.");
            }
        }

        step.setLevel1(level1);
        step.setLevel2(level2);
        step.setLevel3(level3);

        logger.debug("Calculated step levels: level_1={}, level_2={}, level_3={}", level1, level2, level3);
    }

    public boolean canUserUpdateWorkflowStep(byte[] userId, byte[] stepId) throws TicketTrackerException {
        try {
            UserDAO userDAO = new UserDAO();
            User user = userDAO.findById(userId);
            if (user == null) {
                return false;
            }

            if ("eo".equalsIgnoreCase(user.getRoleInternal())) {
                return true;
            }

            WorkflowStep step = workflowStepDAO.findById(stepId);
            if (step == null) {
                return false;
            }

            if (step.getAssignedTo() != null && bytesEquals(step.getAssignedTo(), userId)) {
                return true;
            }

            if ("dept_officer".equalsIgnoreCase(user.getRoleInternal())) {
                TicketDAO ticketDAO = new TicketDAO();
                Ticket ticket = ticketDAO.findById(step.getTicketId());
                if (ticket != null && user.getDepartment() != null &&
                    user.getDepartment().equalsIgnoreCase(ticket.getDepartment())) {
                    return true;
                }
            }

            return false;
        } catch (SQLException e) {
            logger.error("Error checking workflow step update permission", e);
            throw new DatabaseException("Failed to check permissions", e);
        }
    }

    public boolean canUserCreateWorkflowStep(byte[] userId, byte[] ticketId) throws TicketTrackerException {
        try {
            UserDAO userDAO = new UserDAO();
            User user = userDAO.findById(userId);
            if (user == null) {
                return false;
            }

            return "eo".equalsIgnoreCase(user.getRoleInternal());
        } catch (SQLException e) {
            logger.error("Error checking workflow step creation permission", e);
            throw new DatabaseException("Failed to check permissions", e);
        }
    }

    public boolean canUserDeleteWorkflowStep(byte[] userId, byte[] stepId) throws TicketTrackerException {
        try {
            UserDAO userDAO = new UserDAO();
            User user = userDAO.findById(userId);
            if (user == null) {
                return false;
            }

            return "eo".equalsIgnoreCase(user.getRoleInternal());
        } catch (SQLException e) {
            logger.error("Error checking workflow step deletion permission", e);
            throw new DatabaseException("Failed to check permissions", e);
        }
    }

    public boolean checkMandatoryFileReferencesComplete(byte[] stepId) throws TicketTrackerException {
        try {
            List<WorkflowStepFileReference> references = fileReferenceDAO.findByStepId(stepId);

            for (WorkflowStepFileReference ref : references) {
                if (ref.isMandatory() && ref.getDocumentId() == null) {
                    logger.warn("Mandatory file reference '{}' not completed for step {}",
                            ref.getReferenceName(), bytesToHex(stepId));
                    return false;
                }
            }

            return true;
        } catch (SQLException e) {
            logger.error("Error checking mandatory file references", e);
            throw new DatabaseException("Failed to check file references", e);
        }
    }

    private String generateStepNumber(Integer level1, Integer level2, Integer level3) {
        int l1 = level1 != null ? level1 : 1;
        int l2 = level2 != null ? level2 : 0;
        int l3 = level3 != null ? level3 : 0;

        return l1 + "." + l2 + "." + l3;
    }

    private void validateWorkflowStep(WorkflowStep step) throws ValidationException {
        ValidationException validation = new ValidationException("Workflow step validation failed");

        if (step.getTitle() == null || step.getTitle().trim().isEmpty()) {
            validation.addError("Title is required");
        }

        if (step.getTicketId() == null) {
            validation.addError("Ticket ID is required");
        }

        if (step.getStepNumber() == null || step.getStepNumber().trim().isEmpty()) {
            validation.addError("Step number is required");
        }

        if (validation.getValidationErrors().size() > 0) {
            throw validation;
        }
    }

    private void createAuditLog(byte[] ticketId, byte[] stepId, byte[] performedBy,
                                String action, String description, String category)
            throws SQLException {
        AuditLog auditLog = new AuditLog();
        auditLog.setTicketId(ticketId);
        auditLog.setStepId(stepId);
        auditLog.setPerformedBy(performedBy);
        auditLog.setAction(action);
        auditLog.setDescription(description);
        auditLog.setActionCategory(category);
        auditLogDAO.create(auditLog);
    }

    private void createAuditLogWithData(byte[] ticketId, byte[] stepId, byte[] performedBy,
                                        String action, String description, String category,
                                        String oldData, String newData, String metadata)
            throws SQLException {
        logger.debug("Creating audit log - TicketID: {}, StepID: {}, Action: {}, Category: {}",
                bytesToHex(ticketId), bytesToHex(stepId), action, category);

        AuditLog auditLog = new AuditLog();
        auditLog.setTicketId(ticketId);
        auditLog.setStepId(stepId);
        auditLog.setPerformedBy(performedBy);
        auditLog.setAction(action);
        auditLog.setDescription(description);
        auditLog.setActionCategory(category);
        if (oldData != null) auditLog.setOldData(oldData);
        if (newData != null) auditLog.setNewData(newData);
        if (metadata != null) auditLog.setMetadata(metadata);

        try {
            auditLogDAO.create(auditLog);
            logger.debug("Successfully created audit log for StepID: {}", bytesToHex(stepId));
        } catch (SQLException e) {
            logger.error("CRITICAL: Failed to create audit log - StepID: {}, Action: {}, Error: {}",
                    bytesToHex(stepId), action, e.getMessage(), e);
            throw e;
        }
    }

    private String buildUpdateMetadata(BigDecimal progress, String remarks) {
        StringBuilder sb = new StringBuilder("{");
        boolean hasField = false;
        if (progress != null) {
            sb.append("\"progress\":").append(progress.toPlainString());
            hasField = true;
        }
        if (remarks != null && !remarks.trim().isEmpty()) {
            if (hasField) sb.append(",");
            String escaped = remarks.trim().replace("\\", "\\\\").replace("\"", "\\\"");
            sb.append("\"comment\":\"").append(escaped).append("\"");
            sb.append(",\"remarks\":\"").append(escaped).append("\"");
        }
        sb.append("}");
        return hasField || (remarks != null && !remarks.trim().isEmpty()) ? sb.toString() : null;
    }

    private String buildUpdateChangeDescription(WorkflowStepUpdateRequest updateRequest) {
        StringBuilder changes = new StringBuilder();

        if (updateRequest.getTitle() != null) {
            changes.append("Title updated; ");
        }
        if (updateRequest.getDescription() != null) {
            changes.append("Description updated; ");
        }
        if (updateRequest.getStatus() != null) {
            changes.append(String.format("Status changed to '%s'; ", updateRequest.getStatus()));
        }
        if (updateRequest.getAssignedTo() != null) {
            changes.append("Assignment changed; ");
        }
        if (updateRequest.getProgress() != null) {
            changes.append(String.format("Progress updated to %.0f%%; ", updateRequest.getProgress().doubleValue()));
        }
        if (updateRequest.getDueDate() != null) {
            changes.append("Due date updated; ");
        }
        if (updateRequest.getStartDate() != null) {
            changes.append("Start date updated; ");
        }

        return changes.length() > 0 ? changes.toString() : "Minor updates";
    }

    private String determineActionCategory(WorkflowStepUpdateRequest updateRequest) {
        if (updateRequest.getStatus() != null) {
            return "status_change";
        }
        if (updateRequest.getAssignedTo() != null) {
            return "assignment_change";
        }
        if (updateRequest.getProgress() != null) {
            return "progress_update";
        }
        return "workflow_action";
    }

    private boolean bytesEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return a == b;
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * Creates Civil and Electrical inspection workflow steps for tickets with request types
     * that require C&E inspections (e.g., "Vacation Handover", "Annual Maintenance").
     *
     * @param ticketId The ticket ID to create steps for
     * @param currentUserId The user creating the ticket
     * @throws TicketTrackerException if step creation fails
     */
    public void createCEInspectionStepsIfRequired(byte[] ticketId, String requestType, byte[] currentUserId)
            throws TicketTrackerException {

        if (requestType == null || requestType.trim().isEmpty()) {
            return;
        }

        // Check if request type requires C&E inspection
        boolean requiresCEInspection = "Vacation Handover".equals(requestType) ||
                                        "Annual Maintenance".equals(requestType);

        if (!requiresCEInspection) {
            logger.debug("Request type '{}' does not require C&E inspection", requestType);
            return;
        }

        try {
            UserDAO userDAO = new UserDAO();

            // Find Civil Manager
            User civilManager = userDAO.findByDepartment("Civil Manager");

            // Find Electrical Manager
            User electricalManager = userDAO.findByDepartment("Electrical Manager");

            if (civilManager == null) {
                logger.warn("Civil Manager user not found. C&E inspection step creation skipped.");
            }
            if (electricalManager == null) {
                logger.warn("Electrical Manager user not found. C&E inspection step creation skipped.");
            }

            List<WorkflowStep> existingSteps = workflowStepDAO.findByTicketId(ticketId);
            int nextLevel1 = existingSteps.isEmpty() ? 1 :
                    existingSteps.stream().mapToInt(WorkflowStep::getLevel1).max().orElse(0) + 1;

            // Create Civil Inspection Step
            if (civilManager != null) {
                WorkflowStep civilStep = new WorkflowStep();
                civilStep.setTicketId(ticketId);
                civilStep.setTitle("Civil Inspection");
                civilStep.setDescription("Civil engineering inspection and approval");
                civilStep.setStatus("pending");
                civilStep.setAssignedTo(civilManager.getId());
                civilStep.setCreatedBy(currentUserId);
                civilStep.setStepType("civil_inspection");
                civilStep.setLevel1(nextLevel1);
                civilStep.setLevel2(0);
                civilStep.setLevel3(0);
                civilStep.setStepNumber(generateStepNumber(nextLevel1, 0, 0));

                workflowStepDAO.create(civilStep);
                logger.info("Created Civil Inspection step for ticket {}", bytesToHex(ticketId));

                createAuditLog(ticketId, civilStep.getId(), currentUserId,
                        "C&E Inspection step auto-created",
                        "Civil Inspection step automatically created for request type: " + requestType,
                        "workflow_action");

                nextLevel1++;
            }

            // Create Electrical Inspection Step
            if (electricalManager != null) {
                WorkflowStep electricalStep = new WorkflowStep();
                electricalStep.setTicketId(ticketId);
                electricalStep.setTitle("Electrical Inspection");
                electricalStep.setDescription("Electrical engineering inspection and approval");
                electricalStep.setStatus("pending");
                electricalStep.setAssignedTo(electricalManager.getId());
                electricalStep.setCreatedBy(currentUserId);
                electricalStep.setStepType("electrical_inspection");
                electricalStep.setLevel1(nextLevel1);
                electricalStep.setLevel2(0);
                electricalStep.setLevel3(0);
                electricalStep.setStepNumber(generateStepNumber(nextLevel1, 0, 0));

                workflowStepDAO.create(electricalStep);
                logger.info("Created Electrical Inspection step for ticket {}", bytesToHex(ticketId));

                createAuditLog(ticketId, electricalStep.getId(), currentUserId,
                        "C&E Inspection step auto-created",
                        "Electrical Inspection step automatically created for request type: " + requestType,
                        "workflow_action");
            }

        } catch (SQLException e) {
            logger.error("Error creating C&E inspection steps for ticket {}", bytesToHex(ticketId), e);
            throw new DatabaseException("Failed to create C&E inspection steps", e);
        }
    }
}
