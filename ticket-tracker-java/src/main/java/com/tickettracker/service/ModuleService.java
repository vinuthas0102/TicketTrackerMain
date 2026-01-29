package com.tickettracker.service;

import com.tickettracker.dao.ModuleDAO;
import com.tickettracker.exception.*;
import com.tickettracker.model.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class ModuleService {

    private static final Logger logger = LoggerFactory.getLogger(ModuleService.class);
    private final ModuleDAO moduleDAO;

    public ModuleService() {
        this.moduleDAO = new ModuleDAO();
    }

    public Module getModuleById(byte[] moduleId) throws TicketTrackerException {
        try {
            Module module = moduleDAO.findById(moduleId);
            if (module == null) {
                throw new ResourceNotFoundException("Module", bytesToHex(moduleId));
            }
            return module;
        } catch (SQLException e) {
            logger.error("Error fetching module by ID", e);
            throw new DatabaseException("Failed to fetch module", e);
        }
    }

    public Module getModuleBySchemaId(String schemaId) throws TicketTrackerException {
        try {
            Module module = moduleDAO.findBySchemaId(schemaId);
            if (module == null) {
                throw new ResourceNotFoundException("Module", schemaId);
            }
            return module;
        } catch (SQLException e) {
            logger.error("Error fetching module by schema ID", e);
            throw new DatabaseException("Failed to fetch module", e);
        }
    }

    public List<Module> getAllModules() throws TicketTrackerException {
        try {
            return moduleDAO.findAll();
        } catch (SQLException e) {
            logger.error("Error fetching all modules", e);
            throw new DatabaseException("Failed to fetch modules", e);
        }
    }

    public List<Module> getActiveModules() throws TicketTrackerException {
        try {
            return moduleDAO.findAllActive();
        } catch (SQLException e) {
            logger.error("Error fetching active modules", e);
            throw new DatabaseException("Failed to fetch active modules", e);
        }
    }

    public Module createModule(Module module) throws TicketTrackerException {
        try {
            validateModule(module);

            Module existingModule = moduleDAO.findBySchemaId(module.getSchemaId());
            if (existingModule != null) {
                throw new ValidationException("schemaId", "Schema ID already exists");
            }

            Module createdModule = moduleDAO.create(module);

            logger.info("Module created: {}", module.getName());

            return createdModule;
        } catch (SQLException e) {
            logger.error("Error creating module", e);
            throw new DatabaseException("Failed to create module", e);
        }
    }

    public Module updateModule(Module module) throws TicketTrackerException {
        try {
            Module existingModule = getModuleById(module.getId());

            validateModule(module);

            if (!existingModule.getSchemaId().equals(module.getSchemaId())) {
                Module schemaCheck = moduleDAO.findBySchemaId(module.getSchemaId());
                if (schemaCheck != null && !java.util.Arrays.equals(schemaCheck.getId(), module.getId())) {
                    throw new ValidationException("schemaId", "Schema ID already in use by another module");
                }
            }

            Module updatedModule = moduleDAO.update(module);

            logger.info("Module updated: {}", module.getName());

            return updatedModule;
        } catch (SQLException e) {
            logger.error("Error updating module", e);
            throw new DatabaseException("Failed to update module", e);
        }
    }

    public void deleteModule(byte[] moduleId) throws TicketTrackerException {
        try {
            Module module = getModuleById(moduleId);

            boolean deleted = moduleDAO.delete(moduleId);
            if (!deleted) {
                throw new DatabaseException("Failed to delete module");
            }

            logger.info("Module deleted: {}", module.getName());

        } catch (SQLException e) {
            logger.error("Error deleting module", e);
            throw new DatabaseException("Failed to delete module", e);
        }
    }

    public void setModuleActive(byte[] moduleId, boolean active) throws TicketTrackerException {
        try {
            Module module = getModuleById(moduleId);

            boolean updated = moduleDAO.setActive(moduleId, active);
            if (!updated) {
                throw new DatabaseException("Failed to update module status");
            }

            logger.info("Module {} set to active: {}", module.getName(), active);

        } catch (SQLException e) {
            logger.error("Error setting module active status", e);
            throw new DatabaseException("Failed to update module status", e);
        }
    }

    private void validateModule(Module module) throws ValidationException {
        ValidationException validation = new ValidationException("Module validation failed");

        if (module.getName() == null || module.getName().trim().isEmpty()) {
            validation.addError("Module name is required");
        }

        if (module.getSchemaId() == null || module.getSchemaId().trim().isEmpty()) {
            validation.addError("Schema ID is required");
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
