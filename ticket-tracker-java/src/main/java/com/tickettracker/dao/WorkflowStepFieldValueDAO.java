package com.tickettracker.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for workflow_step_field_values table.
 * Manages dynamic field values associated with workflow steps.
 */
public class WorkflowStepFieldValueDAO extends BaseDAO {

    public Map<String, String> findByStepId(byte[] stepId) throws SQLException {
        String sql = "SELECT field_key, field_value FROM workflow_step_field_values WHERE workflow_step_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Map<String, String> values = new HashMap<>();

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                values.put(rs.getString("field_key"), rs.getString("field_value"));
            }
            return values;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public Map<byte[], Map<String, String>> findByStepIds(List<byte[]> stepIds) throws SQLException {
        if (stepIds == null || stepIds.isEmpty()) {
            return new HashMap<>();
        }

        StringBuilder placeholders = new StringBuilder("?");
        for (int i = 1; i < stepIds.size(); i++) {
            placeholders.append(", ?");
        }

        String sql = "SELECT workflow_step_id, field_key, field_value FROM workflow_step_field_values " +
                "WHERE workflow_step_id IN (" + placeholders + ")";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Map<byte[], Map<String, String>> result = new HashMap<>();

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < stepIds.size(); i++) {
                stmt.setBytes(i + 1, stepIds.get(i));
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                byte[] stepId = rs.getBytes("workflow_step_id");
                result.computeIfAbsent(stepId, k -> new HashMap<>())
                        .put(rs.getString("field_key"), rs.getString("field_value"));
            }
            return result;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public void upsert(byte[] stepId, String fieldKey, String fieldValue) throws SQLException {
        String sql = "MERGE INTO workflow_step_field_values v " +
                "USING (SELECT ? AS step_id, ? AS field_key FROM dual) src " +
                "ON (v.workflow_step_id = src.step_id AND v.field_key = src.field_key) " +
                "WHEN MATCHED THEN UPDATE SET field_value = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHEN NOT MATCHED THEN INSERT (workflow_step_id, field_key, field_value) VALUES (?, ?, ?)";

        executeUpdate(sql, stepId, fieldKey, fieldValue, stepId, fieldKey, fieldValue);
    }

    public boolean delete(byte[] stepId, String fieldKey) throws SQLException {
        String sql = "DELETE FROM workflow_step_field_values WHERE workflow_step_id = ? AND field_key = ?";
        return executeUpdate(sql, stepId, fieldKey) > 0;
    }

    public void deleteByStepId(byte[] stepId) throws SQLException {
        String sql = "DELETE FROM workflow_step_field_values WHERE workflow_step_id = ?";
        executeUpdate(sql, stepId);
    }
}
