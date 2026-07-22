package com.tickettracker.dao;

import com.tickettracker.util.UuidUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for module_field_configurations table.
 * Manages dynamic field configurations per module and context.
 */
public class ModuleFieldConfigurationDAO extends BaseDAO {

    public List<FieldConfig> findByModuleId(byte[] moduleId, String context) throws SQLException {
        String sql = "SELECT id, module_id, field_key, field_type, label, context, display_order, " +
                "is_required, is_visible, is_system_field, default_value, validation_rules, " +
                "role_visibility, conditional_visibility, placeholder, help_text, " +
                "created_at, updated_at FROM module_field_configurations " +
                "WHERE module_id = ? AND context = ? ORDER BY display_order ASC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<FieldConfig> configs = new ArrayList<>();

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, moduleId);
            stmt.setString(2, context);
            rs = stmt.executeQuery();

            while (rs.next()) {
                configs.add(mapResultSetToFieldConfig(rs));
            }
            return configs;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public FieldConfig findById(byte[] id) throws SQLException {
        String sql = "SELECT id, module_id, field_key, field_type, label, context, display_order, " +
                "is_required, is_visible, is_system_field, default_value, validation_rules, " +
                "role_visibility, conditional_visibility, placeholder, help_text, " +
                "created_at, updated_at FROM module_field_configurations WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToFieldConfig(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public FieldConfig create(FieldConfig config) throws SQLException {
        String sql = "INSERT INTO module_field_configurations " +
                "(module_id, field_key, field_type, label, context, display_order, " +
                "is_required, is_visible, is_system_field, default_value, validation_rules, " +
                "role_visibility, conditional_visibility, placeholder, help_text) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, config.getModuleId());
            stmt.setString(2, config.getFieldKey());
            stmt.setString(3, config.getFieldType());
            stmt.setString(4, config.getLabel());
            stmt.setString(5, config.getContext());
            stmt.setInt(6, config.getDisplayOrder());
            stmt.setInt(7, config.isRequired() ? 1 : 0);
            stmt.setInt(8, config.isVisible() ? 1 : 0);
            stmt.setInt(9, config.isSystemField() ? 1 : 0);
            stmt.setString(10, config.getDefaultValue());
            setNullableClob(stmt, 11, config.getValidationRules());
            setNullableClob(stmt, 12, config.getRoleVisibility());
            setNullableClob(stmt, 13, config.getConditionalVisibility());
            stmt.setString(14, config.getPlaceholder());
            stmt.setString(15, config.getHelpText());

            stmt.executeUpdate();
            logger.info("Field config created: {} for module", config.getFieldKey());
            return config;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean update(FieldConfig config) throws SQLException {
        String sql = "UPDATE module_field_configurations SET field_key = ?, field_type = ?, " +
                "label = ?, context = ?, display_order = ?, is_required = ?, is_visible = ?, " +
                "is_system_field = ?, default_value = ?, validation_rules = ?, " +
                "role_visibility = ?, conditional_visibility = ?, placeholder = ?, help_text = ? " +
                "WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, config.getFieldKey());
            stmt.setString(2, config.getFieldType());
            stmt.setString(3, config.getLabel());
            stmt.setString(4, config.getContext());
            stmt.setInt(5, config.getDisplayOrder());
            stmt.setInt(6, config.isRequired() ? 1 : 0);
            stmt.setInt(7, config.isVisible() ? 1 : 0);
            stmt.setInt(8, config.isSystemField() ? 1 : 0);
            stmt.setString(9, config.getDefaultValue());
            setNullableClob(stmt, 10, config.getValidationRules());
            setNullableClob(stmt, 11, config.getRoleVisibility());
            setNullableClob(stmt, 12, config.getConditionalVisibility());
            stmt.setString(13, config.getPlaceholder());
            stmt.setString(14, config.getHelpText());
            stmt.setBytes(15, config.getId());

            return stmt.executeUpdate() > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean delete(byte[] id) throws SQLException {
        String sql = "DELETE FROM module_field_configurations WHERE id = ?";
        return executeUpdate(sql, id) > 0;
    }

    public void reorderFields(List<byte[]> fieldIds) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            String sql = "UPDATE module_field_configurations SET display_order = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < fieldIds.size(); i++) {
                stmt.setInt(1, i);
                stmt.setBytes(2, fieldIds.get(i));
                stmt.addBatch();
            }
            stmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private void setNullableClob(PreparedStatement stmt, int index, String value) throws SQLException {
        if (value != null) {
            stmt.setString(index, value);
        } else {
            stmt.setNull(index, java.sql.Types.CLOB);
        }
    }

    private FieldConfig mapResultSetToFieldConfig(ResultSet rs) throws SQLException {
        FieldConfig config = new FieldConfig();
        config.setId(rs.getBytes("id"));
        config.setModuleId(rs.getBytes("module_id"));
        config.setFieldKey(rs.getString("field_key"));
        config.setFieldType(rs.getString("field_type"));
        config.setLabel(rs.getString("label"));
        config.setContext(rs.getString("context"));
        config.setDisplayOrder(rs.getInt("display_order"));
        config.setRequired(rs.getInt("is_required") == 1);
        config.setVisible(rs.getInt("is_visible") == 1);
        config.setSystemField(rs.getInt("is_system_field") == 1);
        config.setDefaultValue(rs.getString("default_value"));
        config.setValidationRules(rs.getString("validation_rules"));
        config.setRoleVisibility(rs.getString("role_visibility"));
        config.setConditionalVisibility(rs.getString("conditional_visibility"));
        config.setPlaceholder(rs.getString("placeholder"));
        config.setHelpText(rs.getString("help_text"));
        config.setCreatedAt(rs.getTimestamp("created_at"));
        config.setUpdatedAt(rs.getTimestamp("updated_at"));
        return config;
    }

    public static class FieldConfig {
        private byte[] id;
        private byte[] moduleId;
        private String fieldKey;
        private String fieldType;
        private String label;
        private String context;
        private int displayOrder;
        private boolean required;
        private boolean visible;
        private boolean systemField;
        private String defaultValue;
        private String validationRules;
        private String roleVisibility;
        private String conditionalVisibility;
        private String placeholder;
        private String helpText;
        private java.sql.Timestamp createdAt;
        private java.sql.Timestamp updatedAt;

        public byte[] getId() { return id; }
        public void setId(byte[] id) { this.id = id; }
        public byte[] getModuleId() { return moduleId; }
        public void setModuleId(byte[] moduleId) { this.moduleId = moduleId; }
        public String getFieldKey() { return fieldKey; }
        public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
        public String getFieldType() { return fieldType; }
        public void setFieldType(String fieldType) { this.fieldType = fieldType; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
        public int getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        public boolean isVisible() { return visible; }
        public void setVisible(boolean visible) { this.visible = visible; }
        public boolean isSystemField() { return systemField; }
        public void setSystemField(boolean systemField) { this.systemField = systemField; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        public String getValidationRules() { return validationRules; }
        public void setValidationRules(String validationRules) { this.validationRules = validationRules; }
        public String getRoleVisibility() { return roleVisibility; }
        public void setRoleVisibility(String roleVisibility) { this.roleVisibility = roleVisibility; }
        public String getConditionalVisibility() { return conditionalVisibility; }
        public void setConditionalVisibility(String conditionalVisibility) { this.conditionalVisibility = conditionalVisibility; }
        public String getPlaceholder() { return placeholder; }
        public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
        public String getHelpText() { return helpText; }
        public void setHelpText(String helpText) { this.helpText = helpText; }
        public java.sql.Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }
        public java.sql.Timestamp getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(java.sql.Timestamp updatedAt) { this.updatedAt = updatedAt; }
    }
}
