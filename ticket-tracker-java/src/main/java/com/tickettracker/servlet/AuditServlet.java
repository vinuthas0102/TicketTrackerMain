package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.dao.AuditLogDAO;
import com.tickettracker.dao.WorkflowStepProgressDocumentDAO;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.AuditLog;
import com.tickettracker.model.User;
import com.tickettracker.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/api/audit-logs/*")
public class AuditServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(AuditServlet.class);
    private AuditLogDAO auditLogDAO;
    private WorkflowStepProgressDocumentDAO progressDocDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.auditLogDAO = new AuditLogDAO();
        this.progressDocDAO = new WorkflowStepProgressDocumentDAO();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String ticketIdParam = request.getParameter("ticketId");
            String stepIdParam = request.getParameter("stepId");

            if (ticketIdParam != null) {
                byte[] ticketId = hexToBytes(ticketIdParam);
                List<AuditLog> logs = auditLogDAO.findByTicketId(ticketId);
                List<AuditLogResponse> enrichedLogs = enrichLogsWithProgressDocs(logs);
                sendJsonResponse(response, enrichedLogs);
            } else if (stepIdParam != null) {
                byte[] stepId = hexToBytes(stepIdParam);
                List<AuditLog> logs = auditLogDAO.findByStepId(stepId);
                List<AuditLogResponse> enrichedLogs = enrichLogsWithProgressDocs(logs);
                sendJsonResponse(response, enrichedLogs);
            } else {
                sendError(response, 400, "ticketId or stepId parameter required");
            }

        } catch (SQLException e) {
            logger.error("Database error in GET /api/audit-logs", e);
            sendError(response, 500, "Internal server error");
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/audit-logs", e);
            sendError(response, 500, "Internal server error");
        }
    }

    private List<AuditLogResponse> enrichLogsWithProgressDocs(List<AuditLog> logs) throws SQLException {
        List<AuditLogResponse> enrichedLogs = new ArrayList<>();
        for (AuditLog log : logs) {
            List<WorkflowStepProgressDocumentDAO.ProgressDocument> progressDocs =
                progressDocDAO.findByAuditLogId(log.getId());
            enrichedLogs.add(new AuditLogResponse(log, progressDocs));
        }
        return enrichedLogs;
    }

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("currentUser");
        }
        return null;
    }

    private void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), data);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                new ErrorResponse(status, message));
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return null;
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private static class AuditLogResponse {
        private AuditLog auditLog;
        private List<WorkflowStepProgressDocumentDAO.ProgressDocument> progressDocs;

        public AuditLogResponse(AuditLog auditLog, List<WorkflowStepProgressDocumentDAO.ProgressDocument> progressDocs) {
            this.auditLog = auditLog;
            this.progressDocs = progressDocs;
        }

        public AuditLog getAuditLog() {
            return auditLog;
        }

        public void setAuditLog(AuditLog auditLog) {
            this.auditLog = auditLog;
        }

        public List<WorkflowStepProgressDocumentDAO.ProgressDocument> getProgressDocs() {
            return progressDocs;
        }

        public void setProgressDocs(List<WorkflowStepProgressDocumentDAO.ProgressDocument> progressDocs) {
            this.progressDocs = progressDocs;
        }
    }

    private static class ErrorResponse {
        private int status;
        private String message;

        public ErrorResponse(int status, String message) {
            this.status = status;
            this.message = message;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}
