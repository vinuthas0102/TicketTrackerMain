package com.tickettracker.service;

import com.tickettracker.dao.DocumentDAO;
import com.tickettracker.dao.TicketDAO;
import com.tickettracker.dao.WorkflowStepDAO;
import com.tickettracker.exception.*;
import com.tickettracker.model.Document;
import com.tickettracker.model.WorkflowStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);
    private final DocumentDAO documentDAO;
    private final TicketDAO ticketDAO;
    private final WorkflowStepDAO workflowStepDAO;

    public DocumentService() {
        this.documentDAO = new DocumentDAO();
        this.ticketDAO = new TicketDAO();
        this.workflowStepDAO = new WorkflowStepDAO();
    }

    public Document getDocumentById(byte[] documentId) throws TicketTrackerException {
        try {
            Document document = documentDAO.findById(documentId);
            if (document == null) {
                throw new ResourceNotFoundException("Document", bytesToHex(documentId));
            }
            return document;
        } catch (SQLException e) {
            logger.error("Error fetching document by ID", e);
            throw new DatabaseException("Failed to fetch document", e);
        }
    }

    public List<Document> getDocumentsByTicketId(byte[] ticketId) throws TicketTrackerException {
        try {
            return documentDAO.findByTicketId(ticketId);
        } catch (SQLException e) {
            logger.error("Error fetching documents by ticket ID", e);
            throw new DatabaseException("Failed to fetch documents", e);
        }
    }

    public List<Document> getDocumentsByStepId(byte[] stepId) throws TicketTrackerException {
        try {
            return documentDAO.findByStepId(stepId);
        } catch (SQLException e) {
            logger.error("Error fetching documents by step ID", e);
            throw new DatabaseException("Failed to fetch documents", e);
        }
    }

    public List<Document> getDocumentsByUserId(byte[] userId) throws TicketTrackerException {
        try {
            return documentDAO.findByUploadedBy(userId);
        } catch (SQLException e) {
            logger.error("Error fetching documents by user ID", e);
            throw new DatabaseException("Failed to fetch documents", e);
        }
    }

    public List<Document> getMandatoryDocumentsByStepId(byte[] stepId) throws TicketTrackerException {
        try {
            return documentDAO.findMandatoryByStepId(stepId);
        } catch (SQLException e) {
            logger.error("Error fetching mandatory documents by step ID", e);
            throw new DatabaseException("Failed to fetch mandatory documents", e);
        }
    }

    public Document createDocument(Document document, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            validateDocument(document);

            if (document.getTicketId() != null) {
                if (ticketDAO.findById(document.getTicketId()) == null) {
                    throw new ResourceNotFoundException("Ticket", bytesToHex(document.getTicketId()));
                }
            }

            if (document.getStepId() != null) {
                WorkflowStep step = workflowStepDAO.findById(document.getStepId());
                if (step == null) {
                    throw new ResourceNotFoundException("Workflow Step", bytesToHex(document.getStepId()));
                }
                if ("COMPLETED".equals(step.getStatus())) {
                    throw new ForbiddenException("Cannot add files to a completed workflow step");
                }
            }

            document.setUploadedBy(currentUserId);

            Document createdDocument = documentDAO.create(document);

            logger.info("Document created: {} by user: {}",
                document.getName(), bytesToHex(currentUserId));

            return createdDocument;
        } catch (SQLException e) {
            logger.error("Error creating document", e);
            throw new DatabaseException("Failed to create document", e);
        }
    }

    public Document updateDocument(Document document, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            Document existingDocument = getDocumentById(document.getId());

            validateDocument(document);

            Document updatedDocument = documentDAO.update(document);

            logger.info("Document updated: {} by user: {}",
                document.getName(), bytesToHex(currentUserId));

            return updatedDocument;
        } catch (SQLException e) {
            logger.error("Error updating document", e);
            throw new DatabaseException("Failed to update document", e);
        }
    }

    public void deleteDocument(byte[] documentId, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            Document document = getDocumentById(documentId);

            if (document.getStepId() != null) {
                WorkflowStep step = workflowStepDAO.findById(document.getStepId());
                if (step != null && "COMPLETED".equals(step.getStatus())) {
                    throw new ForbiddenException("Cannot delete files from a completed workflow step");
                }
            }

            boolean deleted = documentDAO.delete(documentId);
            if (!deleted) {
                throw new DatabaseException("Failed to delete document");
            }

            logger.info("Document deleted: {} by user: {}",
                document.getName(), bytesToHex(currentUserId));

        } catch (SQLException e) {
            logger.error("Error deleting document", e);
            throw new DatabaseException("Failed to delete document", e);
        }
    }

    public boolean checkMandatoryDocumentsComplete(byte[] stepId) throws TicketTrackerException {
        try {
            List<Document> mandatoryDocs = documentDAO.findMandatoryByStepId(stepId);
            return !mandatoryDocs.isEmpty();
        } catch (SQLException e) {
            logger.error("Error checking mandatory documents", e);
            throw new DatabaseException("Failed to check mandatory documents", e);
        }
    }

    public List<Document> getTicketCompletionCertificates(byte[] ticketId) throws TicketTrackerException {
        try {
            return documentDAO.findByTicketId(ticketId).stream()
                    .filter(Document::isCompletionCertificate)
                    .collect(java.util.stream.Collectors.toList());
        } catch (SQLException e) {
            logger.error("Error fetching completion certificates", e);
            throw new DatabaseException("Failed to fetch completion certificates", e);
        }
    }

    public boolean hasTicketCompletionCertificate(byte[] ticketId) throws TicketTrackerException {
        try {
            return documentDAO.findByTicketId(ticketId).stream()
                    .anyMatch(Document::isCompletionCertificate);
        } catch (SQLException e) {
            logger.error("Error checking completion certificate", e);
            throw new DatabaseException("Failed to check completion certificate", e);
        }
    }

    public void copyTicketAttachments(byte[] sourceTicketId, byte[] targetTicketId, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            List<Document> sourceDocs = documentDAO.findByTicketId(sourceTicketId);
            for (Document doc : sourceDocs) {
                Document copy = new Document();
                copy.setTicketId(targetTicketId);
                copy.setStepId(doc.getStepId());
                copy.setName(doc.getName());
                copy.setType(doc.getType());
                copy.setSize(doc.getSize());
                copy.setStoragePath(doc.getStoragePath());
                copy.setFileContent(doc.getFileContent());
                copy.setCompletionCertificate(doc.isCompletionCertificate());
                copy.setUploadedBy(currentUserId);
                documentDAO.create(copy);
            }
            logger.info("Copied {} attachments from ticket {} to ticket {}",
                    sourceDocs.size(), bytesToHex(sourceTicketId), bytesToHex(targetTicketId));
        } catch (SQLException e) {
            logger.error("Error copying ticket attachments", e);
            throw new DatabaseException("Failed to copy ticket attachments", e);
        }
    }

    private void validateDocument(Document document) throws ValidationException {
        ValidationException validation = new ValidationException("Document validation failed");

        if (document.getName() == null || document.getName().trim().isEmpty()) {
            validation.addError("Document name is required");
        }

        if (document.getType() == null || document.getType().trim().isEmpty()) {
            validation.addError("Document type is required");
        }

        if (document.getSize() <= 0) {
            validation.addError("Document size must be greater than 0");
        }

        if (document.getSize() > 5242880) {
            validation.addError("Document size must not exceed 5MB");
        }

        if (document.getFileContent() != null && document.getFileContent().length > 5242880) {
            validation.addError("File content size must not exceed 5MB");
        }

        if (document.getStoragePath() == null || document.getStoragePath().trim().isEmpty()) {
            validation.addError("Storage path is required");
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
