package com.tickettracker.dao;

import com.tickettracker.model.FileAttachment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileAttachmentDAO extends BaseDAO {

    public FileAttachment create(FileAttachment attachment) throws SQLException {
        String sql = "INSERT INTO file_attachments (id, ticket_id, step_id, file_name, file_size, " +
                "file_type, file_url, uploaded_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            byte[] id = attachment.getId();
            if (id == null) {
                id = generateUUID();
                attachment.setId(id);
            }

            stmt.setBytes(1, id);
            stmt.setBytes(2, attachment.getTicketId());
            stmt.setBytes(3, attachment.getStepId());
            stmt.setString(4, attachment.getFileName());
            stmt.setLong(5, attachment.getFileSize());
            stmt.setString(6, attachment.getFileType());
            stmt.setString(7, attachment.getFileUrl());
            stmt.setBytes(8, attachment.getUploadedBy());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Created file attachment: {} (rows affected: {})", attachment.getFileName(), rowsAffected);

            return attachment;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public FileAttachment findById(byte[] id) throws SQLException {
        String sql = "SELECT * FROM file_attachments WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToFileAttachment(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<FileAttachment> findByTicketId(byte[] ticketId) throws SQLException {
        String sql = "SELECT * FROM file_attachments WHERE ticket_id = ? ORDER BY created_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, ticketId);
            rs = stmt.executeQuery();

            List<FileAttachment> attachments = new ArrayList<>();
            while (rs.next()) {
                attachments.add(mapResultSetToFileAttachment(rs));
            }
            return attachments;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<FileAttachment> findByStepId(byte[] stepId) throws SQLException {
        String sql = "SELECT * FROM file_attachments WHERE step_id = ? ORDER BY created_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);
            rs = stmt.executeQuery();

            List<FileAttachment> attachments = new ArrayList<>();
            while (rs.next()) {
                attachments.add(mapResultSetToFileAttachment(rs));
            }
            return attachments;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public boolean delete(byte[] id) throws SQLException {
        String sql = "DELETE FROM file_attachments WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Deleted file attachment ID: {} (rows affected: {})", bytesToHex(id), rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private FileAttachment mapResultSetToFileAttachment(ResultSet rs) throws SQLException {
        FileAttachment attachment = new FileAttachment();
        attachment.setId(rs.getBytes("id"));
        attachment.setTicketId(rs.getBytes("ticket_id"));
        attachment.setStepId(rs.getBytes("step_id"));
        attachment.setFileName(rs.getString("file_name"));
        attachment.setFileSize(rs.getLong("file_size"));
        attachment.setFileType(rs.getString("file_type"));
        attachment.setFileUrl(rs.getString("file_url"));
        attachment.setUploadedBy(rs.getBytes("uploaded_by"));
        attachment.setCreatedAt(rs.getTimestamp("created_at"));
        return attachment;
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
