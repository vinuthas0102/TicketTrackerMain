package com.tickettracker.dao;

import com.tickettracker.model.Ticket;
import oracle.sql.RAW;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TicketDAO extends BaseDAO {
	WorkflowStepDAO wflDao = new WorkflowStepDAO();
	AuditLogDAO		logDao = new AuditLogDAO();
	
    public Ticket create(Ticket ticket) throws SQLException {
        String sql = "INSERT INTO tickets (id, ticket_number, module_id, title, description, status, " +
                "priority, created_by, assigned_to, due_date, data, property_id, property_location, " +
                "completion_documents_required, requires_finance_approval, start_date, request_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            byte[] id = ticket.getId();
            if (id == null) {
                id = generateUUID();
                ticket.setId(id);
            }

            String dataJson = ticket.getData();
            if (dataJson == null || dataJson.trim().isEmpty() || dataJson.equals("{}")) {
                dataJson = buildDataJson(ticket.getDepartment(), ticket.getCategory());
                logger.debug("Built data JSON from department/category: {}", dataJson);
            } else {
                logger.debug("Using provided data JSON: {}", dataJson);
            }

            logger.debug("Creating ticket with values - Title: {}, ModuleId: {}, Data: {}",
                    ticket.getTitle(),
                    ticket.getModuleId() != null ? bytesToHex(ticket.getModuleId()) : "NULL",
                    dataJson);

            stmt.setBytes(1, id);
            stmt.setString(2, ticket.getTicketNumber());
            stmt.setBytes(3, ticket.getModuleId());
            stmt.setString(4, ticket.getTitle());
            stmt.setString(5, ticket.getDescription());
            stmt.setString(6, ticket.getStatus());
            stmt.setString(7, ticket.getPriority());
            stmt.setBytes(8, ticket.getCreatedBy());
            stmt.setBytes(9, ticket.getAssignedTo());
            stmt.setTimestamp(10, ticket.getDueDate());
            stmt.setString(11, dataJson);
            stmt.setString(12, ticket.getPropertyId());
            stmt.setString(13, ticket.getPropertyLocation());
            stmt.setInt(14, ticket.isCompletionDocumentsRequired() ? 1 : 0);
            stmt.setInt(15, ticket.isRequiresFinanceApproval() ? 1 : 0);
            stmt.setTimestamp(16, ticket.getStartDate());
            stmt.setString(17, ticket.getRequestType());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Created ticket: {} (rows affected: {})", ticket.getTicketNumber(), rowsAffected);

            return ticket;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public Ticket findById(byte[] id) throws SQLException {
        String sql = "SELECT * FROM tickets WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToTicket(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public Ticket findByTicketNumber(String ticketNumber) throws SQLException {
        String sql = "SELECT * FROM tickets WHERE ticket_number = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, ticketNumber);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToTicket(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Ticket> findAll() throws SQLException {
        String sql = "SELECT * FROM tickets ORDER BY created_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            List<Ticket> tickets = new ArrayList<>();
            while (rs.next()) {
                tickets.add(mapResultSetToTicket(rs));
            }
            return tickets;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Ticket> findByModuleId(byte[] moduleId) throws SQLException {
        String sql = "SELECT * FROM tickets WHERE module_id = ? ORDER BY created_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, moduleId);

            logger.debug("Executing findByModuleId query with moduleId bytes: {}", bytesToHex(moduleId));
            rs = stmt.executeQuery();

            List<Ticket> tickets = new ArrayList<>();
            while (rs.next()) {
                tickets.add(mapResultSetToTicket(rs));
            }
            logger.debug("Found {} tickets for moduleId: {}", tickets.size(), bytesToHex(moduleId));
            return tickets;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Ticket> findByStatus(String status) throws SQLException {
        String sql = "SELECT * FROM tickets WHERE status = ? ORDER BY created_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            rs = stmt.executeQuery();

            List<Ticket> tickets = new ArrayList<>();
            while (rs.next()) {
                tickets.add(mapResultSetToTicket(rs));
            }
            return tickets;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Ticket> findByAssignedTo(byte[] userId) throws SQLException {
        String sql = "SELECT * FROM tickets WHERE assigned_to = ? ORDER BY created_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, userId);
            rs = stmt.executeQuery();

            List<Ticket> tickets = new ArrayList<>();
            while (rs.next()) {
                tickets.add(mapResultSetToTicket(rs));
            }
            return tickets;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Ticket> findByDepartment(String department) throws SQLException {
        if (department == null || department.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Ticket> allTickets = findAll();
        List<Ticket> departmentTickets = new ArrayList<>();

        for (Ticket ticket : allTickets) {
            String data = ticket.getData();
            if (data != null && !data.trim().isEmpty()) {
                if (data.contains("\"department\"")) {
                    String ticketDepartment = extractDepartmentFromJson(data);
                    if (department.equalsIgnoreCase(ticketDepartment)) {
                        departmentTickets.add(ticket);
                    }
                }
            }
        }

        logger.debug("Found {} tickets for department: {}", departmentTickets.size(), department);
        return departmentTickets;
    }

    private String extractDepartmentFromJson(String json) {
        if (json == null || !json.contains("\"department\"")) {
            return null;
        }

        try {
            int startIndex = json.indexOf("\"department\"");
            if (startIndex == -1) return null;

            int colonIndex = json.indexOf(":", startIndex);
            if (colonIndex == -1) return null;

            int valueStart = json.indexOf("\"", colonIndex) + 1;
            if (valueStart == 0) return null;

            int valueEnd = json.indexOf("\"", valueStart);
            if (valueEnd == -1) return null;

            return json.substring(valueStart, valueEnd);
        } catch (Exception e) {
            logger.warn("Failed to extract department from JSON: {}", json, e);
            return null;
        }
    }

    private String extractCategoryFromJson(String json) {
        if (json == null || !json.contains("\"category\"")) {
            return null;
        }

        try {
            int startIndex = json.indexOf("\"category\"");
            if (startIndex == -1) return null;

            int colonIndex = json.indexOf(":", startIndex);
            if (colonIndex == -1) return null;

            int valueStart = json.indexOf("\"", colonIndex) + 1;
            if (valueStart == 0) return null;

            int valueEnd = json.indexOf("\"", valueStart);
            if (valueEnd == -1) return null;

            return json.substring(valueStart, valueEnd);
        } catch (Exception e) {
            logger.warn("Failed to extract category from JSON: {}", json, e);
            return null;
        }
    }

    public Ticket update(Ticket ticket) throws SQLException {
        String sql = "UPDATE tickets SET module_id = ?, title = ?, description = ?, status = ?, " +
                "priority = ?, assigned_to = ?, due_date = ?, data = ?, property_id = ?, " +
                "property_location = ?, completion_documents_required = ?, " +
                "finance_officer_id = ?, finance_submission_count = ?, latest_finance_status = ?, " +
                "requires_finance_approval = ?, start_date = ?, request_type = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            stmt.setBytes(1, ticket.getModuleId());
            stmt.setString(2, ticket.getTitle());
            stmt.setString(3, ticket.getDescription());
            stmt.setString(4, ticket.getStatus());
            stmt.setString(5, ticket.getPriority());
            stmt.setBytes(6, ticket.getAssignedTo());
            stmt.setTimestamp(7, ticket.getDueDate());
            stmt.setString(8, ticket.getData());
            stmt.setString(9, ticket.getPropertyId());
            stmt.setString(10, ticket.getPropertyLocation());
            stmt.setInt(11, ticket.isCompletionDocumentsRequired() ? 1 : 0);
            stmt.setBytes(12, ticket.getFinanceOfficerId());
            stmt.setInt(13, ticket.getFinanceSubmissionCount());
            stmt.setString(14, ticket.getLatestFinanceStatus());
            stmt.setInt(15, ticket.isRequiresFinanceApproval() ? 1 : 0);
            stmt.setTimestamp(16, ticket.getStartDate());
            stmt.setString(17, ticket.getRequestType());
            stmt.setBytes(18, ticket.getId());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Updated ticket: {} (rows affected: {})", ticket.getTicketNumber(), rowsAffected);

            return ticket;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean delete(byte[] id) throws SQLException {
        String sql = "DELETE FROM tickets WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Deleted ticket ID: {} (rows affected: {})", bytesToHex(id), rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public int countByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tickets WHERE status = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Ticket> searchTickets(String searchTerm) throws SQLException {
        String sql = "SELECT * FROM tickets WHERE " +
                "UPPER(ticket_number) LIKE ? OR " +
                "UPPER(title) LIKE ? OR " +
                "UPPER(description) LIKE ? " +
                "ORDER BY created_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            String searchPattern = "%" + searchTerm.toUpperCase() + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            rs = stmt.executeQuery();

            List<Ticket> tickets = new ArrayList<>();
            while (rs.next()) {
                tickets.add(mapResultSetToTicket(rs));
            }
            return tickets;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Ticket> findAccessibleTickets(byte[] userId, String userRole) throws SQLException {
        String sql;

        if ("Admin".equalsIgnoreCase(userRole) || "Finance".equalsIgnoreCase(userRole)) {
            sql = "SELECT * FROM tickets ORDER BY created_at DESC";

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                conn = getConnection();
                stmt = conn.prepareStatement(sql);
                rs = stmt.executeQuery();

                List<Ticket> tickets = new ArrayList<>();
                while (rs.next()) {
                    tickets.add(mapResultSetToTicket(rs));
                }
                return tickets;
            } finally {
                closeResources(conn, stmt, rs);
            }
        } else if ("DO_Manager".equalsIgnoreCase(userRole)) {
            sql = "SELECT t.* FROM tickets t " +
                    "WHERE EXISTS ( " +
                    "  SELECT 1 FROM user_managed_properties ump " +
                    "  WHERE ump.user_id = ? " +
                    "  AND (t.property_id = ump.property_id OR ump.property_id = 'ALL') " +
                    ") " +
                    "ORDER BY t.created_at DESC";

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                conn = getConnection();
                stmt = conn.prepareStatement(sql);
                stmt.setBytes(1, userId);
                rs = stmt.executeQuery();

                List<Ticket> tickets = new ArrayList<>();
                while (rs.next()) {
                    tickets.add(mapResultSetToTicket(rs));
                }
                return tickets;
            } finally {
                closeResources(conn, stmt, rs);
            }
        } else {
            sql = "SELECT t.* FROM tickets t " +
                    "WHERE t.created_by = ? " +
                    "   OR t.assigned_to = ? " +
                    "   OR EXISTS ( " +
                    "     SELECT 1 FROM workflow_steps ws " +
                    "     WHERE ws.ticket_id = t.id " +
                    "     AND ws.assigned_to = ? " +
                    "   ) " +
                    "ORDER BY t.created_at DESC";

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                conn = getConnection();
                stmt = conn.prepareStatement(sql);
                stmt.setBytes(1, userId);
                stmt.setBytes(2, userId);
                stmt.setBytes(3, userId);
                rs = stmt.executeQuery();

                List<Ticket> tickets = new ArrayList<>();
                while (rs.next()) {
                    tickets.add(mapResultSetToTicket(rs));
                }
                return tickets;
            } finally {
                closeResources(conn, stmt, rs);
            }
        }
    }

    public List<Ticket> findAccessibleTicketsByModule(byte[] userId, String userRole, byte[] moduleId) throws SQLException {
        String sql;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();

            if ("Admin".equalsIgnoreCase(userRole) || "Finance".equalsIgnoreCase(userRole)) {
                sql = "SELECT * FROM tickets WHERE module_id = ? ORDER BY created_at DESC";
                stmt = conn.prepareStatement(sql);
                stmt.setBytes(1, moduleId);
            } else if ("DO_Manager".equalsIgnoreCase(userRole)) {
                sql = "SELECT t.* FROM tickets t " +
                        "WHERE t.module_id = ? " +
                        "AND EXISTS ( " +
                        "  SELECT 1 FROM user_managed_properties ump " +
                        "  WHERE ump.user_id = ? " +
                        "  AND (t.property_id = ump.property_id OR ump.property_id = 'ALL') " +
                        ") " +
                        "ORDER BY t.created_at DESC";
                stmt = conn.prepareStatement(sql);
                stmt.setBytes(1, moduleId);
                stmt.setBytes(2, userId);
            } else {
                sql = "SELECT t.* FROM tickets t " +
                        "WHERE t.module_id = ? " +
                        "   AND (t.created_by = ? " +
                        "        OR t.assigned_to = ? " +
                        "        OR EXISTS ( " +
                        "          SELECT 1 FROM workflow_steps ws " +
                        "          WHERE ws.ticket_id = t.id " +
                        "          AND ws.assigned_to = ? " +
                        "        )) " +
                        "ORDER BY t.created_at DESC";
                stmt = conn.prepareStatement(sql);
                stmt.setBytes(1, moduleId);
                stmt.setBytes(2, userId);
                stmt.setBytes(3, userId);
                stmt.setBytes(4, userId);
            }

            rs = stmt.executeQuery();
            List<Ticket> tickets = new ArrayList<>();
            while (rs.next()) {
                tickets.add(mapResultSetToTicket(rs));
            }
            return tickets;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    private Ticket mapResultSetToTicket(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setId(rs.getBytes("id"));
        ticket.setTicketNumber(rs.getString("ticket_number"));
        ticket.setModuleId(rs.getBytes("module_id"));
        ticket.setTitle(rs.getString("title"));
        ticket.setDescription(rs.getString("description"));
        ticket.setStatus(rs.getString("status"));
        ticket.setPriority(rs.getString("priority"));
        ticket.setCreatedBy(rs.getBytes("created_by"));
        ticket.setAssignedTo(rs.getBytes("assigned_to"));
        ticket.setDueDate(rs.getTimestamp("due_date"));

        String dataJson = rs.getString("data");
        ticket.setData(dataJson);

        if (dataJson != null && !dataJson.trim().isEmpty()) {
            ticket.setDepartment(extractDepartmentFromJson(dataJson));
            ticket.setCategory(extractCategoryFromJson(dataJson));
        }

        ticket.setPropertyId(rs.getString("property_id"));
        ticket.setPropertyLocation(rs.getString("property_location"));
        ticket.setCompletionDocumentsRequired(rs.getInt("completion_documents_required") == 1);
        ticket.setFinanceOfficerId(rs.getBytes("finance_officer_id"));
        ticket.setFinanceSubmissionCount(rs.getInt("finance_submission_count"));
        ticket.setLatestFinanceStatus(rs.getString("latest_finance_status"));
        ticket.setRequiresFinanceApproval(rs.getInt("requires_finance_approval") == 1);
        ticket.setRequestType(rs.getString("request_type"));
        ticket.setStartDate(rs.getTimestamp("start_date"));
        ticket.setCreatedAt(rs.getTimestamp("created_at"));
        ticket.setUpdatedAt(rs.getTimestamp("updated_at"));
        ticket.setWorkflow(wflDao.findByTicketId(rs.getBytes("id")));
        ticket.setAuditLog(logDao.findByTicketId(rs.getBytes("id")));
        return ticket;
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

    private String buildDataJson(String department, String category) {
        StringBuilder json = new StringBuilder("{");
        boolean hasFields = false;

        if (department != null && !department.trim().isEmpty()) {
            json.append("\"department\":\"").append(escapeJson(department)).append("\"");
            hasFields = true;
        }

        if (category != null && !category.trim().isEmpty()) {
            if (hasFields) json.append(",");
            json.append("\"category\":\"").append(escapeJson(category)).append("\"");
        }

        json.append("}");
        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
