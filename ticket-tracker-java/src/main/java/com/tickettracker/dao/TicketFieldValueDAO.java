package com.tickettracker.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for ticket_field_values table.
 * Manages dynamic field values associated with tickets.
 */
public class TicketFieldValueDAO extends BaseDAO {

    public Map<String, String> findByTicketId(byte[] ticketId) throws SQLException {
        String sql = "SELECT field_key, field_value FROM ticket_field_values WHERE ticket_id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Map<String, String> values = new HashMap<>();

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, ticketId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                values.put(rs.getString("field_key"), rs.getString("field_value"));
            }
            return values;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public Map<byte[], Map<String, String>> findByTicketIds(List<byte[]> ticketIds) throws SQLException {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return new HashMap<>();
        }

        StringBuilder placeholders = new StringBuilder("?");
        for (int i = 1; i < ticketIds.size(); i++) {
            placeholders.append(", ?");
        }

        String sql = "SELECT ticket_id, field_key, field_value FROM ticket_field_values " +
                "WHERE ticket_id IN (" + placeholders + ")";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Map<byte[], Map<String, String>> result = new HashMap<>();

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < ticketIds.size(); i++) {
                stmt.setBytes(i + 1, ticketIds.get(i));
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                byte[] ticketId = rs.getBytes("ticket_id");
                result.computeIfAbsent(ticketId, k -> new HashMap<>())
                        .put(rs.getString("field_key"), rs.getString("field_value"));
            }
            return result;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public void upsert(byte[] ticketId, String fieldKey, String fieldValue) throws SQLException {
        String sql = "MERGE INTO ticket_field_values t " +
                "USING (SELECT ? AS ticket_id, ? AS field_key FROM dual) src " +
                "ON (t.ticket_id = src.ticket_id AND t.field_key = src.field_key) " +
                "WHEN MATCHED THEN UPDATE SET field_value = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHEN NOT MATCHED THEN INSERT (ticket_id, field_key, field_value) VALUES (?, ?, ?)";

        executeUpdate(sql, ticketId, fieldKey, fieldValue, ticketId, fieldKey, fieldValue);
    }

    public boolean delete(byte[] ticketId, String fieldKey) throws SQLException {
        String sql = "DELETE FROM ticket_field_values WHERE ticket_id = ? AND field_key = ?";
        return executeUpdate(sql, ticketId, fieldKey) > 0;
    }

    public void deleteByTicketId(byte[] ticketId) throws SQLException {
        String sql = "DELETE FROM ticket_field_values WHERE ticket_id = ?";
        executeUpdate(sql, ticketId);
    }
}
