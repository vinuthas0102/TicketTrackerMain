package com.tickettracker.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkflowStepProgressDocumentDAO extends BaseDAO {

    public static class ProgressDocument {
        private byte[] id;
        private byte[] stepId;
        private byte[] ticketId;
        private byte[] auditLogId;
        private String fileName;
        private String filePath;
        private long fileSize;
        private String fileType;
        private byte[] uploadedBy;
        private Timestamp uploadedAt;
        private Timestamp deletedAt;
        private byte[] deletedBy;
        private String deleteReason;
        private boolean deleted;
        private Timestamp createdAt;
        private Timestamp updatedAt;
        private byte[] fileContent;

        public byte[] getId() { return id; }
        public void setId(byte[] id) { this.id = id; }
        public byte[] getStepId() { return stepId; }
        public void setStepId(byte[] stepId) { this.stepId = stepId; }
        public byte[] getTicketId() { return ticketId; }
        public void setTicketId(byte[] ticketId) { this.ticketId = ticketId; }
        public byte[] getAuditLogId() { return auditLogId; }
        public void setAuditLogId(byte[] auditLogId) { this.auditLogId = auditLogId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        public byte[] getUploadedBy() { return uploadedBy; }
        public void setUploadedBy(byte[] uploadedBy) { this.uploadedBy = uploadedBy; }
        public Timestamp getUploadedAt() { return uploadedAt; }
        public void setUploadedAt(Timestamp uploadedAt) { this.uploadedAt = uploadedAt; }
        public Timestamp getDeletedAt() { return deletedAt; }
        public void setDeletedAt(Timestamp deletedAt) { this.deletedAt = deletedAt; }
        public byte[] getDeletedBy() { return deletedBy; }
        public void setDeletedBy(byte[] deletedBy) { this.deletedBy = deletedBy; }
        public String getDeleteReason() { return deleteReason; }
        public void setDeleteReason(String deleteReason) { this.deleteReason = deleteReason; }
        public boolean isDeleted() { return deleted; }
        public void setDeleted(boolean deleted) { this.deleted = deleted; }
        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
        public Timestamp getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
        public byte[] getFileContent() { return fileContent; }
        public void setFileContent(byte[] fileContent) { this.fileContent = fileContent; }
    }

    public ProgressDocument create(ProgressDocument document) throws SQLException {
        String sql = "INSERT INTO workflow_step_progress_documents (id, step_id, ticket_id, audit_log_id, " +
                "file_name, file_path, file_size, file_type, uploaded_by, file_content) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean originalAutoCommit = true;

        try {
            conn = getConnection();

            originalAutoCommit = conn.getAutoCommit();
            if (!originalAutoCommit) {
                conn.setAutoCommit(true);
                logger.debug("Auto-commit was disabled, enabled for progress document creation");
            }

            stmt = conn.prepareStatement(sql);

            byte[] id = document.getId();
            if (id == null) {
                id = generateUUID();
                document.setId(id);
            }

            logger.debug("Creating progress document - ID: {}, StepID: {}, TicketID: {}, AuditLogID: {}, FileName: {}",
                    bytesToHex(id), bytesToHex(document.getStepId()), bytesToHex(document.getTicketId()),
                    bytesToHex(document.getAuditLogId()), document.getFileName());

            stmt.setBytes(1, id);
            stmt.setBytes(2, document.getStepId());
            stmt.setBytes(3, document.getTicketId());
            stmt.setBytes(4, document.getAuditLogId());
            stmt.setString(5, document.getFileName());
            stmt.setString(6, document.getFilePath());
            stmt.setLong(7, document.getFileSize());
            stmt.setString(8, document.getFileType());
            stmt.setBytes(9, document.getUploadedBy());
            stmt.setBytes(10, document.getFileContent());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Created progress document: {} (rows affected: {})", document.getFileName(), rowsAffected);

            String verifyQuery = "SELECT COUNT(*) FROM workflow_step_progress_documents WHERE id = ?";
            PreparedStatement verifyStmt = conn.prepareStatement(verifyQuery);
            verifyStmt.setBytes(1, id);
            ResultSet verifyRs = verifyStmt.executeQuery();
            if (verifyRs.next()) {
                int count = verifyRs.getInt(1);
                logger.debug("Verification query: Found {} progress document records with ID {}", count, bytesToHex(id));
            }
            verifyRs.close();
            verifyStmt.close();

            return document;
        } catch (SQLException e) {
            logger.error("Failed to create progress document - StepID: {}, FileName: {}, Error: {}",
                    bytesToHex(document.getStepId()), document.getFileName(), e.getMessage(), e);
            throw e;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public ProgressDocument findById(byte[] id) throws SQLException {
        String sql = "SELECT * FROM workflow_step_progress_documents WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToProgressDocument(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public ProgressDocument findById(byte[] id, boolean includeContent) throws SQLException {
        return findById(id);
    }

    public List<ProgressDocument> findByStepId(byte[] stepId) throws SQLException {
        String sql = "SELECT * FROM workflow_step_progress_documents WHERE step_id = ? " +
                "AND is_deleted = 0 ORDER BY uploaded_at DESC";

        logger.debug("Querying progress documents for StepID: {}", bytesToHex(stepId));

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);
            rs = stmt.executeQuery();

            List<ProgressDocument> documents = new ArrayList<>();
            while (rs.next()) {
                documents.add(mapResultSetToProgressDocument(rs));
            }

            logger.debug("Found {} progress documents for StepID: {}", documents.size(), bytesToHex(stepId));

            return documents;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<ProgressDocument> findByTicketId(byte[] ticketId) throws SQLException {
        String sql = "SELECT * FROM workflow_step_progress_documents WHERE ticket_id = ? " +
                "AND is_deleted = 0 ORDER BY uploaded_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, ticketId);
            rs = stmt.executeQuery();

            List<ProgressDocument> documents = new ArrayList<>();
            while (rs.next()) {
                documents.add(mapResultSetToProgressDocument(rs));
            }
            return documents;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<ProgressDocument> findByAuditLogId(byte[] auditLogId) throws SQLException {
        String sql = "SELECT * FROM workflow_step_progress_documents WHERE audit_log_id = ? " +
                "AND is_deleted = 0 ORDER BY uploaded_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, auditLogId);
            rs = stmt.executeQuery();

            List<ProgressDocument> documents = new ArrayList<>();
            while (rs.next()) {
                documents.add(mapResultSetToProgressDocument(rs));
            }
            return documents;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public boolean softDelete(byte[] id, byte[] deletedBy, String deleteReason) throws SQLException {
        String sql = "UPDATE workflow_step_progress_documents SET is_deleted = 1, deleted_by = ?, " +
                "deleted_at = CURRENT_TIMESTAMP, delete_reason = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, deletedBy);
            stmt.setString(2, deleteReason);
            stmt.setBytes(3, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Soft deleted progress document (rows affected: {})", rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean delete(byte[] id) throws SQLException {
        String sql = "DELETE FROM workflow_step_progress_documents WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Permanently deleted progress document (rows affected: {})", rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private ProgressDocument mapResultSetToProgressDocument(ResultSet rs) throws SQLException {
        ProgressDocument document = new ProgressDocument();
        document.setId(rs.getBytes("id"));
        document.setStepId(rs.getBytes("step_id"));
        document.setTicketId(rs.getBytes("ticket_id"));
        document.setAuditLogId(rs.getBytes("audit_log_id"));
        document.setFileName(rs.getString("file_name"));
        document.setFilePath(rs.getString("file_path"));
        document.setFileSize(rs.getLong("file_size"));
        document.setFileType(rs.getString("file_type"));
        document.setUploadedBy(rs.getBytes("uploaded_by"));
        document.setUploadedAt(rs.getTimestamp("uploaded_at"));
        document.setDeletedAt(rs.getTimestamp("deleted_at"));
        document.setDeletedBy(rs.getBytes("deleted_by"));
        document.setDeleteReason(rs.getString("delete_reason"));
        document.setDeleted(rs.getInt("is_deleted") == 1);
        document.setCreatedAt(rs.getTimestamp("created_at"));
        document.setUpdatedAt(rs.getTimestamp("updated_at"));
        document.setFileContent(rs.getBytes("file_content"));
        return document;
    }

    private byte[] generateUUID() throws SQLException {
        String sql = "SELECT SYS_GUID() FROM DUAL";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBytes(1);
            }
            throw new SQLException("Failed to generate UUID");
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
}
