package com.tickettracker.dao;

import com.tickettracker.model.WorkflowStep;
import com.tickettracker.model.WorkflowStepUpdateRequest;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkflowStepDAO extends BaseDAO {

    public WorkflowStep create(WorkflowStep step) throws SQLException {
        String sql = "INSERT INTO workflow_steps (id, ticket_id, step_number, title, description, status, " +
                "assigned_to, parent_step_id, level_1, level_2, level_3, dependencies, is_parallel, " +
                "mandatory_documents, optional_documents, completion_certificate_required, due_date, " +
                "data, progress, dependency_mode, is_dependency_locked, created_by, start_date, step_type, " +
                "remarks, actual_completed_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            byte[] id = step.getId();
            if (id == null) {
                id = generateUUID();
                step.setId(id);
            }

            stmt.setBytes(1, id);
            stmt.setBytes(2, step.getTicketId());
            stmt.setString(3, step.getStepNumber());
            stmt.setString(4, step.getTitle());
            stmt.setString(5, step.getDescription());
            stmt.setString(6, step.getStatus());
            stmt.setBytes(7, step.getAssignedTo());
            stmt.setBytes(8, step.getParentStepId());
            setIntOrNull(stmt, 9, step.getLevel1());
            setIntOrNull(stmt, 10, step.getLevel2());
            setIntOrNull(stmt, 11, step.getLevel3());
            stmt.setString(12, step.getDependencies());
            stmt.setInt(13, step.isParallel() ? 1 : 0);
            stmt.setString(14, step.getMandatoryDocuments());
            stmt.setString(15, step.getOptionalDocuments());
            stmt.setInt(16, step.isCompletionCertificateRequired() ? 1 : 0);
            stmt.setTimestamp(17, step.getDueDate());
            stmt.setString(18, step.getData());
            stmt.setBigDecimal(19, step.getProgress());
            stmt.setString(20, step.getDependencyMode());
            stmt.setInt(21, step.isDependencyLocked() ? 1 : 0);
            stmt.setBytes(22, step.getCreatedBy());
            stmt.setTimestamp(23, step.getStartDate());
            stmt.setString(24, step.getStepType());
            stmt.setString(25, step.getRemarks());
            stmt.setTimestamp(26, step.getActualCompletedAt());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Created workflow step: {} (rows affected: {})", step.getStepNumber(), rowsAffected);

            return step;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public WorkflowStep findById(byte[] id) throws SQLException {
        String sql = "SELECT * FROM workflow_steps WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToWorkflowStep(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<WorkflowStep> findByTicketId(byte[] ticketId) throws SQLException {
        String sql = "SELECT * FROM workflow_steps WHERE ticket_id = ? ORDER BY level_1, level_2, level_3";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, ticketId);
            rs = stmt.executeQuery();

            List<WorkflowStep> steps = new ArrayList<>();
            while (rs.next()) {
                steps.add(mapResultSetToWorkflowStep(rs));
            }
            return steps;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<WorkflowStep> findByParentStepId(byte[] parentStepId) throws SQLException {
        String sql = "SELECT * FROM workflow_steps WHERE parent_step_id = ? " +
                "ORDER BY level_1, level_2, level_3";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, parentStepId);
            rs = stmt.executeQuery();

            List<WorkflowStep> steps = new ArrayList<>();
            while (rs.next()) {
                steps.add(mapResultSetToWorkflowStep(rs));
            }
            return steps;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<WorkflowStep> findByAssignedTo(byte[] userId) throws SQLException {
        String sql = "SELECT * FROM workflow_steps WHERE assigned_to = ? " +
                "ORDER BY created_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, userId);
            rs = stmt.executeQuery();

            List<WorkflowStep> steps = new ArrayList<>();
            while (rs.next()) {
                steps.add(mapResultSetToWorkflowStep(rs));
            }
            return steps;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<WorkflowStep> findByStatus(String status) throws SQLException {
        String sql = "SELECT * FROM workflow_steps WHERE status = ? ORDER BY created_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            rs = stmt.executeQuery();

            List<WorkflowStep> steps = new ArrayList<>();
            while (rs.next()) {
                steps.add(mapResultSetToWorkflowStep(rs));
            }
            return steps;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public WorkflowStep update(WorkflowStep step) throws SQLException {
        String sql = "UPDATE workflow_steps SET step_number = ?, title = ?, description = ?, " +
                "status = ?, assigned_to = ?, parent_step_id = ?, level_1 = ?, level_2 = ?, " +
                "level_3 = ?, dependencies = ?, is_parallel = ?, mandatory_documents = ?, " +
                "optional_documents = ?, completion_certificate_required = ?, due_date = ?, " +
                "data = ?, progress = ?, dependency_mode = ?, is_dependency_locked = ?, " +
                "completed_at = ?, start_date = ?, remarks = ?, actual_completed_at = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, step.getStepNumber());
            stmt.setString(2, step.getTitle());
            stmt.setString(3, step.getDescription());
            stmt.setString(4, step.getStatus());
            stmt.setBytes(5, step.getAssignedTo());
            stmt.setBytes(6, step.getParentStepId());
            setIntOrNull(stmt, 7, step.getLevel1());
            setIntOrNull(stmt, 8, step.getLevel2());
            setIntOrNull(stmt, 9, step.getLevel3());
            stmt.setString(10, step.getDependencies());
            stmt.setInt(11, step.isParallel() ? 1 : 0);
            stmt.setString(12, step.getMandatoryDocuments());
            stmt.setString(13, step.getOptionalDocuments());
            stmt.setInt(14, step.isCompletionCertificateRequired() ? 1 : 0);
            stmt.setTimestamp(15, step.getDueDate());
            stmt.setString(16, step.getData());
            stmt.setBigDecimal(17, step.getProgress());
            stmt.setString(18, step.getDependencyMode());
            stmt.setInt(19, step.isDependencyLocked() ? 1 : 0);
            stmt.setTimestamp(20, step.getCompletedAt());
            stmt.setTimestamp(21, step.getStartDate());
            stmt.setString(22, step.getRemarks());
            stmt.setTimestamp(23, step.getActualCompletedAt());
            stmt.setBytes(24, step.getId());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Updated workflow step: {} (rows affected: {})", step.getStepNumber(), rowsAffected);

            return step;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public WorkflowStep updateSelective(WorkflowStepUpdateRequest updateRequest) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE workflow_steps SET ");
        List<Object> params = new ArrayList<>();

        boolean hasFields = false;

        if (updateRequest.getTitle() != null) {
            sql.append("title = ?, ");
            params.add(updateRequest.getTitle());
            hasFields = true;
        }

        if (updateRequest.getDescription() != null) {
            sql.append("description = ?, ");
            params.add(updateRequest.getDescription());
            hasFields = true;
        }

        if (updateRequest.getStatus() != null) {
            sql.append("status = ?, ");
            params.add(updateRequest.getStatus());
            hasFields = true;
        }

        if (updateRequest.getAssignedTo() != null) {
            sql.append("assigned_to = ?, ");
            params.add(updateRequest.getAssignedTo());
            hasFields = true;
        }

        if (updateRequest.getDueDate() != null) {
            sql.append("due_date = ?, ");
            params.add(updateRequest.getDueDate());
            hasFields = true;
        }

        if (updateRequest.getStartDate() != null) {
            sql.append("start_date = ?, ");
            params.add(updateRequest.getStartDate());
            hasFields = true;
        }

        if (updateRequest.getIsParallel() != null) {
            sql.append("is_parallel = ?, ");
            params.add(updateRequest.getIsParallel() ? 1 : 0);
            hasFields = true;
        }

        if (updateRequest.getProgress() != null) {
            sql.append("progress = ?, ");
            params.add(updateRequest.getProgress());
            hasFields = true;
        }

        if (updateRequest.getDependencyMode() != null) {
            sql.append("dependency_mode = ?, ");
            params.add(updateRequest.getDependencyMode());
            hasFields = true;
        }

        if (updateRequest.getMandatoryDocuments() != null) {
            sql.append("mandatory_documents = ?, ");
            params.add(updateRequest.getMandatoryDocuments());
            hasFields = true;
        }

        if (updateRequest.getOptionalDocuments() != null) {
            sql.append("optional_documents = ?, ");
            params.add(updateRequest.getOptionalDocuments());
            hasFields = true;
        }

        if (updateRequest.getCompletionCertificateRequired() != null) {
            sql.append("completion_certificate_required = ?, ");
            params.add(updateRequest.getCompletionCertificateRequired() ? 1 : 0);
            hasFields = true;
        }

        if (updateRequest.getCompletedAt() != null) {
            sql.append("completed_at = ?, ");
            params.add(updateRequest.getCompletedAt());
            hasFields = true;
        }

        if (updateRequest.getData() != null) {
            sql.append("data = ?, ");
            params.add(updateRequest.getData());
            hasFields = true;
        }

        if (updateRequest.getRemarks() != null) {
            sql.append("remarks = ?, ");
            params.add(updateRequest.getRemarks());
            hasFields = true;
        }

        if (!hasFields) {
            logger.warn("No fields to update for workflow step: {}", bytesToHex(updateRequest.getId()));
            return findById(updateRequest.getId());
        }

        sql.append("updated_at = CURRENT_TIMESTAMP WHERE id = ?");
        params.add(updateRequest.getId());

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql.toString());

            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof byte[]) {
                    stmt.setBytes(i + 1, (byte[]) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else if (param instanceof Long) {
                    stmt.setLong(i + 1, (Long) param);
                } else if (param instanceof Boolean) {
                    stmt.setInt(i + 1, (Boolean) param ? 1 : 0);
                } else if (param instanceof BigDecimal) {
                    stmt.setBigDecimal(i + 1, (BigDecimal) param);
                } else if (param instanceof Timestamp) {
                    stmt.setTimestamp(i + 1, (Timestamp) param);
                } else if (param == null) {
                    stmt.setNull(i + 1, java.sql.Types.NULL);
                }
            }

            int rowsAffected = stmt.executeUpdate();
            logger.info("Updated workflow step selectively (rows affected: {})", rowsAffected);

            return findById(updateRequest.getId());
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean updateProgress(byte[] id, java.math.BigDecimal progress) throws SQLException {
        String sql = "UPDATE workflow_steps SET progress = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBigDecimal(1, progress);
            stmt.setBytes(2, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Updated workflow step progress to {}% (rows affected: {})", progress, rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean delete(byte[] id) throws SQLException {
        String sql = "DELETE FROM workflow_steps WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Deleted workflow step ID: {} (rows affected: {})", bytesToHex(id), rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private WorkflowStep mapResultSetToWorkflowStep(ResultSet rs) throws SQLException {
        WorkflowStep step = new WorkflowStep();
        step.setId(rs.getBytes("id"));
        step.setTicketId(rs.getBytes("ticket_id"));
        step.setStepNumber(rs.getString("step_number"));
        step.setTitle(rs.getString("title"));
        step.setDescription(rs.getString("description"));
        step.setStatus(rs.getString("status"));
        step.setAssignedTo(rs.getBytes("assigned_to"));
        step.setParentStepId(rs.getBytes("parent_step_id"));
        step.setLevel1(getIntOrNull(rs, "level_1"));
        step.setLevel2(getIntOrNull(rs, "level_2"));
        step.setLevel3(getIntOrNull(rs, "level_3"));
        step.setDependencies(rs.getString("dependencies"));
        step.setParallel(rs.getInt("is_parallel") == 1);
        step.setMandatoryDocuments(rs.getString("mandatory_documents"));
        step.setOptionalDocuments(rs.getString("optional_documents"));
        step.setCompletionCertificateRequired(rs.getInt("completion_certificate_required") == 1);
        step.setDueDate(rs.getTimestamp("due_date"));
        step.setData(rs.getString("data"));
        step.setProgress(rs.getBigDecimal("progress"));
        step.setDependencyMode(rs.getString("dependency_mode"));
        step.setDependencyLocked(rs.getInt("is_dependency_locked") == 1);
        step.setCreatedBy(rs.getBytes("created_by"));
        step.setCreatedAt(rs.getTimestamp("created_at"));
        step.setUpdatedAt(rs.getTimestamp("updated_at"));
        step.setCompletedAt(rs.getTimestamp("completed_at"));
        step.setStartDate(rs.getTimestamp("start_date"));
        step.setStepType(rs.getString("step_type"));
        step.setRemarks(rs.getString("remarks"));
        step.setActualCompletedAt(rs.getTimestamp("actual_completed_at"));
        return step;
    }

    private void setIntOrNull(PreparedStatement stmt, int paramIndex, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, Types.INTEGER);
        } else {
            stmt.setInt(paramIndex, value);
        }
    }

    private Integer getIntOrNull(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
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
