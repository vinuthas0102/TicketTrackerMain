package com.tickettracker.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for field_dropdown_options table.
 * Manages dropdown options for dynamic field configurations.
 */
public class FieldDropdownOptionDAO extends BaseDAO {

    public List<DropdownOption> findByFieldConfigId(byte[] fieldConfigId) throws SQLException {
        String sql = "SELECT id, field_config_id, option_value, option_label, display_order, " +
                "is_active, created_at FROM field_dropdown_options " +
                "WHERE field_config_id = ? ORDER BY display_order ASC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<DropdownOption> options = new ArrayList<>();

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, fieldConfigId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                options.add(mapResultSetToOption(rs));
            }
            return options;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public DropdownOption create(DropdownOption option) throws SQLException {
        String sql = "INSERT INTO field_dropdown_options " +
                "(field_config_id, option_value, option_label, display_order, is_active) " +
                "VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, option.getFieldConfigId());
            stmt.setString(2, option.getOptionValue());
            stmt.setString(3, option.getOptionLabel());
            stmt.setInt(4, option.getDisplayOrder());
            stmt.setInt(5, option.isActive() ? 1 : 0);
            stmt.executeUpdate();
            return option;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean update(DropdownOption option) throws SQLException {
        String sql = "UPDATE field_dropdown_options SET option_value = ?, option_label = ?, " +
                "display_order = ?, is_active = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, option.getOptionValue());
            stmt.setString(2, option.getOptionLabel());
            stmt.setInt(3, option.getDisplayOrder());
            stmt.setInt(4, option.isActive() ? 1 : 0);
            stmt.setBytes(5, option.getId());
            return stmt.executeUpdate() > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean delete(byte[] id) throws SQLException {
        String sql = "DELETE FROM field_dropdown_options WHERE id = ?";
        return executeUpdate(sql, id) > 0;
    }

    public void reorderOptions(List<byte[]> optionIds) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            String sql = "UPDATE field_dropdown_options SET display_order = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < optionIds.size(); i++) {
                stmt.setInt(1, i);
                stmt.setBytes(2, optionIds.get(i));
                stmt.addBatch();
            }
            stmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private DropdownOption mapResultSetToOption(ResultSet rs) throws SQLException {
        DropdownOption option = new DropdownOption();
        option.setId(rs.getBytes("id"));
        option.setFieldConfigId(rs.getBytes("field_config_id"));
        option.setOptionValue(rs.getString("option_value"));
        option.setOptionLabel(rs.getString("option_label"));
        option.setDisplayOrder(rs.getInt("display_order"));
        option.setActive(rs.getInt("is_active") == 1);
        option.setCreatedAt(rs.getTimestamp("created_at"));
        return option;
    }

    public static class DropdownOption {
        private byte[] id;
        private byte[] fieldConfigId;
        private String optionValue;
        private String optionLabel;
        private int displayOrder;
        private boolean active;
        private java.sql.Timestamp createdAt;

        public byte[] getId() { return id; }
        public void setId(byte[] id) { this.id = id; }
        public byte[] getFieldConfigId() { return fieldConfigId; }
        public void setFieldConfigId(byte[] fieldConfigId) { this.fieldConfigId = fieldConfigId; }
        public String getOptionValue() { return optionValue; }
        public void setOptionValue(String optionValue) { this.optionValue = optionValue; }
        public String getOptionLabel() { return optionLabel; }
        public void setOptionLabel(String optionLabel) { this.optionLabel = optionLabel; }
        public int getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public java.sql.Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }
    }
}
