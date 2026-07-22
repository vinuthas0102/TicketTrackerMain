package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.User;
import com.tickettracker.service.FieldValueService;
import com.tickettracker.util.JsonUtil;
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
import java.util.Map;

@WebServlet("/api/field-values/*")
public class FieldValueServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(FieldValueServlet.class);
    private FieldValueService fieldValueService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.fieldValueService = new FieldValueService();
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

            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Entity ID required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length == 2) {
                byte[] entityId = UuidUtil.uuidStringToBytes(pathParts[1]);
                Map<String, Object> values = fieldValueService.getFieldValues(
                        entityId, currentUser.getId());
                sendJsonResponse(response, values);
            } else {
                sendError(response, 400, "Invalid request path");
            }

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/field-values", e);
            sendError(response, 500, "Internal server error");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User currentUser = getCurrentUser(request);
            if (currentUser == null) {
                sendError(response, 401, "Authentication required");
                return;
            }

            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Entity ID required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length == 2) {
                byte[] entityId = UuidUtil.uuidStringToBytes(pathParts[1]);
                Map<String, Object> values = objectMapper.readValue(request.getInputStream(), Map.class);
                fieldValueService.saveFieldValues(entityId, values, currentUser.getId());
                Map<String, Object> result = fieldValueService.getFieldValues(entityId, currentUser.getId());
                sendJsonResponse(response, result);
            } else {
                sendError(response, 400, "Invalid request path");
            }

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in POST /api/field-values", e);
            sendError(response, 500, "Internal server error");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User currentUser = getCurrentUser(request);
            if (currentUser == null) {
                sendError(response, 401, "Authentication required");
                return;
            }

            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Entity ID and field key required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length == 3) {
                byte[] entityId = UuidUtil.uuidStringToBytes(pathParts[1]);
                String fieldKey = pathParts[2];
                fieldValueService.deleteTicketFieldValue(entityId, fieldKey);
                sendJsonResponse(response, Map.of("success", true));
            } else {
                sendError(response, 400, "Invalid request path. Expected /{entityId}/{fieldKey}");
            }

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in DELETE /api/field-values", e);
            sendError(response, 500, "Internal server error");
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

        public int getStatus() { return status; }
        public String getMessage() { return message; }
    }
}
