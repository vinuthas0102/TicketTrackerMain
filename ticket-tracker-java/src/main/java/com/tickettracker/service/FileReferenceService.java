package com.tickettracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.dao.FileReferenceTemplateDAO;
import com.tickettracker.dao.WorkflowStepFileReferenceDAO;
import com.tickettracker.exception.*;
import com.tickettracker.model.FileReferenceTemplate;
import com.tickettracker.model.WorkflowStepFileReference;
import com.tickettracker.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileReferenceService {

    private static final Logger logger = LoggerFactory.getLogger(FileReferenceService.class);
    private final FileReferenceTemplateDAO templateDAO;
    private final WorkflowStepFileReferenceDAO referenceDAO;
    private final ObjectMapper objectMapper;

    public FileReferenceService() {
        this.templateDAO = new FileReferenceTemplateDAO();
        this.referenceDAO = new WorkflowStepFileReferenceDAO();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    public List<FileReferenceTemplate> getAllTemplates() throws TicketTrackerException {
        try {
            return templateDAO.findAll();
        } catch (SQLException e) {
            logger.error("Error fetching all templates", e);
            throw new DatabaseException("Failed to fetch file reference templates", e);
        }
    }

    public List<FileReferenceTemplate> getActiveTemplates() throws TicketTrackerException {
        try {
            return templateDAO.findActiveTemplates();
        } catch (SQLException e) {
            logger.error("Error fetching active templates", e);
            throw new DatabaseException("Failed to fetch active file reference templates", e);
        }
    }

    public FileReferenceTemplate getTemplateById(byte[] templateId) throws TicketTrackerException {
        try {
            FileReferenceTemplate template = templateDAO.findById(templateId);
            if (template == null) {
                throw new ResourceNotFoundException("File reference template not found");
            }
            return template;
        } catch (SQLException e) {
            logger.error("Error fetching template by ID", e);
            throw new DatabaseException("Failed to fetch file reference template", e);
        }
    }

    public FileReferenceTemplate getTemplateByName(String templateName) throws TicketTrackerException {
        try {
            return templateDAO.findByName(templateName);
        } catch (SQLException e) {
            logger.error("Error fetching template by name", e);
            throw new DatabaseException("Failed to fetch file reference template by name", e);
        }
    }

    public Map<String, Object> validateTemplateJSON(String jsonContent) {
        Map<String, Object> result = new HashMap<>();

        try {
            JsonNode rootNode = objectMapper.readTree(jsonContent);

            if (!rootNode.has("fileReferences")) {
                result.put("valid", false);
                result.put("error", "JSON content must have 'fileReferences' field");
                return result;
            }

            JsonNode fileReferencesNode = rootNode.get("fileReferences");
            if (!fileReferencesNode.isArray()) {
                result.put("valid", false);
                result.put("error", "'fileReferences' must be an array");
                return result;
            }

            if (fileReferencesNode.size() == 0) {
                result.put("valid", false);
                result.put("error", "'fileReferences' array must not be empty");
                return result;
            }

            for (int i = 0; i < fileReferencesNode.size(); i++) {
                JsonNode refNode = fileReferencesNode.get(i);
                if (!refNode.isTextual() || refNode.asText().trim().isEmpty()) {
                    result.put("valid", false);
                    result.put("error", "File reference at index " + i + " must be a non-empty string");
                    return result;
                }
            }

            if (rootNode.has("mandatoryFlags")) {
                JsonNode mandatoryFlagsNode = rootNode.get("mandatoryFlags");
                if (!mandatoryFlagsNode.isArray()) {
                    result.put("valid", false);
                    result.put("error", "'mandatoryFlags' must be an array");
                    return result;
                }

                if (mandatoryFlagsNode.size() != fileReferencesNode.size()) {
                    result.put("valid", false);
                    result.put("error", "'mandatoryFlags' array length must match 'fileReferences' array length");
                    return result;
                }

                for (int i = 0; i < mandatoryFlagsNode.size(); i++) {
                    JsonNode flagNode = mandatoryFlagsNode.get(i);
                    if (!flagNode.isBoolean()) {
                        result.put("valid", false);
                        result.put("error", "Mandatory flag at index " + i + " must be a boolean");
                        return result;
                    }
                }
            }

            result.put("valid", true);
            return result;

        } catch (Exception e) {
            logger.error("Error validating template JSON", e);
            result.put("valid", false);
            result.put("error", "Invalid JSON format: " + e.getMessage());
            return result;
        }
    }

    public FileReferenceTemplate createTemplate(FileReferenceTemplate template, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            template.setUploadedBy(currentUserId);
            return templateDAO.create(template);
        } catch (SQLException e) {
            logger.error("Error creating template", e);
            throw new DatabaseException("Failed to create file reference template", e);
        }
    }

    public FileReferenceTemplate createTemplate(FileReferenceTemplate template)
            throws TicketTrackerException {
        try {
            return templateDAO.create(template);
        } catch (SQLException e) {
            logger.error("Error creating template", e);
            throw new DatabaseException("Failed to create file reference template", e);
        }
    }

    public FileReferenceTemplate updateTemplate(FileReferenceTemplate template, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            FileReferenceTemplate existing = templateDAO.findById(template.getId());
            if (existing == null) {
                throw new ResourceNotFoundException("File reference template not found");
            }
            return templateDAO.update(template);
        } catch (SQLException e) {
            logger.error("Error updating template", e);
            throw new DatabaseException("Failed to update file reference template", e);
        }
    }

    public FileReferenceTemplate updateTemplate(FileReferenceTemplate template)
            throws TicketTrackerException {
        try {
            FileReferenceTemplate existing = templateDAO.findById(template.getId());
            if (existing == null) {
                throw new ResourceNotFoundException("File reference template not found");
            }
            return templateDAO.update(template);
        } catch (SQLException e) {
            logger.error("Error updating template", e);
            throw new DatabaseException("Failed to update file reference template", e);
        }
    }

    public boolean deleteTemplate(byte[] templateId) throws TicketTrackerException {
        try {
            return templateDAO.delete(templateId);
        } catch (SQLException e) {
            logger.error("Error deleting template", e);
            throw new DatabaseException("Failed to delete file reference template", e);
        }
    }

    public void deleteTemplate(byte[] templateId, byte[] currentUserId) throws TicketTrackerException {
        try {
            boolean deleted = templateDAO.delete(templateId);
            if (!deleted) {
                throw new ResourceNotFoundException("File reference template not found");
            }
        } catch (SQLException e) {
            logger.error("Error deleting template", e);
            throw new DatabaseException("Failed to delete file reference template", e);
        }
    }

    public List<WorkflowStepFileReference> getStepFileReferences(byte[] stepId)
            throws TicketTrackerException {
        try {
            return referenceDAO.findByStepId(stepId);
        } catch (SQLException e) {
            logger.error("Error fetching step file references", e);
            throw new DatabaseException("Failed to fetch workflow step file references", e);
        }
    }

    public WorkflowStepFileReference createStepReference(WorkflowStepFileReference reference, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            return referenceDAO.create(reference);
        } catch (SQLException e) {
            logger.error("Error creating step file reference", e);
            throw new DatabaseException("Failed to create workflow step file reference", e);
        }
    }

    public void linkDocumentToReference(byte[] referenceId, byte[] documentId, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            boolean updated = referenceDAO.updateDocumentLink(referenceId, documentId, currentUserId);
            if (!updated) {
                throw new ResourceNotFoundException("File reference not found");
            }
        } catch (SQLException e) {
            logger.error("Error linking document to reference", e);
            throw new DatabaseException("Failed to link document to file reference", e);
        }
    }

    public void deleteStepReference(byte[] referenceId, byte[] currentUserId) throws TicketTrackerException {
        try {
            boolean deleted = referenceDAO.delete(referenceId);
            if (!deleted) {
                throw new ResourceNotFoundException("File reference not found");
            }
        } catch (SQLException e) {
            logger.error("Error deleting step file reference", e);
            throw new DatabaseException("Failed to delete workflow step file reference", e);
        }
    }

    public List<WorkflowStepFileReference> getPendingMandatoryReferences(byte[] stepId)
            throws TicketTrackerException {
        try {
            return referenceDAO.findMandatoryPending(stepId);
        } catch (SQLException e) {
            logger.error("Error fetching pending mandatory references", e);
            throw new DatabaseException("Failed to fetch pending mandatory file references", e);
        }
    }

    public List<Map<String, Object>> getFileReferences(byte[] stepId, byte[] userId)
            throws TicketTrackerException {
        try {
            return referenceDAO.findFileReferencesWithDocumentDetails(stepId);
        } catch (SQLException e) {
            logger.error("Error fetching file references for step", e);
            throw new DatabaseException("Failed to fetch file references", e);
        }
    }
}
