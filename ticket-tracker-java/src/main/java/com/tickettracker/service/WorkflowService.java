package com.tickettracker.service;

import com.tickettracker.dao.*;
import com.tickettracker.exception.*;
import com.tickettracker.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    private static final BigDecimal MIN_PROGRESS = BigDecimal.ZERO;
    private static final BigDecimal MAX_PROGRESS = new BigDecimal("100");
    private static final BigDecimal PROGRESS_COMPARISON_EPSILON = new BigDecimal("0.01");
    private static final long MAX_FILE_SIZE_FOR_INT = Integer.MAX_VALUE;

    private final WorkflowStepDAO workflowStepDAO;
    private final WorkflowStepDependencyDAO dependencyDAO;
    private final AuditLogDAO auditLogDAO;

    public WorkflowService() {
        this.workflowStepDAO = new WorkflowStepDAO();
        this.dependencyDAO = new WorkflowStepDependencyDAO();
        this.auditLogDAO = new AuditLogDAO();
    }

    public WorkflowStep createWorkflowStep(WorkflowStep step, byte[] currentUserId) throws TicketTrackerException {
        try {
            validateWorkflowStep(step);

            calculateAndSetStepLevels(step);

            step.setCreatedBy(currentUserId);
            step.setStatus("pending");

            WorkflowStep createdStep = workflowStepDAO.create(step);

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
            for (WorkflowStep step : steps) {
                validateWorkflowStep(step);

                calculateAndSetStepLevels(step);

                step.setCreatedBy(currentUserId);
                step.setStatus("pending");

                WorkflowStep createdStep = workflowStepDAO.create(step);
                createdSteps.add(createdStep);

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

    public WorkflowStep updateWorkflowStep(WorkflowStep step, byte[] currentUserId) throws TicketTrackerException {
        try {
            WorkflowStep existingStep = getWorkflowStepById(step.getId());

            validateWorkflowStep(step);

            WorkflowStep updatedStep = workflowStepDAO.update(step);

            String changes = buildStepChangeDescription(existingStep, step);
            createAuditLog(step.getTicketId(), step.getId(), currentUserId,
                    "Workflow step updated", changes, "workflow_action");

            logger.info("Workflow step updated: {} by user: {}",
                    step.getStepNumber(), bytesToHex(currentUserId));

            return updatedStep;
        } catch (SQLException e) {
            logger.error("Error updating workflow step", e);
            throw new DatabaseException("Failed to update workflow step", e);
        }
    }

    /**
     * Updates the progress of a workflow step.
     * Uses BigDecimal for precise percentage calculations (e.g., 47.5% not 47% or 48%).
     *
     * @param stepId The ID of the workflow step
     * @param progress The progress value between 0 and 100 (BigDecimal for precision)
     * @param currentUserId The ID of the user making the update
     * @throws TicketTrackerException if validation fails or database error occurs
     */
    public void updateStepProgress(byte[] stepId, BigDecimal progress, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            if (progress.compareTo(MIN_PROGRESS) < 0 || progress.compareTo(MAX_PROGRESS) > 0) {
                throw new ValidationException("progress", "Progress must be between 0 and 100");
            }

            WorkflowStep step = getWorkflowStepById(stepId);
            BigDecimal oldProgress = step.getProgress();

            boolean updated = workflowStepDAO.updateProgress(stepId, progress);
            if (!updated) {
                throw new DatabaseException("Failed to update progress");
            }

            String description = String.format("Progress updated from %.2f%% to %.2f%%",
                    oldProgress.doubleValue(), progress.doubleValue());
            createAuditLog(step.getTicketId(), stepId, currentUserId,
                    "Progress updated", description, "progress_update");

            logger.info("Step progress updated: {} from {}% to {}%",
                    step.getStepNumber(), oldProgress.doubleValue(), progress.doubleValue());

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

            step.setStatus(newStatus);
            if ("completed".equals(newStatus)) {
                step.setCompletedAt(new java.sql.Timestamp(System.currentTimeMillis()));
                step.setProgress(new BigDecimal("100"));
            }

            workflowStepDAO.update(step);

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

    private void calculateAndSetStepLevels(WorkflowStep step) throws TicketTrackerException {
        try {
            List<WorkflowStep> existingSteps = workflowStepDAO.findByTicketId(step.getTicketId());

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

        } catch (SQLException e) {
            logger.error("Error calculating step levels", e);
            throw new DatabaseException("Failed to calculate step levels", e);
        }
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

    /**
     * Builds a description of changes between two workflow step states.
     * Uses BigDecimal comparison with tolerance to avoid false positives from floating point precision.
     */
    private String buildStepChangeDescription(WorkflowStep oldStep, WorkflowStep newStep) {
        StringBuilder changes = new StringBuilder();

        if (!oldStep.getTitle().equals(newStep.getTitle())) {
            changes.append("Title changed; ");
        }
        if (!oldStep.getStatus().equals(newStep.getStatus())) {
            changes.append(String.format("Status: %s -> %s; ",
                    oldStep.getStatus(), newStep.getStatus()));
        }
        if (!isBigDecimalEqual(oldStep.getProgress(), newStep.getProgress(), PROGRESS_COMPARISON_EPSILON)) {
            changes.append(String.format("Progress: %.0f%% -> %.0f%%; ",
                    oldStep.getProgress().doubleValue(), newStep.getProgress().doubleValue()));
        }

        return changes.length() > 0 ? changes.toString() : "Minor updates";
    }

    /**
     * Compares two BigDecimal values with a tolerance (epsilon) to handle precision issues.
     * This is important for percentage comparisons where small differences should be ignored.
     *
     * @param a First BigDecimal value
     * @param b Second BigDecimal value
     * @param epsilon Tolerance for comparison (e.g., 0.01 for percentage)
     * @return true if values are equal within the epsilon tolerance
     */
    private boolean isBigDecimalEqual(BigDecimal a, BigDecimal b, BigDecimal epsilon) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.subtract(b).abs().compareTo(epsilon) < 0;
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
}
