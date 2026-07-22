package com.tickettracker.service;

import com.tickettracker.exception.DatabaseException;
import com.tickettracker.exception.TicketTrackerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserPreferencesService {

    private static final Logger logger = LoggerFactory.getLogger(UserPreferencesService.class);

    private static final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;
    private static final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

    private static final String DEFAULT_ICON_DISPLAY_TYPE = "dropdown_menu";
    private static final String DEFAULT_ICON_SIZE = "medium";
    private static final boolean DEFAULT_SHOW_LABELS = true;
    private static final boolean DEFAULT_GROUP_BY_CATEGORY = false;
    private static final boolean DEFAULT_ANIMATION_ENABLED = true;

    public UserPreferencesService() {
    }

    public Map<String, Object> getUserPreferences(byte[] userId) throws TicketTrackerException {
        String userIdHex = bytesToHex(userId);

        Map<String, Object> cached = getFromCache(userIdHex);
        if (cached != null) {
            return cached;
        }

        Map<String, Object> result = loadFromDatabase(userId, userIdHex);
        putInCache(userIdHex, result);
        return result;
    }

    public void saveUserPreferences(byte[] userId, Map<String, Object> preferences)
            throws TicketTrackerException {
        String userIdHex = bytesToHex(userId);
        logger.info("Saving user preferences for user: {}", userIdHex);

        if (preferences == null) return;

        saveToDatabase(userId, preferences);
        invalidateCache(userIdHex);
    }

    public void resetToDefaults(byte[] userId) throws TicketTrackerException {
        String userIdHex = bytesToHex(userId);
        logger.info("Resetting user preferences to defaults for user: {}", userIdHex);

        Map<String, Object> defaults = createDefaultPreferences(userIdHex);
        saveToDatabase(userId, defaults);
        invalidateCache(userIdHex);
    }

    public void deleteUserPreferences(byte[] userId) throws TicketTrackerException {
        String userIdHex = bytesToHex(userId);
        logger.info("Deleting user preferences for user: {}", userIdHex);

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = new com.tickettracker.config.DatabaseConfig().getConnection();
            String sql = "DELETE FROM user_display_preferences WHERE user_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting user preferences", e);
            throw new DatabaseException("Failed to delete user preferences", e);
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            if (conn != null) try { conn.close(); } catch (SQLException e) { }
        }
        invalidateCache(userIdHex);
    }

    private Map<String, Object> loadFromDatabase(byte[] userId, String userIdHex) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = new com.tickettracker.config.DatabaseConfig().getConnection();
            String sql = "SELECT preferred_layout, items_per_page, show_completed, " +
                    "default_sort_field, default_sort_order, theme " +
                    "FROM user_display_preferences WHERE user_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> result = new HashMap<>();
                result.put("userId", userIdHex);
                result.put("iconDisplayType", rs.getString("preferred_layout"));
                result.put("iconSize", DEFAULT_ICON_SIZE);
                result.put("showLabels", rs.getInt("show_completed") == 1);
                result.put("groupByCategory", DEFAULT_GROUP_BY_CATEGORY);
                result.put("animationEnabled", DEFAULT_ANIMATION_ENABLED);
                return result;
            }
        } catch (SQLException e) {
            logger.warn("Could not load preferences from database, using defaults", e);
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            if (conn != null) try { conn.close(); } catch (SQLException e) { }
        }

        return createDefaultPreferences(userIdHex);
    }

    private void saveToDatabase(byte[] userId, Map<String, Object> preferences) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            com.tickettracker.config.DatabaseConfig dbConfig = com.tickettracker.config.DatabaseConfig.getInstance();
            conn = dbConfig.getConnection();

            String iconDisplayType = preferences.containsKey("iconDisplayType")
                    ? (String) preferences.get("iconDisplayType") : DEFAULT_ICON_DISPLAY_TYPE;
            boolean showLabels = preferences.containsKey("showLabels")
                    ? Boolean.TRUE.equals(preferences.get("showLabels")) : DEFAULT_SHOW_LABELS;

            String checkSql = "SELECT COUNT(*) FROM user_display_preferences WHERE user_id = ?";
            stmt = conn.prepareStatement(checkSql);
            stmt.setBytes(1, userId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            rs.close();
            stmt.close();

            if (count > 0) {
                String updateSql = "UPDATE user_display_preferences SET preferred_layout = ?, " +
                        "show_completed = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
                stmt = conn.prepareStatement(updateSql);
                stmt.setString(1, iconDisplayType);
                stmt.setInt(2, showLabels ? 1 : 0);
                stmt.setBytes(3, userId);
                stmt.executeUpdate();
            } else {
                String insertSql = "INSERT INTO user_display_preferences " +
                        "(user_id, preferred_layout, items_per_page, show_completed, " +
                        "default_sort_field, default_sort_order, theme) " +
                        "VALUES (?, ?, 20, ?, 'created_at', 'desc', 'light')";
                stmt = conn.prepareStatement(insertSql);
                stmt.setBytes(1, userId);
                stmt.setString(2, iconDisplayType);
                stmt.setInt(3, showLabels ? 1 : 0);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Error saving user preferences to database", e);
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            if (conn != null) try { conn.close(); } catch (SQLException e) { }
        }
    }

    private Map<String, Object> createDefaultPreferences(String userIdHex) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userIdHex);
        result.put("iconDisplayType", DEFAULT_ICON_DISPLAY_TYPE);
        result.put("iconSize", DEFAULT_ICON_SIZE);
        result.put("showLabels", DEFAULT_SHOW_LABELS);
        result.put("groupByCategory", DEFAULT_GROUP_BY_CATEGORY);
        result.put("animationEnabled", DEFAULT_ANIMATION_ENABLED);
        return result;
    }

    private Map<String, Object> getFromCache(String userIdHex) {
        Long timestamp = cacheTimestamps.get(userIdHex);
        if (timestamp == null) return null;
        if (System.currentTimeMillis() - timestamp > CACHE_TTL_MS) {
            cache.remove(userIdHex);
            cacheTimestamps.remove(userIdHex);
            return null;
        }
        return cache.get(userIdHex);
    }

    private void putInCache(String userIdHex, Map<String, Object> prefs) {
        cache.put(userIdHex, prefs);
        cacheTimestamps.put(userIdHex, System.currentTimeMillis());
    }

    private void invalidateCache(String userIdHex) {
        cache.remove(userIdHex);
        cacheTimestamps.remove(userIdHex);
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
