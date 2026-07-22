package com.tickettracker.service;

import com.tickettracker.dao.FieldDropdownOptionDAO;
import com.tickettracker.dao.ModuleFieldConfigurationDAO;
import com.tickettracker.dao.ModuleFieldConfigurationDAO.FieldConfig;
import com.tickettracker.dao.FieldDropdownOptionDAO.DropdownOption;
import com.tickettracker.exception.ForbiddenException;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.exception.ValidationException;
import com.tickettracker.exception.DatabaseException;
import com.tickettracker.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldConfigService {

    private static final Logger logger = LoggerFactory.getLogger(FieldConfigService.class);

    private final ModuleFieldConfigurationDAO fieldConfigDAO;
    private final FieldDropdownOptionDAO dropdownOptionDAO;

    public FieldConfigService() {
        this.fieldConfigDAO = new ModuleFieldConfigurationDAO();
        this.dropdownOptionDAO = new FieldDropdownOptionDAO();
    }

    public List<FieldConfig> getConfigurationsByModule(byte[] moduleId, String context)
            throws TicketTrackerException {
        try {
            return fieldConfigDAO.findByModuleId(moduleId, context != null ? context : "ticket");
        } catch (SQLException e) {
            logger.error("Error fetching field configurations", e);
            throw new DatabaseException("Failed to fetch field configurations", e);
        }
    }

    public FieldConfig createConfiguration(FieldConfig config, User currentUser)
            throws TicketTrackerException {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only Executive Officers can configure fields");
        }

        validateFieldConfig(config);

        try {
            return fieldConfigDAO.create(config);
        } catch (SQLException e) {
            logger.error("Error creating field configuration", e);
            throw new DatabaseException("Failed to create field configuration", e);
        }
    }

    public FieldConfig updateConfiguration(FieldConfig config, User currentUser)
            throws TicketTrackerException {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only Executive Officers can configure fields");
        }

        validateFieldConfig(config);

        try {
            fieldConfigDAO.update(config);
            return fieldConfigDAO.findById(config.getId());
        } catch (SQLException e) {
            logger.error("Error updating field configuration", e);
            throw new DatabaseException("Failed to update field configuration", e);
        }
    }

    public void deleteConfiguration(byte[] configId, User currentUser)
            throws TicketTrackerException {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only Executive Officers can configure fields");
        }

        try {
            fieldConfigDAO.delete(configId);
        } catch (SQLException e) {
            logger.error("Error deleting field configuration", e);
            throw new DatabaseException("Failed to delete field configuration", e);
        }
    }

    public void reorderFields(byte[] moduleId, List<byte[]> orderedIds, User currentUser)
            throws TicketTrackerException {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only Executive Officers can configure fields");
        }

        try {
            fieldConfigDAO.reorderFields(orderedIds);
        } catch (SQLException e) {
            logger.error("Error reordering fields", e);
            throw new DatabaseException("Failed to reorder fields", e);
        }
    }

    public List<DropdownOption> getDropdownOptions(byte[] fieldConfigId)
            throws TicketTrackerException {
        try {
            return dropdownOptionDAO.findByFieldConfigId(fieldConfigId);
        } catch (SQLException e) {
            logger.error("Error fetching dropdown options", e);
            throw new DatabaseException("Failed to fetch dropdown options", e);
        }
    }

    public DropdownOption createDropdownOption(DropdownOption option, User currentUser)
            throws TicketTrackerException {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only Executive Officers can configure fields");
        }

        if (option.getOptionValue() == null || option.getOptionValue().trim().isEmpty()) {
            throw new ValidationException("optionValue", "Option value is required");
        }

        try {
            return dropdownOptionDAO.create(option);
        } catch (SQLException e) {
            logger.error("Error creating dropdown option", e);
            throw new DatabaseException("Failed to create dropdown option", e);
        }
    }

    public boolean updateDropdownOption(DropdownOption option, User currentUser)
            throws TicketTrackerException {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only Executive Officers can configure fields");
        }

        try {
            return dropdownOptionDAO.update(option);
        } catch (SQLException e) {
            logger.error("Error updating dropdown option", e);
            throw new DatabaseException("Failed to update dropdown option", e);
        }
    }

    public boolean deleteDropdownOption(byte[] optionId, User currentUser)
            throws TicketTrackerException {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only Executive Officers can configure fields");
        }

        try {
            return dropdownOptionDAO.delete(optionId);
        } catch (SQLException e) {
            logger.error("Error deleting dropdown option", e);
            throw new DatabaseException("Failed to delete dropdown option", e);
        }
    }

    public boolean validateFieldValue(FieldConfig config, String value) {
        if (config.isRequired() && (value == null || value.trim().isEmpty())) {
            return false;
        }
        return true;
    }

    private void validateFieldConfig(FieldConfig config) throws ValidationException {
        ValidationException validation = new ValidationException("Field configuration validation failed");

        if (config.getFieldKey() == null || config.getFieldKey().trim().isEmpty()) {
            validation.addError("Field key is required");
        }
        if (config.getFieldType() == null || config.getFieldType().trim().isEmpty()) {
            validation.addError("Field type is required");
        }
        if (config.getLabel() == null || config.getLabel().trim().isEmpty()) {
            validation.addError("Label is required");
        }
        if (config.getModuleId() == null) {
            validation.addError("Module ID is required");
        }

        if (validation.getValidationErrors().size() > 0) {
            throw validation;
        }
    }

    public Map<String, Object> getFieldConfiguration(String entityType, byte[] currentUserId)
            throws TicketTrackerException {
        logger.info("Fetching field configuration for entity type: {}", entityType);
        Map<String, Object> config = new HashMap<>();
        config.put("entityType", entityType);
        config.put("fields", new ArrayList<>());
        return config;
    }

    public Map<String, Object> updateFieldConfiguration(String entityType,
                                                         Map<String, Object> configuration,
                                                         byte[] currentUserId)
            throws TicketTrackerException {
        logger.info("Updating field configuration for entity type: {}", entityType);
        return configuration;
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
