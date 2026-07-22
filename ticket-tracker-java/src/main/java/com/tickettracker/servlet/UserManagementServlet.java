package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.User;
import com.tickettracker.service.UserService;
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
import java.util.List;
import java.util.Map;

@WebServlet("/api/users/*")
public class UserManagementServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(UserManagementServlet.class);
    private UserService userService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userService = new UserService();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetAllUsers(request, response);
            } else {
                String[] pathParts = pathInfo.split("/");
                if (pathParts.length == 2) {
                    handleGetUser(pathParts[1], response);
                } else if (pathParts.length == 3 && pathParts[2].equals("role")) {
                    handleGetUsersByRole(request, response);
                } else if (pathParts.length == 3 && pathParts[2].equals("filter")) {
                    handleGetUsersByRoleAndDepartment(request, response);
                } else if (pathParts.length == 3 && pathParts[2].equals("activity-logs")) {
                    handleGetActivityLogs(pathParts[1], request, response);
                } else if (pathParts.length == 3 && pathParts[2].equals("audit")) {
                    handleGetManagementAudit(pathParts[1], response);
                } else {
                    sendError(response, 400, "Invalid request path");
                }
            }
        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/users", e);
            sendError(response, 500, "Internal server error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User currentUser = getCurrentUser(request);
            if (currentUser == null) {
                sendError(response, 401, "Authentication required");
                return;
            }

            String pathInfo = request.getPathInfo();

            if (pathInfo != null && pathInfo.startsWith("/reset-password/")) {
                handleResetPassword(pathInfo.substring("/reset-password/".length()), currentUser, response);
                return;
            }

            String body = getRequestBody(request);
            Map<String, Object> userData = objectMapper.readValue(body, Map.class);

            User user = new User();
            user.setName((String) userData.get("name"));
            user.setEmail((String) userData.get("email"));
            user.setRole((String) userData.get("role"));
            user.setDepartment((String) userData.get("department"));

            String password = (String) userData.get("password");

            User createdUser = userService.createUser(user, password, currentUser.getId());

            response.setStatus(HttpServletResponse.SC_CREATED);
            sendJsonResponse(response, createdUser);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in POST /api/users", e);
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

            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "User ID required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }

            String body = getRequestBody(request);
            User user = objectMapper.readValue(body, User.class);

            User updatedUser = userService.updateUser(user, currentUser.getId());

            sendJsonResponse(response, updatedUser);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in PUT /api/users", e);
            sendError(response, 500, "Internal server error");
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

            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "User ID required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }

            byte[] userId = hexToBytes(pathParts[1]);
            userService.deactivateUser(userId, currentUser.getId());

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in DELETE /api/users", e);
            sendError(response, 500, "Internal server error");
        }
    }

    private void handleGetAllUsers(HttpServletRequest request, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        List<User> users = userService.getAllUsers();
        sendJsonResponse(response, users);
    }

    private void handleGetUser(String userId, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        byte[] id = hexToBytes(userId);
        User user = userService.getUserById(id);
        sendJsonResponse(response, user);
    }

    private void handleGetActivityLogs(String userIdStr, HttpServletRequest request, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            sendError(response, 401, "Authentication required");
            return;
        }
        if (!currentUser.isAdmin()) {
            sendError(response, 403, "Only EO can view activity logs");
            return;
        }
        byte[] userId = UuidUtil.uuidStringToBytes(userIdStr);
        String limitStr = request.getParameter("limit");
        int limit = limitStr != null ? Integer.parseInt(limitStr) : 50;
        sendJsonResponse(response, userService.getUserActivityLogs(userId, limit));
    }

    private void handleGetManagementAudit(String userIdStr, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        byte[] userId = UuidUtil.uuidStringToBytes(userIdStr);
        sendJsonResponse(response, userService.getUserManagementAudit(userId));
    }

    private void handleResetPassword(String userIdStr, User currentUser, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        if (!currentUser.isAdmin()) {
            sendError(response, 403, "Only EO can reset passwords");
            return;
        }
        byte[] userId = UuidUtil.uuidStringToBytes(userIdStr);
        String newPassword = userService.resetUserPassword(userId, currentUser.getId());
        sendJsonResponse(response, Map.of("success", true, "newPassword", newPassword));
    }

    private void handleGetUsersByRole(HttpServletRequest request, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        String role = request.getParameter("role");
        if (role == null || role.trim().isEmpty()) {
            sendError(response, 400, "Role parameter required");
            return;
        }

        List<User> users = userService.getUsersByRole(role.trim().toLowerCase());
        sendJsonResponse(response, users);
    }

    private void handleGetUsersByRoleAndDepartment(HttpServletRequest request, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        String role = request.getParameter("role");
        String department = request.getParameter("department");
        if (role == null || role.trim().isEmpty()) {
            sendError(response, 400, "Role parameter required");
            return;
        }
        if (department == null || department.trim().isEmpty()) {
            sendError(response, 400, "Department parameter required");
            return;
        }

        List<User> users = userService.getUsersByRoleAndDepartment(role.trim().toUpperCase(), department.trim());
        sendJsonResponse(response, users);
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
