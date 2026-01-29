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
