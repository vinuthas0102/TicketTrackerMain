package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.User;
import com.tickettracker.model.WorkflowStepFileReference;
import com.tickettracker.service.FileReferenceService;
import com.tickettracker.util.JsonUtil;
import com.tickettracker.util.ResponseUtil;
import com.tickettracker.util.UuidUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/file-references/*")
public class FileReferenceServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(FileReferenceServlet.class);
    private FileReferenceService fileReferenceService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.fileReferenceService = new FileReferenceService();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            User currentUser = getCurrentUser(request);
            if (currentUser == null) {
                sendError(response, 401, "Authentication required");
                return;
            }

            // Handle path-based routing: /step/{stepId}
            if (pathInfo != null && pathInfo.startsWith("/step/")) {
                handleGetStepFileReferences(request, response, pathInfo);
                return;
            }

            // Handle query parameter routing: ?entityId=...
            String entityIdParam = request.getParameter("entityId");
            if (entityIdParam == null) {
                sendError(response, 400, "entityId parameter required or use /step/{stepId} path");
                return;
            }

            byte[] entityId = hexToBytes(entityIdParam);
            List<Map<String, Object>> references = fileReferenceService.getFileReferences(
                    entityId, currentUser.getId());

            sendJsonResponse(response, references);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/file-references", e);
            sendError(response, 500, "Internal server error");
        }
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

            // Handle PUT /api/file-references/{referenceId}/status
            if (pathInfo != null && pathInfo.matches("^/[^/]+/status$")) {
                String[] pathParts = pathInfo.split("/");
                String referenceIdStr = pathParts[1];

                byte[] referenceId;
                try {
                    referenceId = UuidUtil.uuidStringToBytes(referenceIdStr);
                } catch (Exception e) {
                    logger.warn("Invalid UUID format for referenceId: {}", referenceIdStr);
                    sendError(response, 400, "Invalid UUID format for referenceId");
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> body = objectMapper.readValue(request.getInputStream(), Map.class);

                Map<String, Object> updates = new HashMap<>();
                if (body.containsKey("documentId")) {
                    updates.put("documentId", body.get("documentId"));
                }
                if (body.containsKey("uploadedBy")) {
                    updates.put("uploadedBy", body.get("uploadedBy"));
                }
                if (body.containsKey("uploadedAt")) {
                    updates.put("uploadedAt", body.get("uploadedAt"));
                }

                logger.debug("Updating file reference {} with keys: {}", referenceIdStr, updates.keySet());

                WorkflowStepFileReference updated = fileReferenceService.updateStepFileReference(
                        referenceId, updates, currentUser.getId());

                sendJsonResponse(response, updated);
                return;
            }

            sendError(response, 400, "Invalid path. Expected: /api/file-references/{id}/status");

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in PUT /api/file-references", e);
            sendError(response, 500, "Internal server error");
        }
    }

    private void handleGetStepFileReferences(HttpServletRequest request, HttpServletResponse response, String pathInfo)
            throws IOException {
        try {
            // Extract stepId from path: /step/{stepId}
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 3) {
                sendError(response, 400, "Invalid path format. Expected: /step/{stepId}");
                return;
            }

            String stepIdStr = pathParts[2];
            if (stepIdStr == null || stepIdStr.trim().isEmpty()) {
                sendError(response, 400, "Step ID cannot be empty");
                return;
            }

            logger.debug("Fetching file references for step: {}", stepIdStr);

            // Convert UUID string to byte array
            byte[] stepId;
            try {
                stepId = UuidUtil.uuidStringToBytes(stepIdStr);
            } catch (Exception e) {
                logger.warn("Invalid UUID format for stepId: {}", stepIdStr);
                sendError(response, 400, "Invalid UUID format for stepId");
                return;
            }

            // Fetch step file references
            List<WorkflowStepFileReference> references = fileReferenceService.getStepFileReferences(stepId);

            logger.debug("Found {} file references for step: {}", references.size(), stepIdStr);

            // Return the list (empty array if no references)
            sendJsonResponse(response, references);

        } catch (TicketTrackerException e) {
            logger.error("Error fetching step file references: {}", e.getMessage());
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error fetching step file references", e);
            sendError(response, 500, "Internal server error: " + e.getMessage());
        }
    }

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("currentUser");
        }
        return null;
    }

    private void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        ResponseUtil.sendWrappedSuccess(response, data);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        ResponseUtil.sendWrappedError(response, status, message);
    }

    private void handleException(HttpServletResponse response, TicketTrackerException e)
            throws IOException {
        logger.error("Application error: {}", e.getMessage());
        sendError(response, e.getHttpStatus(), e.getMessage());
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

}
