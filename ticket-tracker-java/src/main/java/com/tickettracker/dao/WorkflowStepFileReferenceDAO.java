package com.tickettracker.dao;

import com.tickettracker.model.WorkflowStepFileReference;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkflowStepFileReferenceDAO extends BaseDAO {

    public WorkflowStepFileReference create(WorkflowStepFileReference reference) throws SQLException {
        String sql = "INSERT INTO workflow_step_file_references (id, step_id, template_id, " +
                "reference_name, is_mandatory, document_id, uploaded_by, uploaded_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            byte[] id = reference.getId();
            if (id == null) {
                id = generateUUID();
                reference.setId(id);
            }

            stmt.setBytes(1, id);
            stmt.setBytes(2, reference.getStepId());
            stmt.setBytes(3, reference.getTemplateId());
            stmt.setString(4, reference.getReferenceName());
            stmt.setInt(5, reference.isMandatory() ? 1 : 0);
            stmt.setBytes(6, reference.getDocumentId());
            stmt.setBytes(7, reference.getUploadedBy());
            stmt.setTimestamp(8, reference.getUploadedAt());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Created workflow step file reference: {} (rows affected: {})",
                    reference.getReferenceName(), rowsAffected);

            return reference;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public WorkflowStepFileReference findById(byte[] id) throws SQLException {
        String sql = "SELECT * FROM workflow_step_file_references WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToReference(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<WorkflowStepFileReference> findByStepId(byte[] stepId) throws SQLException {
        String sql = "SELECT r.*, d.name AS document_name, d.\"SIZE\" AS document_size " +
                "FROM workflow_step_file_references r " +
                "LEFT JOIN documents d ON r.document_id = d.id " +
                "WHERE r.step_id = ? " +
                "ORDER BY r.is_mandatory DESC, r.reference_name";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);
            rs = stmt.executeQuery();

            List<WorkflowStepFileReference> references = new ArrayList<>();
            while (rs.next()) {
                WorkflowStepFileReference ref = mapResultSetToReference(rs);
                try {
                    ref.setDocumentName(rs.getString("document_name"));
                    long docSize = rs.getLong("document_size");
                    if (!rs.wasNull()) {
                        ref.setDocumentSize(docSize);
                    }
                } catch (SQLException ignored) {}
                references.add(ref);
            }
            return references;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<WorkflowStepFileReference> findByTemplateId(byte[] templateId) throws SQLException {
        String sql = "SELECT * FROM workflow_step_file_references WHERE template_id = ? " +
                "ORDER BY created_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, templateId);
            rs = stmt.executeQuery();

            List<WorkflowStepFileReference> references = new ArrayList<>();
            while (rs.next()) {
                references.add(mapResultSetToReference(rs));
            }
            return references;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<WorkflowStepFileReference> findMandatoryPending(byte[] stepId) throws SQLException {
        String sql = "SELECT * FROM workflow_step_file_references " +
                "WHERE step_id = ? AND is_mandatory = 1 AND document_id IS NULL " +
                "ORDER BY reference_name";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);
            rs = stmt.executeQuery();

            List<WorkflowStepFileReference> references = new ArrayList<>();
            while (rs.next()) {
                references.add(mapResultSetToReference(rs));
            }
            return references;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public WorkflowStepFileReference update(WorkflowStepFileReference reference) throws SQLException {
        String sql = "UPDATE workflow_step_file_references SET reference_name = ?, " +
                "is_mandatory = ?, document_id = ?, uploaded_by = ?, uploaded_at = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, reference.getReferenceName());
            stmt.setInt(2, reference.isMandatory() ? 1 : 0);
            stmt.setBytes(3, reference.getDocumentId());
            stmt.setBytes(4, reference.getUploadedBy());
            stmt.setTimestamp(5, reference.getUploadedAt());
            stmt.setBytes(6, reference.getId());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Updated workflow step file reference: {} (rows affected: {})",
                    reference.getReferenceName(), rowsAffected);

            return reference;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean updateDocumentLink(byte[] id, byte[] documentId, byte[] uploadedBy) throws SQLException {
        String sql = "UPDATE workflow_step_file_references SET document_id = ?, uploaded_by = ?, " +
                "uploaded_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, documentId);
            stmt.setBytes(2, uploadedBy);
            stmt.setBytes(3, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Linked document to file reference (rows affected: {})", rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean delete(byte[] id) throws SQLException {
        String sql = "DELETE FROM workflow_step_file_references WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Deleted workflow step file reference ID: {} (rows affected: {})",
                    bytesToHex(id), rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean deleteByStepId(byte[] stepId) throws SQLException {
        String sql = "DELETE FROM workflow_step_file_references WHERE step_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Deleted file references for step (rows affected: {})", rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private WorkflowStepFileReference mapResultSetToReference(ResultSet rs) throws SQLException {
        WorkflowStepFileReference reference = new WorkflowStepFileReference();
        reference.setId(rs.getBytes("id"));
        reference.setStepId(rs.getBytes("step_id"));
        reference.setTemplateId(rs.getBytes("template_id"));
        reference.setReferenceName(rs.getString("reference_name"));
        reference.setMandatory(rs.getInt("is_mandatory") == 1);
        reference.setDocumentId(rs.getBytes("document_id"));
        reference.setUploadedBy(rs.getBytes("uploaded_by"));
        reference.setUploadedAt(rs.getTimestamp("uploaded_at"));
        reference.setCreatedAt(rs.getTimestamp("created_at"));
        reference.setUpdatedAt(rs.getTimestamp("updated_at"));
        return reference;
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
