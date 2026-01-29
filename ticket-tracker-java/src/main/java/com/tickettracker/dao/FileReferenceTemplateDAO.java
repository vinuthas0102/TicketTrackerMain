package com.tickettracker.dao;

import com.tickettracker.model.FileReferenceTemplate;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileReferenceTemplateDAO extends BaseDAO {

    public FileReferenceTemplate create(FileReferenceTemplate template) throws SQLException {
        String sql = "INSERT INTO file_reference_templates (id, template_name, description, " +
                "json_content, uploaded_by, is_active) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            byte[] id = template.getId();
            if (id == null) {
                id = generateUUID();
                template.setId(id);
            }

            stmt.setBytes(1, id);
            stmt.setString(2, template.getTemplateName());
            stmt.setString(3, template.getDescription());
            stmt.setString(4, template.getJsonContent());
            stmt.setBytes(5, template.getUploadedBy());
            stmt.setInt(6, template.isActive() ? 1 : 0);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Created file reference template: {} (rows affected: {})",
                    template.getTemplateName(), rowsAffected);

            return template;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public FileReferenceTemplate findById(byte[] id) throws SQLException {
        String sql = "SELECT * FROM file_reference_templates WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToTemplate(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public FileReferenceTemplate findByName(String templateName) throws SQLException {
        String sql = "SELECT * FROM file_reference_templates WHERE template_name = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, templateName);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToTemplate(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<FileReferenceTemplate> findAll() throws SQLException {
        String sql = "SELECT * FROM file_reference_templates ORDER BY template_name";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            List<FileReferenceTemplate> templates = new ArrayList<>();
            while (rs.next()) {
                templates.add(mapResultSetToTemplate(rs));
            }
            return templates;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<FileReferenceTemplate> findActiveTemplates() throws SQLException {
        String sql = "SELECT * FROM file_reference_templates WHERE is_active = 1 " +
                "ORDER BY template_name";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            List<FileReferenceTemplate> templates = new ArrayList<>();
            while (rs.next()) {
                templates.add(mapResultSetToTemplate(rs));
            }
            return templates;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public FileReferenceTemplate update(FileReferenceTemplate template) throws SQLException {
        String sql = "UPDATE file_reference_templates SET template_name = ?, description = ?, " +
                "json_content = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, template.getTemplateName());
            stmt.setString(2, template.getDescription());
            stmt.setString(3, template.getJsonContent());
            stmt.setInt(4, template.isActive() ? 1 : 0);
            stmt.setBytes(5, template.getId());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Updated file reference template: {} (rows affected: {})",
                    template.getTemplateName(), rowsAffected);

            return template;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean delete(byte[] id) throws SQLException {
        String sql = "DELETE FROM file_reference_templates WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Deleted file reference template ID: {} (rows affected: {})",
                    bytesToHex(id), rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean deactivate(byte[] id) throws SQLException {
        String sql = "UPDATE file_reference_templates SET is_active = 0, " +
                "updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Deactivated file reference template ID: {} (rows affected: {})",
                    bytesToHex(id), rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private FileReferenceTemplate mapResultSetToTemplate(ResultSet rs) throws SQLException {
        FileReferenceTemplate template = new FileReferenceTemplate();
        template.setId(rs.getBytes("id"));
        template.setTemplateName(rs.getString("template_name"));
        template.setDescription(rs.getString("description"));
        template.setJsonContent(rs.getString("json_content"));
        template.setUploadedBy(rs.getBytes("uploaded_by"));
        template.setActive(rs.getInt("is_active") == 1);
        template.setCreatedAt(rs.getTimestamp("created_at"));
        template.setUpdatedAt(rs.getTimestamp("updated_at"));
        return template;
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
