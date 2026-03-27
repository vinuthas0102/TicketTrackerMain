package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.dao.AuditLogDAO;
import com.tickettracker.dao.UserDAO;
import com.tickettracker.dao.WorkflowStepProgressDocumentDAO;
import com.tickettracker.dao.WorkflowStepProgressDocumentDAO.ProgressDocument;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.AuditLog;
import com.tickettracker.model.BulkWorkflowStepRequest;
import com.tickettracker.model.Document;
import com.tickettracker.model.User;
import com.tickettracker.model.WorkflowStep;
import com.tickettracker.model.WorkflowStepUpdateRequest;
import com.tickettracker.service.DocumentService;
import com.tickettracker.service.WorkflowService;
import com.tickettracker.service.WorkflowStepProgressDocumentService;
import com.tickettracker.util.ByteArrayUtil;
import com.tickettracker.util.UuidUtil;
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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/workflow-steps/*")
public class WorkflowStepServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowStepServlet.class);
    private WorkflowService workflowService;
    private DocumentService documentService;
    private WorkflowStepProgressDocumentService progressDocumentService;
    private AuditLogDAO auditLogDAO;
    private UserDAO userDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.workflowService = new WorkflowService();
        this.documentService = new DocumentService();
        this.progressDocumentService = new WorkflowStepProgressDocumentService();
        this.auditLogDAO = new AuditLogDAO();
        this.userDAO = new UserDAO();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetAllSteps(request, response);
            } else {
                String[] pathParts = pathInfo.split("/");
                if (pathParts.length == 2) {
                    handleGetStep(pathParts[1], response);
                } else if (pathParts.length == 3) {
                    String stepId = pathParts[1];
                    String subResource = pathParts[2];

                    switch (subResource) {
                        case "files":
                            handleGetStepFiles(stepId, response);
                            break;
                        case "progress-documents":
                            handleGetProgressDocuments(stepId, request, response);
                            break;
                        case "progress-history":
                            handleGetProgressHistory(stepId, response);
                            break;
                        default:
                            sendError(response, 400, "Invalid sub-resource: " + subResource);
                    }
                } else {
                    sendError(response, 400, "Invalid request path");
                }
            }
        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/workflow-steps", e);
            sendError(response, 500, "Internal server error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            User currentUser = getCurrentUser(request);
            if (currentUser == null) {
                sendError(response, 401, "Authentication required");
                return;
            }

            if (!currentUser.isAdmin()) {
                logger.warn("Permission denied: User {} (role: {}) attempted to create workflow step",
                        currentUser.getEmail(), currentUser.getRole());
                sendError(response, 403, "Forbidden: Only EO users can create workflow steps");
                return;
            }

            if (pathInfo != null && pathInfo.equals("/bulk")) {
                handleBulkCreate(request, response, currentUser);
                return;
            }

            String body = getRequestBody(request);
            WorkflowStep step;

            try {
                step = objectMapper.readValue(body, WorkflowStep.class);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid UUID format in workflow step creation request", e);
                sendError(response, 400, "Invalid UUID format: " + e.getMessage());
                return;
            }

            WorkflowStep createdStep = workflowService.createWorkflowStep(step, currentUser.getId());

            response.setStatus(HttpServletResponse.SC_CREATED);
            sendJsonResponse(response, createdStep);

        } catch (TicketTrackerException e) {
        	e.printStackTrace();
        	handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in POST /api/workflow-steps", e);
            sendError(response, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleBulkCreate(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException, TicketTrackerException {
        String body = getRequestBody(request);
        BulkWorkflowStepRequest bulkRequest = objectMapper.readValue(body, BulkWorkflowStepRequest.class);

        if (bulkRequest.getSteps() == null || bulkRequest.getSteps().isEmpty()) {
            sendError(response, 400, "No steps provided in bulk request");
            return;
        }

        for (WorkflowStep step : bulkRequest.getSteps()) {
            if (step.getTicketId() == null && bulkRequest.getTicketId() != null) {
                step.setTicketId(bulkRequest.getTicketId());
            }
            if (step.getParentStepId() == null && bulkRequest.getParentStepId() != null) {
                step.setParentStepId(bulkRequest.getParentStepId());
            }
            if (step.getStepNumber() == null || step.getStepNumber().trim().isEmpty()) {
                step.setStepNumber("0");
            }
        }

        List<WorkflowStep> createdSteps = workflowService.createWorkflowStepsBulk(bulkRequest.getSteps(), currentUser.getId());

        response.setStatus(HttpServletResponse.SC_CREATED);
        sendJsonResponse(response, createdSteps);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            User currentUser = getCurrentUser(request);
            if (currentUser == null) {
                sendError(response, 401, "Authentication required");
                return;
            }

            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Step ID required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }
            byte[] stepId = UuidUtil.uuidStringToBytes(pathParts[1]);

            boolean canUpdate = workflowService.canUserUpdateWorkflowStep(currentUser.getId(), stepId);
            if (!canUpdate) {
                logger.warn("Permission denied: User {} (role: {}) attempted to update workflow step {}",
                        currentUser.getEmail(), currentUser.getRole(), pathParts[1]);
                sendError(response, 403, "Forbidden: You do not have permission to update this workflow step");
                return;
            }

            String body = getRequestBody(request);
            WorkflowStepUpdateRequest updateRequest;

            try {
                updateRequest = objectMapper.readValue(body, WorkflowStepUpdateRequest.class);
                updateRequest.setId(stepId);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid UUID format in workflow step update request", e);
                sendError(response, 400, "Invalid UUID format: " + e.getMessage());
                return;
            }

            WorkflowStep updatedStep = workflowService.updateWorkflowStep(updateRequest, currentUser.getId());

            sendJsonResponse(response, updatedStep);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in PUT /api/workflow-steps", e);
            sendError(response, 500, "Internal server error: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            User currentUser = getCurrentUser(request);
            if (currentUser == null) {
                sendError(response, 401, "Authentication required");
                return;
            }

            if (!currentUser.isAdmin()) {
                logger.warn("Permission denied: User {} (role: {}) attempted to delete workflow step",
                        currentUser.getEmail(), currentUser.getRole());
                sendError(response, 403, "Forbidden: Only EO users can delete workflow steps");
                return;
            }

            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Step ID required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }

            byte[] stepId = UuidUtil.uuidStringToBytes(pathParts[1]);
            workflowService.deleteWorkflowStep(stepId, currentUser.getId());

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in DELETE /api/workflow-steps", e);
            sendError(response, 500, "Internal server error");
        }
    }

    private void handleGetAllSteps(HttpServletRequest request, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        String ticketIdParam = request.getParameter("ticketId");

        if (ticketIdParam != null) {
            byte[] ticketId = UuidUtil.uuidStringToBytes(ticketIdParam);
            List<WorkflowStep> steps = workflowService.getWorkflowStepsByTicketId(ticketId);
            sendJsonResponse(response, steps);
        } else {
            sendError(response, 400, "ticketId parameter required");
        }
    }

    private void handleGetStep(String stepId, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        byte[] id = UuidUtil.uuidStringToBytes(stepId);
        WorkflowStep step = workflowService.getWorkflowStepById(id);
        sendJsonResponse(response, step);
    }

    private void handleGetStepFiles(String stepId, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        logger.debug("Fetching files for step ID: {}", stepId);
        byte[] id = UuidUtil.uuidStringToBytes(stepId);
        List<Document> documents = documentService.getDocumentsByStepId(id);
        sendJsonResponse(response, documents);
    }

    private void handleGetProgressDocuments(String stepId, HttpServletRequest request,
            HttpServletResponse response) throws TicketTrackerException, IOException {
        logger.debug("Fetching progress documents for step ID: {}", stepId);
        byte[] id = UuidUtil.uuidStringToBytes(stepId);

        List<ProgressDocument> documents = progressDocumentService.getProgressDocumentsByStepId(id);
        sendJsonResponse(response, documents);
    }

    private void handleGetProgressHistory(String stepId, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        logger.debug("=== Starting progress history fetch for step ID: {} ===", stepId);
        byte[] id = UuidUtil.uuidStringToBytes(stepId);
        logger.debug("Converted step ID to bytes: {}", ByteArrayUtil.bytesToHex(id));

        try {
            logger.debug("Querying audit_logs WHERE step_id = {}", ByteArrayUtil.bytesToHex(id));
            List<AuditLog> auditLogs = auditLogDAO.findByStepId(id);
            logger.debug("Found {} audit logs for step {}", auditLogs.size(), stepId);

            List<ProgressDocument> progressDocuments = new WorkflowStepProgressDocumentDAO().findByStepId(id);
            logger.debug("Found {} progress documents for step {}", progressDocuments.size(), stepId);

            List<Document> allDocuments = documentService.getDocumentsByStepId(id);
            logger.debug("Found {} total documents for step {}", allDocuments.size(), stepId);

            List<User> allUsers = userDAO.findAll();

            Map<String, User> userMap = new HashMap<>();
            for (User user : allUsers) {
                userMap.put(ByteArrayUtil.bytesToHex(user.getId()), user);
            }

            Map<String, List<Map<String, Object>>> progressDocsMap = new HashMap<>();
            for (ProgressDocument doc : progressDocuments) {
                if (doc.getAuditLogId() != null) {
                    String auditLogIdHex = ByteArrayUtil.bytesToHex(doc.getAuditLogId());
                    if (!progressDocsMap.containsKey(auditLogIdHex)) {
                        progressDocsMap.put(auditLogIdHex, new ArrayList<>());
                    }
                    Map<String, Object> docMap = new HashMap<>();
                    docMap.put("id", ByteArrayUtil.bytesToHex(doc.getId()));
                    docMap.put("stepId", ByteArrayUtil.bytesToHex(doc.getStepId()));
                    docMap.put("ticketId", ByteArrayUtil.bytesToHex(doc.getTicketId()));
                    docMap.put("auditLogId", auditLogIdHex);
                    docMap.put("fileName", doc.getFileName());
                    docMap.put("filePath", doc.getFilePath());
                    docMap.put("fileSize", doc.getFileSize());
                    docMap.put("fileType", doc.getFileType());
                    docMap.put("uploadedBy", ByteArrayUtil.bytesToHex(doc.getUploadedBy()));
                    docMap.put("uploadedAt", doc.getUploadedAt());
                    docMap.put("isDeleted", doc.isDeleted());
                    if (doc.getDeletedAt() != null) {
                        docMap.put("deletedAt", doc.getDeletedAt());
                        docMap.put("deletedBy", ByteArrayUtil.bytesToHex(doc.getDeletedBy()));
                        docMap.put("deleteReason", doc.getDeleteReason());
                    }
                    progressDocsMap.get(auditLogIdHex).add(docMap);
                }
            }

            List<Map<String, Object>> historyEntries = new ArrayList<>();
            logger.debug("Processing {} audit logs to build history entries", auditLogs.size());

            for (AuditLog auditLog : auditLogs) {
                String auditLogIdHex = ByteArrayUtil.bytesToHex(auditLog.getId());
                User performedByUser = userMap.get(ByteArrayUtil.bytesToHex(auditLog.getPerformedBy()));
                String userName = performedByUser != null ? performedByUser.getName() : "Unknown User";
                String userRole = performedByUser != null ? performedByUser.getRole() : "unknown";

                Map<String, Object> baseEntry = new HashMap<>();
                baseEntry.put("id", auditLogIdHex);
                baseEntry.put("timestamp", auditLog.getPerformedAt());
                baseEntry.put("userId", ByteArrayUtil.bytesToHex(auditLog.getPerformedBy()));
                baseEntry.put("userName", userName);
                baseEntry.put("userRole", userRole);
                String metadataComment = extractCommentFromMetadata(auditLog.getMetadata());
                baseEntry.put("comment", metadataComment != null ? metadataComment : auditLog.getDescription());
                baseEntry.put("auditLogId", auditLogIdHex);
                baseEntry.put("metadata", auditLog.getMetadata());

                String action = auditLog.getAction();
                String actionCategory = auditLog.getActionCategory();
                logger.debug("Processing audit log {} - action: {}, category: {}", auditLogIdHex, action, actionCategory);

                if ("PROGRESS_DOCUMENTS_UPLOADED".equals(action) ||
                    ("document_action".equals(actionCategory) && action != null && action.contains("PROGRESS"))) {
                    List<Map<String, Object>> docs = progressDocsMap.getOrDefault(auditLogIdHex, new ArrayList<>());
                    logger.debug("PROGRESS_DOCUMENTS_UPLOADED entry - docs count: {}, has description: {}",
                        docs.size(), auditLog.getDescription() != null);
                    if (!docs.isEmpty() || auditLog.getDescription() != null) {
                        Map<String, Object> entry = new HashMap<>(baseEntry);
                        entry.put("type", "progress_update");
                        entry.put("documents", docs);
                        Integer progress = extractProgressFromMetadata(auditLog.getMetadata());
                        if (progress != null) {
                            entry.put("progress", progress);
                        }
                        historyEntries.add(entry);
                        logger.debug("Added PROGRESS_DOCUMENTS_UPLOADED entry to history");
                    } else {
                        logger.debug("Skipped PROGRESS_DOCUMENTS_UPLOADED entry - no docs and no description");
                    }
                } else if ("WORKFLOW_UPDATED".equals(action)) {
                    Integer progress = null;
                    Integer oldProgress = null;

                    if (auditLog.getNewData() != null && !auditLog.getNewData().isEmpty()) {
                        try {
                            progress = Integer.parseInt(auditLog.getNewData().trim());
                        } catch (NumberFormatException e) {
                            progress = extractProgressFromMetadata(auditLog.getMetadata());
                        }
                    } else {
                        progress = extractProgressFromMetadata(auditLog.getMetadata());
                    }

                    if (auditLog.getOldData() != null && !auditLog.getOldData().isEmpty()) {
                        try {
                            oldProgress = Integer.parseInt(auditLog.getOldData().trim());
                        } catch (NumberFormatException e) {
                        }
                    }

                    logger.debug("WORKFLOW_UPDATED entry - progress: {}, oldProgress: {}, description: {}, metadata: {}",
                        progress, oldProgress, auditLog.getDescription(), auditLog.getMetadata());

                    Map<String, Object> entry = new HashMap<>(baseEntry);
                    entry.put("type", "progress_update");
                    if (progress != null) {
                        entry.put("progress", progress);
                    }
                    if (oldProgress != null) {
                        entry.put("oldProgress", oldProgress);
                    }
                    List<Map<String, Object>> docs = progressDocsMap.getOrDefault(auditLogIdHex, new ArrayList<>());
                    if (!docs.isEmpty()) {
                        entry.put("documents", docs);
                    }
                    historyEntries.add(entry);
                    logger.debug("Added WORKFLOW_UPDATED entry to history (progress: {}, has docs: {})",
                        progress, !docs.isEmpty());
                } else if ("STATUS_CHANGED".equals(action) || "status_change".equals(actionCategory)) {
                    logger.debug("STATUS_CHANGED entry - new status: {}, old status: {}",
                        auditLog.getNewData(), auditLog.getOldData());
                    Map<String, Object> entry = new HashMap<>(baseEntry);
                    entry.put("type", "status_change");
                    entry.put("status", auditLog.getNewData());
                    entry.put("oldStatus", auditLog.getOldData());
                    historyEntries.add(entry);
                    logger.debug("Added STATUS_CHANGED entry to history");
                } else {
                    logger.debug("Skipped audit log - action '{}' not recognized for progress history", action);
                }
            }

            logger.debug("Finished processing audit logs. Total history entries so far: {}", historyEntries.size());

            int completionCertCount = 0;
            for (Document cert : allDocuments) {
                if (cert.isCompletionCertificate()) {
                    completionCertCount++;
                    User uploadedByUser = userMap.get(ByteArrayUtil.bytesToHex(cert.getUploadedBy()));
                    String userName = uploadedByUser != null ? uploadedByUser.getName() : "Unknown User";
                    String userRole = uploadedByUser != null ? uploadedByUser.getRole() : "unknown";

                    Map<String, Object> entry = new HashMap<>();
                    entry.put("id", ByteArrayUtil.bytesToHex(cert.getId()));
                    entry.put("type", "completion_certificate");
                    entry.put("timestamp", cert.getUploadedAt());
                    entry.put("userId", ByteArrayUtil.bytesToHex(cert.getUploadedBy()));
                    entry.put("userName", userName);
                    entry.put("userRole", userRole);

                    List<Map<String, Object>> completionCerts = new ArrayList<>();
                    Map<String, Object> certMap = new HashMap<>();
                    certMap.put("id", ByteArrayUtil.bytesToHex(cert.getId()));
                    certMap.put("name", cert.getName());
                    certMap.put("type", cert.getType());
                    certMap.put("size", cert.getSize());
                    certMap.put("url", cert.getUrl());
                    certMap.put("storagePath", cert.getStoragePath());
                    certMap.put("uploadedBy", ByteArrayUtil.bytesToHex(cert.getUploadedBy()));
                    certMap.put("uploadedAt", cert.getUploadedAt());
                    certMap.put("isMandatory", cert.isMandatory());
                    certMap.put("isCompletionCertificate", cert.isCompletionCertificate());
                    certMap.put("stepId", ByteArrayUtil.bytesToHex(cert.getStepId()));
                    completionCerts.add(certMap);

                    entry.put("completionCertificates", completionCerts);
                    historyEntries.add(entry);
                    logger.debug("Added completion certificate entry to history");
                }
            }
            logger.debug("Processed {} completion certificates", completionCertCount);

            historyEntries.sort((a, b) -> {
                Timestamp tsA = (Timestamp) a.get("timestamp");
                Timestamp tsB = (Timestamp) b.get("timestamp");
                return tsB.compareTo(tsA);
            });

            logger.debug("=== Sending {} total history entries in response ===", historyEntries.size());
            sendJsonResponse(response, historyEntries);
        } catch (Exception e) {
            logger.error("Error fetching progress history", e);
            throw new TicketTrackerException("Failed to fetch progress history", e);
        }
    }

    private String extractCommentFromMetadata(String metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            for (String key : new String[]{"\"comment\"", "\"remarks\""}) {
                if (metadata.contains(key)) {
                    int startIdx = metadata.indexOf(key);
                    int colonIdx = metadata.indexOf(":", startIdx);
                    if (colonIdx != -1) {
                        int quoteStart = metadata.indexOf("\"", colonIdx + 1);
                        if (quoteStart != -1) {
                            int quoteEnd = metadata.indexOf("\"", quoteStart + 1);
                            if (quoteEnd != -1) {
                                return metadata.substring(quoteStart + 1, quoteEnd);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract comment from metadata: {}", metadata);
        }
        return null;
    }

    private Integer extractProgressFromMetadata(String metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            if (metadata.contains("\"progress\"")) {
                int startIdx = metadata.indexOf("\"progress\"");
                int colonIdx = metadata.indexOf(":", startIdx);
                if (colonIdx != -1) {
                    int endIdx = metadata.indexOf(",", colonIdx);
                    if (endIdx == -1) {
                        endIdx = metadata.indexOf("}", colonIdx);
                    }
                    if (endIdx != -1) {
                        String progressStr = metadata.substring(colonIdx + 1, endIdx).trim();
                        return Integer.parseInt(progressStr);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract progress from metadata: {}", metadata);
        }
        return null;
    }

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("currentUser");
        }
        return null;
    }

    private String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder buffer = new StringBuilder();
        String line;
        while ((line = request.getReader().readLine()) != null) {
            buffer.append(line);
        }
        return buffer.toString();
    }

    private void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", true);
        apiResponse.put("data", data);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                new ErrorResponse(status, message));
    }

    private void handleException(HttpServletResponse response, TicketTrackerException e)
            throws IOException {
        logger.error("Application error: {}", e.getMessage());
        sendError(response, e.getHttpStatus(), e.getMessage());
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
