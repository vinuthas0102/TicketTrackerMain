package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.dao.AuditLogDAO;
import com.tickettracker.dao.DocumentDAO;
import com.tickettracker.dao.WorkflowStepProgressDocumentDAO;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.AuditLog;
import com.tickettracker.model.Document;
import com.tickettracker.model.User;
import com.tickettracker.util.JsonUtil;
import com.tickettracker.util.ResponseUtil;
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
    private DocumentDAO documentDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.auditLogDAO = new AuditLogDAO();
        this.progressDocDAO = new WorkflowStepProgressDocumentDAO();
        this.documentDAO = new DocumentDAO();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User currentUser = getCurrentUser(request);
            String ticketIdParam = request.getParameter("ticketId");
            String stepIdParam = request.getParameter("stepId");

            if (ticketIdParam != null) {
                byte[] ticketId = hexToBytes(ticketIdParam);
                List<AuditLog> logs = auditLogDAO.findByTicketId(ticketId);
                List<AuditLogResponse> enrichedLogs = enrichLogsWithProgressDocs(logs);
                if (currentUser != null && "EMPLOYEE".equalsIgnoreCase(currentUser.getRole())) {
                    enrichedLogs = filterForEmployeeEnriched(enrichedLogs);
                }
                sendJsonResponse(response, enrichedLogs);
            } else if (stepIdParam != null) {
                byte[] stepId = hexToBytes(stepIdParam);
                List<AuditLog> logs = auditLogDAO.findByStepId(stepId);
                List<AuditLogResponse> enrichedLogs = enrichLogsWithProgressDocs(logs);
                if (currentUser != null && "EMPLOYEE".equalsIgnoreCase(currentUser.getRole())) {
                    enrichedLogs = filterForEmployeeEnriched(enrichedLogs);
                }
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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User currentUser = getCurrentUser(request);
            if (currentUser == null) {
                sendError(response, 401, "Not authenticated");
                return;
            }

            AuditLogCreateRequest createRequest = objectMapper.readValue(
                    request.getReader(), AuditLogCreateRequest.class);

            if (createRequest.ticketId == null || createRequest.ticketId.trim().isEmpty()) {
                sendError(response, 400, "ticketId is required");
                return;
            }
            if (createRequest.action == null || createRequest.action.trim().isEmpty()) {
                sendError(response, 400, "action is required");
                return;
            }
            if (createRequest.actionCategory == null || createRequest.actionCategory.trim().isEmpty()) {
                sendError(response, 400, "actionCategory is required");
                return;
            }
            if (createRequest.description == null || createRequest.description.trim().isEmpty()) {
                sendError(response, 400, "description is required");
                return;
            }
            if (createRequest.performedBy == null || createRequest.performedBy.trim().isEmpty()) {
                sendError(response, 400, "performedBy is required");
                return;
            }

            AuditLog auditLog = new AuditLog();
            auditLog.setTicketIdAsString(createRequest.ticketId);

            if (createRequest.stepId != null && !createRequest.stepId.trim().isEmpty()) {
                auditLog.setStepIdAsString(createRequest.stepId);
            }

            auditLog.setAction(createRequest.action);
            auditLog.setActionCategory(createRequest.actionCategory);
            auditLog.setDescription(createRequest.description);
            auditLog.setPerformedByAsString(createRequest.performedBy);

            if (createRequest.oldData != null && !createRequest.oldData.trim().isEmpty()) {
                auditLog.setOldData(createRequest.oldData);
            }

            if (createRequest.newData != null && !createRequest.newData.trim().isEmpty()) {
                auditLog.setNewData(createRequest.newData);
            }

            if (createRequest.metadata != null && !createRequest.metadata.isEmpty()) {
                String metadataJson = objectMapper.writeValueAsString(createRequest.metadata);
                auditLog.setMetadata(metadataJson);
            }

            AuditLog createdLog = auditLogDAO.create(auditLog);

            AuditLogCreateResponse responseData = new AuditLogCreateResponse();
            responseData.id = createdLog.getIdAsString();

            response.setStatus(HttpServletResponse.SC_CREATED);
            sendJsonResponse(response, responseData);

            logger.info("Created audit log {} for ticket {} with action {}",
                    responseData.id, createRequest.ticketId, createRequest.action);

        } catch (SQLException e) {
            logger.error("Database error in POST /api/audit-logs", e);
            sendError(response, 500, "Internal server error");
        } catch (Exception e) {
            logger.error("Unexpected error in POST /api/audit-logs", e);
            sendError(response, 500, "Internal server error");
        }
    }

    private List<AuditLogResponse> enrichLogsWithProgressDocs(List<AuditLog> logs) throws SQLException {
        List<AuditLogResponse> enrichedLogs = new ArrayList<>();
        for (AuditLog log : logs) {
            List<WorkflowStepProgressDocumentDAO.ProgressDocument> progressDocs =
                progressDocDAO.findByAuditLogId(log.getId());
            List<Document> stepDocs = documentDAO.findByAuditLogId(log.getId());
            enrichedLogs.add(new AuditLogResponse(log, progressDocs, stepDocs));
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

    private List<AuditLog> filterForEmployee(List<AuditLog> logs) {
        List<AuditLog> filtered = new ArrayList<>();
        for (AuditLog log : logs) {
            String category = log.getActionCategory();
            boolean isTicketLevel = "ticket_action".equalsIgnoreCase(category)
                    || "status_change".equalsIgnoreCase(category)
                    || log.getStepId() == null;
            if (isTicketLevel) {
                filtered.add(log);
            }
        }
        return filtered;
    }

    private List<AuditLogResponse> filterForEmployeeEnriched(List<AuditLogResponse> logs) {
        List<AuditLogResponse> filtered = new ArrayList<>();
        for (AuditLogResponse log : logs) {
            AuditLog al = log.getAuditLog();
            String category = al.getActionCategory();
            boolean isTicketLevel = "ticket_action".equalsIgnoreCase(category)
                    || "status_change".equalsIgnoreCase(category)
                    || al.getStepId() == null;
            boolean hasProgressDocs = log.getProgressDocs() != null && !log.getProgressDocs().isEmpty();
            boolean hasStepDocs = log.getStepDocs() != null && !log.getStepDocs().isEmpty();
            if (isTicketLevel || hasProgressDocs || hasStepDocs) {
                filtered.add(log);
            }
        }
        return filtered;
    }

    private void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        ResponseUtil.sendWrappedSuccess(response, data);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        ResponseUtil.sendWrappedError(response, status, message);
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
        private List<Document> stepDocs;

        public AuditLogResponse(AuditLog auditLog,
                List<WorkflowStepProgressDocumentDAO.ProgressDocument> progressDocs,
                List<Document> stepDocs) {
            this.auditLog = auditLog;
            this.progressDocs = progressDocs;
            this.stepDocs = stepDocs;
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

        public List<Document> getStepDocs() {
            return stepDocs;
        }

        public void setStepDocs(List<Document> stepDocs) {
            this.stepDocs = stepDocs;
        }
    }

    private static class AuditLogCreateRequest {
        public String ticketId;
        public String stepId;
        public String action;
        public String actionCategory;
        public String description;
        public String performedBy;
        public String oldData;
        public String newData;
        public java.util.Map<String, Object> metadata;
    }

    private static class AuditLogCreateResponse {
        public String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

}
