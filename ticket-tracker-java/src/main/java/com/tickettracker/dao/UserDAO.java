package com.tickettracker.dao;

import com.tickettracker.model.User;
import com.tickettracker.util.UuidUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User operations.
 * Demonstrates pure JDBC operations with Oracle database.
 */
public class UserDAO extends BaseDAO {

    /**
     * Find user by email
     *
     * @param email user email
     * @return User object or null if not found
     * @throws SQLException if database error occurs
     */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT id, username, name, email, role, department, sap_id, password_hash, password_salt, " +
                "avatar, active, created_at, updated_at, last_login FROM users WHERE email = ? AND active = 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Find user by ID
     *
     * @param id user ID as byte array
     * @return User object or null if not found
     * @throws SQLException if database error occurs
     */
    public User findById(byte[] id) throws SQLException {
        String sql = "SELECT id, username, name, email, role, department, sap_id, password_hash, password_salt, " +
                "avatar, active, created_at, updated_at, last_login FROM users WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Find all active users
     *
     * @return List of User objects
     * @throws SQLException if database error occurs
     */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, username, name, email, role, department, sap_id, password_hash, password_salt, " +
                "avatar, active, created_at, updated_at, last_login FROM users WHERE active = 1 ORDER BY name";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<User> users = new ArrayList<>();

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            return users;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Find users by role
     *
     * @param role user role
     * @return List of User objects
     * @throws SQLException if database error occurs
     */
    public List<User> findByRole(String role) throws SQLException {
        String sql = "SELECT id, username, name, email, role, department, sap_id, password_hash, password_salt, " +
                "avatar, active, created_at, updated_at, last_login FROM users WHERE LOWER(role) = LOWER(?) AND active = 1 ORDER BY name";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<User> users = new ArrayList<>();

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, role);
            rs = stmt.executeQuery();

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            return users;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<User> findByRoleAndDepartment(String role, String department) throws SQLException {
        String sql = "SELECT id, username, name, email, role, department, sap_id, password_hash, password_salt, " +
                "avatar, active, created_at, updated_at, last_login FROM users WHERE LOWER(role) = LOWER(?) AND LOWER(department) = LOWER(?) AND active = 1 ORDER BY name";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<User> users = new ArrayList<>();

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, role);
            stmt.setString(2, department);
            rs = stmt.executeQuery();

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            return users;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public User findByDepartment(String department) throws SQLException {
        String sql = "SELECT id, username, name, email, role, department, sap_id, password_hash, password_salt, " +
                "avatar, active, created_at, updated_at, last_login FROM users WHERE LOWER(department) = LOWER(?) AND active = 1 FETCH FIRST 1 ROW ONLY";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, department);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Create a new user
     *
     * @param user User object to create
     * @return created User with generated ID
     * @throws SQLException if database error occurs
     */
    public User create(User user) throws SQLException {
        String sql = "INSERT INTO users (id, username, name, email, role, department, sap_id, password_hash, " +
                "password_salt, avatar, active) VALUES (SYS_GUID(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "RETURNING id INTO ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getRoleInternal());
            stmt.setString(5, user.getDepartment());
            stmt.setString(6, user.getSapId());
            stmt.setString(7, user.getPasswordHash());
            stmt.setString(8, user.getPasswordSalt());
            stmt.setString(9, user.getAvatar());
            stmt.setInt(10, user.isActive() ? 1 : 0);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                logger.info("User created successfully: {}", user.getEmail());
                // In a real implementation, retrieve the generated ID
                return user;
            }
            throw new SQLException("Failed to create user, no rows affected");
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    /**
     * Update an existing user
     *
     * @param user User object with updated data
     * @return true if update successful
     * @throws SQLException if database error occurs
     */
    public boolean update(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, name = ?, email = ?, role = ?, department = ?, " +
                "sap_id = ?, avatar = ?, active = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getRoleInternal());
            stmt.setString(5, user.getDepartment());
            stmt.setString(6, user.getSapId());
            stmt.setString(7, user.getAvatar());
            stmt.setInt(8, user.isActive() ? 1 : 0);
            stmt.setBytes(9, user.getId());

            int rowsAffected = stmt.executeUpdate();
            logger.info("User updated: {}", user.getEmail());
            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    /**
     * Update user password
     *
     * @param userId user ID
     * @param passwordHash new password hash
     * @param passwordSalt new password salt
     * @return true if update successful
     * @throws SQLException if database error occurs
     */
    public boolean updatePassword(byte[] userId, String passwordHash, String passwordSalt) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, password_salt = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, passwordHash);
            stmt.setString(2, passwordSalt);
            stmt.setBytes(3, userId);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Password updated for user ID: {}", UuidUtil.bytesToUuidString(userId));
            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    /**
     * Map ResultSet to User object
     *
     * @param rs ResultSet
     * @return User object
     * @throws SQLException if mapping fails
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getBytes("id"));
        user.setUsername(rs.getString("username"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setRoleInternal(rs.getString("role"));
        user.setDepartment(rs.getString("department"));
        user.setSapId(rs.getString("sap_id"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setPasswordSalt(rs.getString("password_salt"));
        user.setAvatar(rs.getString("avatar"));
        user.setActive(rs.getInt("active") == 1);
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setUpdatedAt(rs.getTimestamp("updated_at"));
        user.setLastLogin(rs.getTimestamp("last_login"));
        return user;
    }
}
