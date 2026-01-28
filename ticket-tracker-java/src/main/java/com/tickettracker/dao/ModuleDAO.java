package com.tickettracker.dao;

import com.tickettracker.model.Module;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleDAO extends BaseDAO {

    public Module create(Module module) throws SQLException {
        String sql = "INSERT INTO modules (id, name, description, icon, color, schema_id, config, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            byte[] id = module.getId();
            if (id == null) {
                id = generateUUID();
                module.setId(id);
            }

            stmt.setBytes(1, id);
            stmt.setString(2, module.getName());
            stmt.setString(3, module.getDescription());
            stmt.setString(4, module.getIcon());
            stmt.setString(5, module.getColor());
            stmt.setString(6, module.getSchemaId());
            stmt.setString(7, module.getConfig());
            stmt.setInt(8, module.isActive() ? 1 : 0);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Created module: {} (rows affected: {})", module.getName(), rowsAffected);

            return module;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public Module findById(byte[] id) throws SQLException {
        String sql = "SELECT * FROM modules WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToModule(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public Module findBySchemaId(String schemaId) throws SQLException {
        String sql = "SELECT * FROM modules WHERE schema_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, schemaId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToModule(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Module> findAll() throws SQLException {
        String sql = "SELECT * FROM modules ORDER BY name";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            List<Module> modules = new ArrayList<>();
            while (rs.next()) {
                modules.add(mapResultSetToModule(rs));
            }
            return modules;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Module> findAllActive() throws SQLException {
        String sql = "SELECT * FROM modules WHERE active = 1 ORDER BY name";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            List<Module> modules = new ArrayList<>();
            while (rs.next()) {
                modules.add(mapResultSetToModule(rs));
            }
            return modules;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public Module update(Module module) throws SQLException {
        String sql = "UPDATE modules SET name = ?, description = ?, icon = ?, color = ?, " +
                "schema_id = ?, config = ?, active = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, module.getName());
            stmt.setString(2, module.getDescription());
            stmt.setString(3, module.getIcon());
            stmt.setString(4, module.getColor());
            stmt.setString(5, module.getSchemaId());
            stmt.setString(6, module.getConfig());
            stmt.setInt(7, module.isActive() ? 1 : 0);
            stmt.setBytes(8, module.getId());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Updated module: {} (rows affected: {})", module.getName(), rowsAffected);

            return module;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean delete(byte[] id) throws SQLException {
        String sql = "DELETE FROM modules WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Deleted module ID: {} (rows affected: {})", bytesToHex(id), rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean setActive(byte[] id, boolean active) throws SQLException {
        String sql = "UPDATE modules SET active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, active ? 1 : 0);
            stmt.setBytes(2, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Set module {} active status to: {} (rows affected: {})",
                bytesToHex(id), active, rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private Module mapResultSetToModule(ResultSet rs) throws SQLException {
        Module module = new Module();
        module.setId(rs.getBytes("id"));
        module.setName(rs.getString("name"));
        module.setDescription(rs.getString("description"));
        module.setIcon(rs.getString("icon"));
        module.setColor(rs.getString("color"));
        module.setSchemaId(rs.getString("schema_id"));
        module.setConfig(rs.getString("config"));
        module.setActive(rs.getInt("active") == 1);
        module.setCreatedAt(rs.getTimestamp("created_at"));
        module.setUpdatedAt(rs.getTimestamp("updated_at"));
        return module;
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
