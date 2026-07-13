package com.tickettracker.dao;

import com.tickettracker.model.WorkflowComment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkflowCommentDAO extends BaseDAO {

    public WorkflowComment create(WorkflowComment comment) throws SQLException {
        String sql = "INSERT INTO workflow_comments (id, step_id, content, created_by, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            byte[] id = comment.getId();
            if (id == null) {
                id = generateUUID();
                comment.setId(id);
            }

            stmt.setBytes(1, id);
            stmt.setBytes(2, comment.getStepId());
            stmt.setString(3, comment.getContent());
            stmt.setBytes(4, comment.getCreatedBy());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Created workflow comment for step (rows affected: {})", rowsAffected);

            return findById(id);
        } catch (SQLException e) {
            logger.error("SQL Error creating workflow comment. Error code: {}, SQL state: {}, Message: {}",
                    e.getErrorCode(), e.getSQLState(), e.getMessage());
            logger.error("SQL statement: {}", sql);
            logger.error("Full exception: ", e);
            throw e;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public WorkflowComment update(WorkflowComment comment) throws SQLException {
        String sql = "UPDATE workflow_comments SET content = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, comment.getContent());
            stmt.setBytes(2, comment.getId());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Updated workflow comment (rows affected: {})", rowsAffected);

            return findById(comment.getId());
        } catch (SQLException e) {
            logger.error("SQL Error updating workflow comment. Error code: {}, SQL state: {}, Message: {}",
                    e.getErrorCode(), e.getSQLState(), e.getMessage());
            logger.error("SQL statement: {}", sql);
            logger.error("Full exception: ", e);
            throw e;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public WorkflowComment findById(byte[] id) throws SQLException {
        String sql = "SELECT wc.*, u.name as created_by_name, u.role as created_by_role " +
                "FROM workflow_comments wc " +
                "LEFT JOIN users u ON wc.created_by = u.id " +
                "WHERE wc.id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToWorkflowComment(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<WorkflowComment> findByStepId(byte[] stepId) throws SQLException {
        String sql = "SELECT wc.*, u.name as created_by_name, u.role as created_by_role " +
                "FROM workflow_comments wc " +
                "LEFT JOIN users u ON wc.created_by = u.id " +
                "WHERE wc.step_id = ? " +
                "ORDER BY wc.created_at ASC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);
            rs = stmt.executeQuery();

            List<WorkflowComment> comments = new ArrayList<>();
            while (rs.next()) {
                comments.add(mapResultSetToWorkflowComment(rs));
            }
            logger.info("Retrieved {} comments for step", comments.size());
            return comments;
        } catch (SQLException e) {
            logger.error("SQL Error retrieving comments by step. Error code: {}, SQL state: {}, Message: {}",
                    e.getErrorCode(), e.getSQLState(), e.getMessage());
            logger.error("SQL statement: {}", sql);
            throw e;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<WorkflowComment> findByStepIdAndUser(byte[] stepId, byte[] userId) throws SQLException {
        String sql = "SELECT wc.*, u.name as created_by_name, u.role as created_by_role " +
                "FROM workflow_comments wc " +
                "LEFT JOIN users u ON wc.created_by = u.id " +
                "WHERE wc.step_id = ? AND wc.created_by = ? " +
                "ORDER BY wc.created_at ASC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);
            stmt.setBytes(2, userId);
            rs = stmt.executeQuery();

            List<WorkflowComment> comments = new ArrayList<>();
            while (rs.next()) {
                comments.add(mapResultSetToWorkflowComment(rs));
            }
            return comments;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    private WorkflowComment mapResultSetToWorkflowComment(ResultSet rs) throws SQLException {
        WorkflowComment comment = new WorkflowComment();
        comment.setId(rs.getBytes("id"));
        comment.setStepId(rs.getBytes("step_id"));
        comment.setContent(rs.getString("content"));
        comment.setCreatedBy(rs.getBytes("created_by"));
        comment.setCreatedAt(rs.getTimestamp("created_at"));

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            comment.setUpdatedAt(updatedAt);
        }

        comment.setCreatedByName(rs.getString("created_by_name"));
        comment.setCreatedByRole(rs.getString("created_by_role"));

        return comment;
    }

    public boolean delete(byte[] id) throws SQLException {
        String sql = "DELETE FROM workflow_comments WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Deleted workflow comment (rows affected: {})", rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
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
