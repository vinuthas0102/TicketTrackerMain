package com.tickettracker.service;

import com.tickettracker.dao.WorkflowStepProgressDocumentDAO;
import com.tickettracker.dao.WorkflowStepProgressDocumentDAO.ProgressDocument;
import com.tickettracker.exception.DatabaseException;
import com.tickettracker.exception.ResourceNotFoundException;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class WorkflowStepProgressDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowStepProgressDocumentService.class);
    private static final long MAX_FILE_SIZE = 5242880L;

    private final WorkflowStepProgressDocumentDAO progressDocumentDAO;

    public WorkflowStepProgressDocumentService() {
        this.progressDocumentDAO = new WorkflowStepProgressDocumentDAO();
    }

    public ProgressDocument uploadProgressDocument(ProgressDocument document) throws TicketTrackerException {
        logger.info("Uploading progress document: {}", document.getFileName());

        if (document.getStepId() == null) {
            throw new ValidationException("Step ID is required");
        }

        if (document.getTicketId() == null) {
            throw new ValidationException("Ticket ID is required");
        }

        if (document.getFileName() == null || document.getFileName().trim().isEmpty()) {
            throw new ValidationException("File name is required");
        }

        if (document.getFileSize() <= 0) {
            throw new ValidationException("File size must be greater than 0");
        }

        if (document.getFileSize() > MAX_FILE_SIZE) {
            throw new ValidationException("File size exceeds maximum allowed size of 5 MB");
        }

        if (document.getFilePath() == null || document.getFilePath().trim().isEmpty()) {
            throw new ValidationException("File path is required for external storage");
        }

        if (document.getUploadedBy() == null) {
            throw new ValidationException("Uploaded by user ID is required");
        }

        try {
            return progressDocumentDAO.create(document);
        } catch (SQLException e) {
            logger.error("Error uploading progress document", e);
            throw new DatabaseException("Failed to upload progress document", e);
        }
    }

    public ProgressDocument getProgressDocumentById(byte[] id, boolean includeContent)
            throws TicketTrackerException {
        logger.info("Fetching progress document by ID (include content: {})", includeContent);

        try {
            ProgressDocument document = progressDocumentDAO.findById(id, includeContent);
            if (document == null) {
                throw new ResourceNotFoundException("Progress document not found");
            }

            return document;
        } catch (SQLException e) {
            logger.error("Error fetching progress document by ID", e);
            throw new DatabaseException("Failed to fetch progress document", e);
        }
    }

    public List<ProgressDocument> getProgressDocumentsByStepId(byte[] stepId) throws TicketTrackerException {
        logger.info("Fetching progress documents for step");
        try {
            return progressDocumentDAO.findByStepId(stepId);
        } catch (SQLException e) {
            logger.error("Error fetching progress documents by step ID", e);
            throw new DatabaseException("Failed to fetch progress documents for step", e);
        }
    }

    public List<ProgressDocument> getProgressDocumentsByTicketId(byte[] ticketId) throws TicketTrackerException {
        logger.info("Fetching progress documents for ticket");
        try {
            return progressDocumentDAO.findByTicketId(ticketId);
        } catch (SQLException e) {
            logger.error("Error fetching progress documents by ticket ID", e);
            throw new DatabaseException("Failed to fetch progress documents for ticket", e);
        }
    }

    public boolean deleteProgressDocument(byte[] id, byte[] deletedBy, String deleteReason)
            throws TicketTrackerException {
        logger.info("Soft deleting progress document");

        try {
            ProgressDocument existingDoc = progressDocumentDAO.findById(id, false);
            if (existingDoc == null) {
                throw new ResourceNotFoundException("Progress document not found");
            }

            if (existingDoc.isDeleted()) {
                throw new ValidationException("Progress document is already deleted");
            }

            return progressDocumentDAO.softDelete(id, deletedBy, deleteReason);
        } catch (SQLException e) {
            logger.error("Error deleting progress document", e);
            throw new DatabaseException("Failed to delete progress document", e);
        }
    }
}
