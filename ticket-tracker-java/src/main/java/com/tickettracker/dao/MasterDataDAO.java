package com.tickettracker.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MasterDataDAO extends BaseDAO {

    private static final Logger logger = LoggerFactory.getLogger(MasterDataDAO.class);

    public List<MapItem> findAll(String tableName) throws SQLException {
        return findAll(tableName, null);
    }

    public List<MapItem> findAll(String tableName, byte[] moduleId) throws SQLException {
        String sql;
        if (moduleId != null && "master_categories".equals(tableName)) {
            sql = String.format(
                "SELECT id, name, is_active, display_order, created_at, updated_at FROM %s WHERE module_id = ? ORDER BY display_order ASC", tableName);
        } else {
            sql = String.format(
                "SELECT id, name, is_active, display_order, created_at, updated_at FROM %s ORDER BY display_order ASC", tableName);
        }
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<MapItem> items = new ArrayList<>();
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            if (moduleId != null && "master_categories".equals(tableName)) {
                stmt.setBytes(1, moduleId);
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                MapItem item = new MapItem();
                item.setId(rs.getBytes("id"));
                item.setName(rs.getString("name"));
                item.setIsActive(rs.getBoolean("is_active"));
                item.setDisplayOrder(rs.getInt("display_order"));
                item.setCreatedAt(rs.getTimestamp("created_at"));
                item.setUpdatedAt(rs.getTimestamp("updated_at"));
                items.add(item);
            }
            return items;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public MapItem findByName(String tableName, String name) throws SQLException {
        return findByName(tableName, name, null);
    }

    public MapItem findByName(String tableName, String name, byte[] moduleId) throws SQLException {
        String sql;
        if (moduleId != null && "master_categories".equals(tableName)) {
            sql = String.format(
                "SELECT id, name, is_active, display_order, created_at, updated_at FROM %s WHERE LOWER(name) = LOWER(?) AND module_id = ?",
                tableName);
        } else {
            sql = String.format(
                "SELECT id, name, is_active, display_order, created_at, updated_at FROM %s WHERE LOWER(name) = LOWER(?)",
                tableName);
        }
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            if (moduleId != null && "master_categories".equals(tableName)) {
                stmt.setBytes(2, moduleId);
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                MapItem item = new MapItem();
                item.setId(rs.getBytes("id"));
                item.setName(rs.getString("name"));
                item.setIsActive(rs.getBoolean("is_active"));
                item.setDisplayOrder(rs.getInt("display_order"));
                item.setCreatedAt(rs.getTimestamp("created_at"));
                item.setUpdatedAt(rs.getTimestamp("updated_at"));
                return item;
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public MapItem add(String tableName, String name) throws SQLException {
        return add(tableName, name, null);
    }

    public MapItem add(String tableName, String name, byte[] moduleId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();

            int maxOrder = 0;
            String orderSql;
            if (moduleId != null && "master_categories".equals(tableName)) {
                orderSql = String.format("SELECT MAX(display_order) AS max_order FROM %s WHERE module_id = ?", tableName);
            } else {
                orderSql = String.format("SELECT MAX(display_order) AS max_order FROM %s", tableName);
            }
            stmt = conn.prepareStatement(orderSql);
            if (moduleId != null && "master_categories".equals(tableName)) {
                stmt.setBytes(1, moduleId);
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                maxOrder = rs.getInt("max_order");
            }
            rs.close();
            stmt.close();

            String insertSql;
            if (moduleId != null && "master_categories".equals(tableName)) {
                insertSql = String.format(
                    "INSERT INTO %s (id, name, is_active, display_order, created_at, updated_at, module_id) " +
                    "VALUES (SYS_GUID(), ?, 1, ?, SYSTIMESTAMP, SYSTIMESTAMP, ?)", tableName);
            } else {
                insertSql = String.format(
                    "INSERT INTO %s (id, name, is_active, display_order, created_at, updated_at) " +
                    "VALUES (SYS_GUID(), ?, 1, ?, SYSTIMESTAMP, SYSTIMESTAMP)", tableName);
            }
            stmt = conn.prepareStatement(insertSql);
            stmt.setString(1, name);
            stmt.setInt(2, maxOrder + 1);
            if (moduleId != null && "master_categories".equals(tableName)) {
                stmt.setBytes(3, moduleId);
            }
            stmt.executeUpdate();
            stmt.close();

            return findByName(tableName, name, moduleId);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public void delete(String tableName, byte[] id) throws SQLException {
        String sql = String.format("DELETE FROM %s WHERE id = ?", tableName);
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);
            stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public void toggleActive(String tableName, byte[] id, boolean isActive) throws SQLException {
        String sql = String.format("UPDATE %s SET is_active = ?, updated_at = SYSTIMESTAMP WHERE id = ?", tableName);
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, isActive ? 1 : 0);
            stmt.setBytes(2, id);
            stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public String getConfigValue(String key) throws SQLException {
        String sql = "SELECT value FROM master_config WHERE key = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, key);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("value");
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public void setConfigValue(String key, String value, String description) throws SQLException {
        String sql = "MERGE INTO master_config t USING (SELECT ? AS key, ? AS value, ? AS description FROM dual) s " +
            "ON (t.key = s.key) WHEN MATCHED THEN UPDATE SET t.value = s.value, t.description = s.description, t.updated_at = SYSTIMESTAMP " +
            "WHEN NOT MATCHED THEN INSERT (id, key, value, description, updated_at) VALUES (SYS_GUID(), s.key, s.value, s.description, SYSTIMESTAMP)";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.setString(3, description);
            stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public String getModuleCode(byte[] moduleId) throws SQLException {
        String sql = "SELECT config FROM modules WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, moduleId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                String configJson = rs.getString("config");
                if (configJson != null && !configJson.isEmpty()) {
                    com.fasterxml.jackson.databind.JsonNode node =
                        new com.fasterxml.jackson.databind.ObjectMapper().readTree(configJson);
                    if (node.has("moduleCode")) {
                        return node.get("moduleCode").asText();
                    }
                }
            }
            return "TKT";
        } catch (Exception e) {
            logger.warn("Error parsing module config: {}", e.getMessage());
            return "TKT";
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public void setModuleCode(byte[] moduleId, String moduleCode) throws SQLException {
        String sql = "SELECT config FROM modules WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, moduleId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                String configJson = rs.getString("config");
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.node.ObjectNode node;
                if (configJson != null && !configJson.isEmpty()) {
                    node = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(configJson);
                } else {
                    node = mapper.createObjectNode();
                }
                node.put("moduleCode", moduleCode);
                rs.close();
                stmt.close();

                String updateSql = "UPDATE modules SET config = ? WHERE id = ?";
                stmt = conn.prepareStatement(updateSql);
                stmt.setString(1, mapper.writeValueAsString(node));
                stmt.setBytes(2, moduleId);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Error setting module code: {}", e.getMessage());
            throw new SQLException("Failed to set module code", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public int getNextCounter(String locationPrefix, String moduleCode) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            String selectSql = "SELECT id, counter FROM ticket_number_counter WHERE location_prefix = ? AND module_code = ? FOR UPDATE";
            stmt = conn.prepareStatement(selectSql);
            stmt.setString(1, locationPrefix);
            stmt.setString(2, moduleCode);
            rs = stmt.executeQuery();

            int nextCounter;
            byte[] existingId = null;

            if (rs.next()) {
                existingId = rs.getBytes("id");
                nextCounter = rs.getInt("counter") + 1;
                rs.close();
                stmt.close();

                String updateSql = "UPDATE ticket_number_counter SET counter = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
                stmt = conn.prepareStatement(updateSql);
                stmt.setInt(1, nextCounter);
                stmt.setBytes(2, existingId);
                stmt.executeUpdate();
            } else {
                rs.close();
                stmt.close();

                nextCounter = 1;
                String insertSql = "INSERT INTO ticket_number_counter (id, location_prefix, module_code, counter, created_at, updated_at) " +
                    "VALUES (SYS_GUID(), ?, ?, 1, SYSTIMESTAMP, SYSTIMESTAMP)";
                stmt = conn.prepareStatement(insertSql);
                stmt.setString(1, locationPrefix);
                stmt.setString(2, moduleCode);
                stmt.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);
            return nextCounter;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<String> findActiveNames(String tableName) throws SQLException {
        return findActiveNames(tableName, null);
    }

    public List<String> findActiveNames(String tableName, byte[] moduleId) throws SQLException {
        String sql;
        if (moduleId != null && "master_categories".equals(tableName)) {
            sql = String.format(
                "SELECT name FROM %s WHERE is_active = 1 AND module_id = ? ORDER BY display_order ASC", tableName);
        } else {
            sql = String.format(
                "SELECT name FROM %s WHERE is_active = 1 ORDER BY display_order ASC", tableName);
        }
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<String> names = new ArrayList<>();
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            if (moduleId != null && "master_categories".equals(tableName)) {
                stmt.setBytes(1, moduleId);
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
            return names;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public static class MapItem {
        private byte[] id;
        private String name;
        private boolean isActive;
        private int displayOrder;
        private Timestamp createdAt;
        private Timestamp updatedAt;

        public byte[] getId() { return id; }
        public void setId(byte[] id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isActive() { return isActive; }
        public void setIsActive(boolean isActive) { this.isActive = isActive; }
        public int getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
        public Timestamp getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    }
}
