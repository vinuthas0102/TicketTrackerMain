package com.tickettracker.service;

import com.tickettracker.dao.FileReferenceTemplateDAO;
import com.tickettracker.dao.WorkflowStepFileReferenceDAO;
import com.tickettracker.exception.*;
import com.tickettracker.model.FileReferenceTemplate;
import com.tickettracker.model.WorkflowStepFileReference;
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

    public FileReferenceService() {
        this.templateDAO = new FileReferenceTemplateDAO();
        this.referenceDAO = new WorkflowStepFileReferenceDAO();
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
            logger.error("Error fetching template by name: {}", templateName, e);
            throw new DatabaseException("Failed to fetch file reference template by name", e);
        }
    }

    public Map<String, Object> validateTemplateJSON(String jsonContent) {
        Map<String, Object> result = new HashMap<>();

        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            result.put("valid", false);
            result.put("error", "JSON content cannot be empty");
            return result;
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> jsonMap = mapper.readValue(jsonContent, Map.class);

            if (!jsonMap.containsKey("fileReferences")) {
                result.put("valid", false);
                result.put("error", "JSON must contain 'fileReferences' array at root level");
                return result;
            }

            Object fileReferencesObj = jsonMap.get("fileReferences");
            if (!(fileReferencesObj instanceof List)) {
                result.put("valid", false);
                result.put("error", "'fileReferences' must be an array");
                return result;
            }

            List<?> fileReferences = (List<?>) fileReferencesObj;
            java.util.Set<String> referenceNames = new java.util.HashSet<>();
            String[] allowedFileTypes = {"pdf", "docx", "xlsx", "doc", "xls", "jpg", "jpeg", "png", "txt", "csv"};
            java.util.Set<String> allowedFileTypesSet = new java.util.HashSet<>(java.util.Arrays.asList(allowedFileTypes));
            int maxNestingDepth = 5;

            for (int i = 0; i < fileReferences.size(); i++) {
                Object refObj = fileReferences.get(i);
                if (!(refObj instanceof Map)) {
                    result.put("valid", false);
                    result.put("error", "File reference at index " + i + " must be an object");
                    return result;
                }

                Map<String, Object> reference = (Map<String, Object>) refObj;

                if (!reference.containsKey("name") || !(reference.get("name") instanceof String)) {
                    result.put("valid", false);
                    result.put("error", "File reference at index " + i + " must have a 'name' field of type string");
                    return result;
                }

                String name = (String) reference.get("name");
                if (name.trim().isEmpty()) {
                    result.put("valid", false);
                    result.put("error", "File reference name at index " + i + " cannot be empty");
                    return result;
                }

                if (name.contains("<") || name.contains(">") || name.contains("script")) {
                    result.put("valid", false);
                    result.put("error", "File reference name at index " + i + " contains unsafe characters");
                    return result;
                }

                if (referenceNames.contains(name)) {
                    result.put("valid", false);
                    result.put("error", "Duplicate file reference name '" + name + "' found at index " + i);
                    return result;
                }
                referenceNames.add(name);

                if (reference.containsKey("isMandatory") && !(reference.get("isMandatory") instanceof Boolean)) {
                    result.put("valid", false);
                    result.put("error", "File reference 'isMandatory' at index " + i + " must be boolean");
                    return result;
                }

                if (reference.containsKey("description")) {
                    Object descObj = reference.get("description");
                    if (!(descObj instanceof String)) {
                        result.put("valid", false);
                        result.put("error", "File reference 'description' at index " + i + " must be a string");
                        return result;
                    }
                    String desc = (String) descObj;
                    if (desc.contains("<script") || desc.contains("javascript:")) {
                        result.put("valid", false);
                        result.put("error", "File reference description at index " + i + " contains unsafe content");
                        return result;
                    }
                }

                if (reference.containsKey("fileTypes")) {
                    Object fileTypesObj = reference.get("fileTypes");
                    if (!(fileTypesObj instanceof List)) {
                        result.put("valid", false);
                        result.put("error", "File reference 'fileTypes' at index " + i + " must be an array");
                        return result;
                    }
                    List<?> fileTypes = (List<?>) fileTypesObj;
                    for (Object ftObj : fileTypes) {
                        if (!(ftObj instanceof String)) {
                            result.put("valid", false);
                            result.put("error", "All file types in reference at index " + i + " must be strings");
                            return result;
                        }
                        String fileType = ((String) ftObj).toLowerCase().replace(".", "");
                        if (!allowedFileTypesSet.contains(fileType)) {
                            result.put("valid", false);
                            result.put("error", "Unsupported file type '" + fileType + "' in reference at index " + i);
                            return result;
                        }
                    }
                }

                if (reference.containsKey("maxSize")) {
                    Object maxSizeObj = reference.get("maxSize");
                    if (!(maxSizeObj instanceof Number)) {
                        result.put("valid", false);
                        result.put("error", "File reference 'maxSize' at index " + i + " must be a number");
                        return result;
                    }
                    long maxSize = ((Number) maxSizeObj).longValue();
                    if (maxSize <= 0 || maxSize > 10485760) {
                        result.put("valid", false);
                        result.put("error", "File reference 'maxSize' at index " + i + " must be between 1 and 10485760 bytes (10MB)");
                        return result;
                    }
                }

                if (reference.containsKey("allowMultiple") && !(reference.get("allowMultiple") instanceof Boolean)) {
                    result.put("valid", false);
                    result.put("error", "File reference 'allowMultiple' at index " + i + " must be boolean");
                    return result;
                }
            }

            int depth = calculateJSONDepth(jsonMap, 0);
            if (depth > maxNestingDepth) {
                result.put("valid", false);
                result.put("error", "JSON nesting depth exceeds maximum allowed depth of " + maxNestingDepth);
                return result;
            }

            result.put("valid", true);
            return result;

        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            result.put("valid", false);
            result.put("error", "Invalid JSON syntax at line " + e.getLocation().getLineNr() +
                              ", column " + e.getLocation().getColumnNr() + ": " + e.getOriginalMessage());
            return result;
        } catch (Exception e) {
            logger.error("Error validating template JSON", e);
            result.put("valid", false);
            result.put("error", "JSON validation failed: " + e.getMessage());
            return result;
        }
    }

    private int calculateJSONDepth(Object obj, int currentDepth) {
        if (currentDepth > 10) {
            return currentDepth;
        }

        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            int maxDepth = currentDepth;
            for (Object value : map.values()) {
                int depth = calculateJSONDepth(value, currentDepth + 1);
                maxDepth = Math.max(maxDepth, depth);
            }
            return maxDepth;
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            int maxDepth = currentDepth;
            for (Object item : list) {
                int depth = calculateJSONDepth(item, currentDepth + 1);
                maxDepth = Math.max(maxDepth, depth);
            }
            return maxDepth;
        }
        return currentDepth;
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

    public FileReferenceTemplate createTemplate(FileReferenceTemplate template) throws TicketTrackerException {
        return createTemplate(template, template.getUploadedBy());
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

    public FileReferenceTemplate updateTemplate(FileReferenceTemplate template) throws TicketTrackerException {
        return updateTemplate(template, template.getUploadedBy());
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

    public boolean deleteTemplate(byte[] templateId) throws TicketTrackerException {
        try {
            boolean deleted = templateDAO.delete(templateId);
            if (!deleted) {
                throw new ResourceNotFoundException("File reference template not found");
            }
            return true;
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

    public WorkflowStepFileReference updateStepFileReference(byte[] referenceId, Map<String, Object> updates, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            WorkflowStepFileReference reference = referenceDAO.findById(referenceId);
            if (reference == null) {
                throw new ResourceNotFoundException("File reference not found");
            }

            if (updates.containsKey("documentId")) {
                String documentIdStr = (String) updates.get("documentId");
                byte[] documentId = documentIdStr != null ? hexToBytes(documentIdStr) : null;
                reference.setDocumentId(documentId);
            }

            if (updates.containsKey("uploadedBy")) {
                String uploadedByStr = (String) updates.get("uploadedBy");
                byte[] uploadedBy = uploadedByStr != null ? hexToBytes(uploadedByStr) : null;
                reference.setUploadedBy(uploadedBy);
            }

            if (updates.containsKey("uploadedAt") && updates.get("uploadedAt") != null) {
                Object uploadedAtObj = updates.get("uploadedAt");
                if (uploadedAtObj instanceof java.util.Date) {
                    reference.setUploadedAt(new java.sql.Timestamp(((java.util.Date) uploadedAtObj).getTime()));
                } else if (uploadedAtObj instanceof String) {
                    reference.setUploadedAt(java.sql.Timestamp.valueOf((String) uploadedAtObj));
                }
            } else {
                reference.setUploadedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            }

            return referenceDAO.update(reference);
        } catch (SQLException e) {
            logger.error("Error updating step file reference", e);
            throw new DatabaseException("Failed to update workflow step file reference", e);
        }
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return null;
        }
        String cleanHex = hex.replace("-", "");
        int len = cleanHex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(cleanHex.charAt(i), 16) << 4)
                    + Character.digit(cleanHex.charAt(i + 1), 16));
        }
        return data;
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

    public boolean checkMandatoryFileReferencesComplete(byte[] stepId) throws TicketTrackerException {
        try {
            List<WorkflowStepFileReference> pendingMandatory = referenceDAO.findMandatoryPending(stepId);
            boolean isComplete = pendingMandatory.isEmpty();

            if (!isComplete) {
                logger.info("Step {} has {} pending mandatory file references",
                           bytesToHex(stepId), pendingMandatory.size());
            } else {
                logger.info("Step {} has all mandatory file references completed", bytesToHex(stepId));
            }

            return isComplete;
        } catch (SQLException e) {
            logger.error("Error checking mandatory file references completion", e);
            throw new DatabaseException("Failed to check mandatory file references", e);
        }
    }

    public List<Map<String, Object>> getFileReferences(byte[] stepId, byte[] userId)
            throws TicketTrackerException {
        try {
            String sql = "SELECT fr.id, fr.step_id, fr.template_id, fr.reference_name, fr.is_mandatory, " +
                    "fr.document_id, fr.uploaded_by, fr.uploaded_at, fr.created_at, fr.updated_at, " +
                    "d.name as document_name, d.size as document_size, d.type as document_type, " +
                    "d.storage_path, d.url as document_url " +
                    "FROM workflow_step_file_references fr " +
                    "LEFT JOIN documents d ON fr.document_id = d.id " +
                    "WHERE fr.step_id = ? " +
                    "ORDER BY fr.is_mandatory DESC, fr.reference_name";

            java.sql.Connection conn = null;
            java.sql.PreparedStatement stmt = null;
            java.sql.ResultSet rs = null;

            try {
                conn = referenceDAO.getConnection();
                stmt = conn.prepareStatement(sql);
                stmt.setBytes(1, stepId);
                rs = stmt.executeQuery();

                List<Map<String, Object>> results = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> reference = new HashMap<>();

                    reference.put("id", bytesToHex(rs.getBytes("id")));
                    reference.put("stepId", bytesToHex(rs.getBytes("step_id")));

                    byte[] templateId = rs.getBytes("template_id");
                    reference.put("templateId", templateId != null ? bytesToHex(templateId) : null);

                    reference.put("referenceName", rs.getString("reference_name"));
                    reference.put("isMandatory", rs.getInt("is_mandatory") == 1);

                    byte[] documentId = rs.getBytes("document_id");
                    reference.put("documentId", documentId != null ? bytesToHex(documentId) : null);
                    reference.put("documentName", rs.getString("document_name"));
                    reference.put("documentSize", rs.getLong("document_size"));
                    reference.put("documentType", rs.getString("document_type"));
                    reference.put("storagePath", rs.getString("storage_path"));
                    reference.put("documentUrl", rs.getString("document_url"));

                    byte[] uploadedBy = rs.getBytes("uploaded_by");
                    reference.put("uploadedBy", uploadedBy != null ? bytesToHex(uploadedBy) : null);

                    java.sql.Timestamp uploadedAt = rs.getTimestamp("uploaded_at");
                    reference.put("uploadedAt", uploadedAt != null ? uploadedAt.toString() : null);

                    java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                    reference.put("createdAt", createdAt != null ? createdAt.toString() : null);

                    java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
                    reference.put("updatedAt", updatedAt != null ? updatedAt.toString() : null);

                    results.add(reference);
                }

                logger.info("Fetched {} file references for step {}", results.size(), bytesToHex(stepId));
                return results;

            } finally {
                if (rs != null) try { rs.close(); } catch (SQLException e) { }
                if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
                if (conn != null) try { conn.close(); } catch (SQLException e) { }
            }
        } catch (SQLException e) {
            logger.error("Error fetching file references for step", e);
            throw new DatabaseException("Failed to fetch file references", e);
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
