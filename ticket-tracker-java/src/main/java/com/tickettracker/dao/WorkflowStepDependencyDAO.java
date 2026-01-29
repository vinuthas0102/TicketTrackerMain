package com.tickettracker.dao;

import com.tickettracker.model.WorkflowStepDependency;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkflowStepDependencyDAO extends BaseDAO {

    public WorkflowStepDependency create(WorkflowStepDependency dependency) throws SQLException {
        String sql = "INSERT INTO workflow_step_dependencies (id, step_id, depends_on_step_id, " +
                "created_by, is_active) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            byte[] id = dependency.getId();
            if (id == null) {
                id = generateUUID();
                dependency.setId(id);
            }

            stmt.setBytes(1, id);
            stmt.setBytes(2, dependency.getStepId());
            stmt.setBytes(3, dependency.getDependsOnStepId());
            stmt.setBytes(4, dependency.getCreatedBy());
            stmt.setInt(5, dependency.isActive() ? 1 : 0);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Created workflow step dependency (rows affected: {})", rowsAffected);

            return dependency;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public WorkflowStepDependency findById(byte[] id) throws SQLException {
        String sql = "SELECT * FROM workflow_step_dependencies WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToDependency(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<WorkflowStepDependency> findByStepId(byte[] stepId) throws SQLException {
        String sql = "SELECT * FROM workflow_step_dependencies WHERE step_id = ? AND is_active = 1 " +
                "ORDER BY created_at";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);
            rs = stmt.executeQuery();

            List<WorkflowStepDependency> dependencies = new ArrayList<>();
            while (rs.next()) {
                dependencies.add(mapResultSetToDependency(rs));
            }
            return dependencies;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<WorkflowStepDependency> findDependentSteps(byte[] stepId) throws SQLException {
        String sql = "SELECT * FROM workflow_step_dependencies WHERE depends_on_step_id = ? " +
                "AND is_active = 1 ORDER BY created_at";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);
            rs = stmt.executeQuery();

            List<WorkflowStepDependency> dependencies = new ArrayList<>();
            while (rs.next()) {
                dependencies.add(mapResultSetToDependency(rs));
            }
            return dependencies;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public boolean dependencyExists(byte[] stepId, byte[] dependsOnStepId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM workflow_step_dependencies " +
                "WHERE step_id = ? AND depends_on_step_id = ? AND is_active = 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);
            stmt.setBytes(2, dependsOnStepId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public boolean setActive(byte[] id, boolean active) throws SQLException {
        String sql = "UPDATE workflow_step_dependencies SET is_active = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, active ? 1 : 0);
            stmt.setBytes(2, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Updated dependency active status (rows affected: {})", rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean delete(byte[] id) throws SQLException {
        String sql = "DELETE FROM workflow_step_dependencies WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Deleted workflow step dependency (rows affected: {})", rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public int deleteByStepId(byte[] stepId) throws SQLException {
        String sql = "DELETE FROM workflow_step_dependencies WHERE step_id = ? OR depends_on_step_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);
            stmt.setBytes(2, stepId);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Deleted all dependencies for step (rows affected: {})", rowsAffected);

            return rowsAffected;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private WorkflowStepDependency mapResultSetToDependency(ResultSet rs) throws SQLException {
        WorkflowStepDependency dependency = new WorkflowStepDependency();
        dependency.setId(rs.getBytes("id"));
        dependency.setStepId(rs.getBytes("step_id"));
        dependency.setDependsOnStepId(rs.getBytes("depends_on_step_id"));
        dependency.setCreatedBy(rs.getBytes("created_by"));
        dependency.setCreatedAt(rs.getTimestamp("created_at"));
        dependency.setActive(rs.getInt("is_active") == 1);
        return dependency;
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
