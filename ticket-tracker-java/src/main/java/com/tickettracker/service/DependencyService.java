package com.tickettracker.service;

import com.tickettracker.dao.WorkflowStepDAO;
import com.tickettracker.dao.WorkflowStepDependencyDAO;
import com.tickettracker.exception.*;
import com.tickettracker.model.WorkflowStep;
import com.tickettracker.model.WorkflowStepDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;

public class DependencyService {

    private static final Logger logger = LoggerFactory.getLogger(DependencyService.class);
    private final WorkflowStepDependencyDAO dependencyDAO;
    private final WorkflowStepDAO workflowStepDAO;

    public DependencyService() {
        this.dependencyDAO = new WorkflowStepDependencyDAO();
        this.workflowStepDAO = new WorkflowStepDAO();
    }

    public WorkflowStepDependency getDependencyById(byte[] dependencyId)
            throws TicketTrackerException {
        try {
            WorkflowStepDependency dependency = dependencyDAO.findById(dependencyId);
            if (dependency == null) {
                throw new ResourceNotFoundException("Dependency", bytesToHex(dependencyId));
            }
            return dependency;
        } catch (SQLException e) {
            logger.error("Error fetching dependency by ID", e);
            throw new DatabaseException("Failed to fetch dependency", e);
        }
    }

    public List<WorkflowStepDependency> getDependenciesByStepId(byte[] stepId)
            throws TicketTrackerException {
        try {
            return dependencyDAO.findByStepId(stepId);
        } catch (SQLException e) {
            logger.error("Error fetching dependencies by step ID", e);
            throw new DatabaseException("Failed to fetch dependencies", e);
        }
    }

    public List<WorkflowStepDependency> getDependentSteps(byte[] stepId)
            throws TicketTrackerException {
        try {
            return dependencyDAO.findDependentSteps(stepId);
        } catch (SQLException e) {
            logger.error("Error fetching dependent steps", e);
            throw new DatabaseException("Failed to fetch dependent steps", e);
        }
    }

    public WorkflowStepDependency createDependency(WorkflowStepDependency dependency,
                                                    byte[] currentUserId)
            throws TicketTrackerException {
        try {
            validateDependency(dependency);

            WorkflowStep step = workflowStepDAO.findById(dependency.getStepId());
            if (step == null) {
                throw new ResourceNotFoundException("Workflow Step",
                    bytesToHex(dependency.getStepId()));
            }

            WorkflowStep dependsOnStep = workflowStepDAO.findById(dependency.getDependsOnStepId());
            if (dependsOnStep == null) {
                throw new ResourceNotFoundException("Dependency Step",
                    bytesToHex(dependency.getDependsOnStepId()));
            }

            if (java.util.Arrays.equals(dependency.getStepId(), dependency.getDependsOnStepId())) {
                throw new ValidationException("dependsOnStepId",
                    "A step cannot depend on itself");
            }

            boolean exists = dependencyDAO.dependencyExists(
                dependency.getStepId(),
                dependency.getDependsOnStepId()
            );
            if (exists) {
                throw new ValidationException("dependency",
                    "This dependency already exists");
            }

            if (wouldCreateCycle(dependency.getStepId(), dependency.getDependsOnStepId())) {
                throw new ValidationException("dependsOnStepId",
                    "This dependency would create a circular dependency");
            }

            dependency.setCreatedBy(currentUserId);
            dependency.setActive(true);

            WorkflowStepDependency createdDependency = dependencyDAO.create(dependency);

            logger.info("Dependency created: step {} depends on step {} by user: {}",
                bytesToHex(dependency.getStepId()),
                bytesToHex(dependency.getDependsOnStepId()),
                bytesToHex(currentUserId));

            return createdDependency;
        } catch (SQLException e) {
            logger.error("Error creating dependency", e);
            throw new DatabaseException("Failed to create dependency", e);
        }
    }

    public void deleteDependency(byte[] dependencyId, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            WorkflowStepDependency dependency = getDependencyById(dependencyId);

            boolean deleted = dependencyDAO.delete(dependencyId);
            if (!deleted) {
                throw new DatabaseException("Failed to delete dependency");
            }

            logger.info("Dependency deleted: {} by user: {}",
                bytesToHex(dependencyId), bytesToHex(currentUserId));

        } catch (SQLException e) {
            logger.error("Error deleting dependency", e);
            throw new DatabaseException("Failed to delete dependency", e);
        }
    }

    public void deleteAllDependenciesForStep(byte[] stepId, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            int deleted = dependencyDAO.deleteByStepId(stepId);

            logger.info("Deleted {} dependencies for step {} by user: {}",
                deleted, bytesToHex(stepId), bytesToHex(currentUserId));

        } catch (SQLException e) {
            logger.error("Error deleting dependencies for step", e);
            throw new DatabaseException("Failed to delete dependencies", e);
        }
    }

    public void setDependencyActive(byte[] dependencyId, boolean active, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            WorkflowStepDependency dependency = getDependencyById(dependencyId);

            boolean updated = dependencyDAO.setActive(dependencyId, active);
            if (!updated) {
                throw new DatabaseException("Failed to update dependency status");
            }

            logger.info("Dependency {} set to active: {} by user: {}",
                bytesToHex(dependencyId), active, bytesToHex(currentUserId));

        } catch (SQLException e) {
            logger.error("Error setting dependency active status", e);
            throw new DatabaseException("Failed to update dependency status", e);
        }
    }

    public boolean areDependenciesSatisfied(byte[] stepId) throws TicketTrackerException {
        try {
            List<WorkflowStepDependency> dependencies = dependencyDAO.findByStepId(stepId);
            if (dependencies.isEmpty()) {
                return true;
            }

            for (WorkflowStepDependency dependency : dependencies) {
                WorkflowStep dependsOnStep = workflowStepDAO.findById(
                    dependency.getDependsOnStepId()
                );

                if (dependsOnStep == null) {
                    continue;
                }

                if (!"completed".equalsIgnoreCase(dependsOnStep.getStatus())) {
                    return false;
                }
            }

            return true;
        } catch (SQLException e) {
            logger.error("Error checking dependencies", e);
            throw new DatabaseException("Failed to check dependencies", e);
        }
    }

    private boolean wouldCreateCycle(byte[] stepId, byte[] dependsOnStepId)
            throws TicketTrackerException {
        try {
            Set<String> visited = new HashSet<>();
            Queue<byte[]> queue = new LinkedList<>();
            queue.add(dependsOnStepId);

            while (!queue.isEmpty()) {
                byte[] currentStep = queue.poll();
                String currentStepHex = bytesToHex(currentStep);

                if (visited.contains(currentStepHex)) {
                    continue;
                }
                visited.add(currentStepHex);

                if (java.util.Arrays.equals(currentStep, stepId)) {
                    return true;
                }

                List<WorkflowStepDependency> dependencies = dependencyDAO.findByStepId(currentStep);
                for (WorkflowStepDependency dep : dependencies) {
                    queue.add(dep.getDependsOnStepId());
                }
            }

            return false;
        } catch (SQLException e) {
            logger.error("Error checking for circular dependencies", e);
            throw new DatabaseException("Failed to check for circular dependencies", e);
        }
    }

    private void validateDependency(WorkflowStepDependency dependency) throws ValidationException {
        ValidationException validation = new ValidationException("Dependency validation failed");

        if (dependency.getStepId() == null) {
            validation.addError("Step ID is required");
        }

        if (dependency.getDependsOnStepId() == null) {
            validation.addError("Depends on step ID is required");
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
