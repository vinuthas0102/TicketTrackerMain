package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.User;
import com.tickettracker.service.AuthService;
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
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(AuthServlet.class);
    private AuthService authService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.authService = new AuthService();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(response, 400, "Invalid auth endpoint");
            return;
        }

        try {
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                sendError(response, 400, "Invalid auth endpoint");
                return;
            }

            String action = pathParts[1];

            switch (action) {
                case "login":
                    handleLogin(request, response);
                    break;
                case "logout":
                    handleLogout(request, response);
                    break;
                case "register":
                    handleRegister(request, response);
                    break;
                case "change-password":
                    handleChangePassword(request, response);
                    break;
                default:
                    sendError(response, 404, "Auth endpoint not found");
            }

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in auth endpoint", e);
            sendError(response, 500, "Internal server error");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/current")) {
            handleGetCurrentUser(request, response);
        } else {
            sendError(response, 404, "Endpoint not found");
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException, TicketTrackerException {
        String body = getRequestBody(request);
        Map<String, String> credentials = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));

        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email == null || password == null) {
            sendError(response, 400, "Email and password are required");
            return;
        }

        User user = authService.authenticate(email, password);

        HttpSession session = request.getSession(true);
        session.setAttribute("currentUser", user);
        session.setAttribute("userId", user.getId());
        session.setMaxInactiveInterval(3600);

        Map<String, Object> loginResponse = new HashMap<>();
        loginResponse.put("user", sanitizeUser(user));
        loginResponse.put("message", "Login successful");

        sendJsonResponse(response, loginResponse);

        logger.info("User logged in: {}", email);
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("currentUser");
            if (user != null) {
                logger.info("User logged out: {}", user.getEmail());
            }
            session.invalidate();
        }

        Map<String, String> logoutResponse = new HashMap<>();
        logoutResponse.put("message", "Logout successful");

        sendJsonResponse(response, logoutResponse);
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response)
            throws IOException, TicketTrackerException {
        String body = getRequestBody(request);
        Map<String, Object> registrationData = objectMapper.readValue(body, Map.class);

        User user = new User();
        user.setName((String) registrationData.get("name"));
        user.setEmail((String) registrationData.get("email"));
        user.setRole((String) registrationData.get("role"));
        user.setDepartment((String) registrationData.get("department"));

        String password = (String) registrationData.get("password");

        User createdUser = authService.register(user, password);

        response.setStatus(HttpServletResponse.SC_CREATED);
        sendJsonResponse(response, sanitizeUser(createdUser));

        logger.info("User registered: {}", createdUser.getEmail());
    }

    private void handleChangePassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException, TicketTrackerException {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            sendError(response, 401, "Authentication required");
            return;
        }

        String body = getRequestBody(request);
        Map<String, String> passwordData = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));

        String currentPassword = passwordData.get("currentPassword");
        String newPassword = passwordData.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            sendError(response, 400, "Current and new passwords are required");
            return;
        }

        authService.changePassword(currentUser.getId(), currentPassword, newPassword);

        Map<String, String> changeResponse = new HashMap<>();
        changeResponse.put("message", "Password changed successfully");

        sendJsonResponse(response, changeResponse);

        logger.info("Password changed for user: {}", currentUser.getEmail());
    }

    private void handleGetCurrentUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            sendError(response, 401, "Not authenticated");
            return;
        }

        sendJsonResponse(response, sanitizeUser(currentUser));
    }

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("currentUser");
        }
        return null;
    }

    private Map<String, Object> sanitizeUser(User user) {
        Map<String, Object> sanitized = new HashMap<>();
        sanitized.put("id", bytesToHex(user.getId()));
        sanitized.put("name", user.getName());
        sanitized.put("email", user.getEmail());
        sanitized.put("role", user.getRole());
        sanitized.put("department", user.getDepartment());
        sanitized.put("active", user.isActive());
        return sanitized;
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

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
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
