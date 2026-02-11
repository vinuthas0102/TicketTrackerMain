package com.tickettracker.dao;

import com.tickettracker.model.Document;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DocumentDAO extends BaseDAO {

    public Document create(Document document) throws SQLException {
        String sql = "INSERT INTO documents (id, ticket_id, step_id, name, type, file_size, url, " +
                "storage_path, uploaded_by, is_mandatory, is_completion_certificate, file_content) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();

            // Ensure autocommit is enabled for immediate persistence
            boolean originalAutoCommit = conn.getAutoCommit();
            if (!originalAutoCommit) {
                conn.setAutoCommit(true);
                logger.debug("Auto-commit was disabled, enabled for document creation");
            }

            stmt = conn.prepareStatement(sql);

            byte[] id = document.getId();
            if (id == null) {
                id = generateUUID();
                document.setId(id);
            }

            stmt.setBytes(1, id);
            stmt.setBytes(2, document.getTicketId());
            stmt.setBytes(3, document.getStepId());
            stmt.setString(4, document.getName());
            stmt.setString(5, document.getType());
            stmt.setLong(6, document.getSize());
            stmt.setString(7, document.getUrl());
            stmt.setString(8, document.getStoragePath());
            stmt.setBytes(9, document.getUploadedBy());
            stmt.setInt(10, document.isMandatory() ? 1 : 0);
            stmt.setInt(11, document.isCompletionCertificate() ? 1 : 0);

            if (document.getFileContent() != null) {
                stmt.setBytes(12, document.getFileContent());
            } else {
                stmt.setNull(12, Types.BLOB);
            }

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                logger.error("Document creation failed: no rows affected for document: {}", document.getName());
                throw new SQLException("Document creation failed: no rows were inserted");
            }

            logger.info("Created document: {} with file content: {} bytes (rows affected: {})",
                document.getName(),
                document.getFileContent() != null ? document.getFileContent().length : 0,
                rowsAffected);

            return document;
        } catch (SQLException e) {
            logger.error("SQL exception during document creation: {}", e.getMessage(), e);
            throw e;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public Document findById(byte[] id) throws SQLException {
        return findById(id, true);
    }

    public Document findById(byte[] id, boolean includeContent) throws SQLException {
        String sql = "SELECT * FROM documents WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToDocument(rs, includeContent);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Document> findByTicketId(byte[] ticketId) throws SQLException {
        String sql = "SELECT * FROM documents WHERE ticket_id = ? ORDER BY uploaded_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, ticketId);
            rs = stmt.executeQuery();

            List<Document> documents = new ArrayList<>();
            while (rs.next()) {
                documents.add(mapResultSetToDocument(rs));
            }
            return documents;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Document> findByStepId(byte[] stepId) throws SQLException {
        String sql = "SELECT * FROM documents WHERE step_id = ? ORDER BY uploaded_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);
            rs = stmt.executeQuery();

            List<Document> documents = new ArrayList<>();
            while (rs.next()) {
                documents.add(mapResultSetToDocument(rs));
            }
            return documents;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Document> findByUploadedBy(byte[] userId) throws SQLException {
        String sql = "SELECT * FROM documents WHERE uploaded_by = ? ORDER BY uploaded_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, userId);
            rs = stmt.executeQuery();

            List<Document> documents = new ArrayList<>();
            while (rs.next()) {
                documents.add(mapResultSetToDocument(rs));
            }
            return documents;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Document> findMandatoryByStepId(byte[] stepId) throws SQLException {
        String sql = "SELECT * FROM documents WHERE step_id = ? AND is_mandatory = 1 " +
                "ORDER BY uploaded_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);
            rs = stmt.executeQuery();

            List<Document> documents = new ArrayList<>();
            while (rs.next()) {
                documents.add(mapResultSetToDocument(rs));
            }
            return documents;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public Document update(Document document) throws SQLException {
        String sql = "UPDATE documents SET name = ?, type = ?, file_size = ?, url = ?, " +
                "storage_path = ?, is_mandatory = ?, is_completion_certificate = ? " +
                "WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, document.getName());
            stmt.setString(2, document.getType());
            stmt.setLong(3, document.getSize());
            stmt.setString(4, document.getUrl());
            stmt.setString(5, document.getStoragePath());
            stmt.setInt(6, document.isMandatory() ? 1 : 0);
            stmt.setInt(7, document.isCompletionCertificate() ? 1 : 0);
            stmt.setBytes(8, document.getId());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Updated document: {} (rows affected: {})", document.getName(), rowsAffected);

            return document;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean delete(byte[] id) throws SQLException {
        String sql = "DELETE FROM documents WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Deleted document ID: {} (rows affected: {})", bytesToHex(id), rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private Document mapResultSetToDocument(ResultSet rs) throws SQLException {
        return mapResultSetToDocument(rs, false);
    }

    private Document mapResultSetToDocument(ResultSet rs, boolean includeContent) throws SQLException {
        Document document = new Document();
        document.setId(rs.getBytes("id"));
        document.setTicketId(rs.getBytes("ticket_id"));
        document.setStepId(rs.getBytes("step_id"));
        document.setName(rs.getString("name"));
        document.setType(rs.getString("type"));
        document.setSize(rs.getLong("file_size"));
        document.setUrl(rs.getString("url"));
        document.setStoragePath(rs.getString("storage_path"));
        document.setUploadedBy(rs.getBytes("uploaded_by"));
        document.setUploadedAt(rs.getTimestamp("uploaded_at"));
        document.setMandatory(rs.getInt("is_mandatory") == 1);
        document.setCompletionCertificate(rs.getInt("is_completion_certificate") == 1);

        if (includeContent) {
            byte[] fileContent = rs.getBytes("file_content");
            if (fileContent != null) {
                document.setFileContent(fileContent);
            }
        }

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

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
