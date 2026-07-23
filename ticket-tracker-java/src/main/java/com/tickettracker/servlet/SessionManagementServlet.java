package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/auth/csrf-token")
public class SessionManagementServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(SessionManagementServlet.class);
    private static final String CSRF_TOKEN_ATTRIBUTE = "CSRF_TOKEN";
    private static final int TOKEN_LENGTH = 32;
    private static final SecureRandom secureRandom = new SecureRandom();
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            HttpSession session = request.getSession(true);

            String csrfToken = (String) session.getAttribute(CSRF_TOKEN_ATTRIBUTE);
            if (csrfToken == null) {
                csrfToken = generateCSRFToken();
                session.setAttribute(CSRF_TOKEN_ATTRIBUTE, csrfToken);
            }

            User currentUser = (User) session.getAttribute("currentUser");

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("csrfToken", csrfToken);
            responseData.put("sessionId", session.getId());
            responseData.put("maxInactiveInterval", session.getMaxInactiveInterval());
            responseData.put("creationTime", session.getCreationTime());
            responseData.put("lastAccessedTime", session.getLastAccessedTime());
            responseData.put("authenticated", currentUser != null);

            sendJsonResponse(response, responseData);

            logger.debug("CSRF token provided for session: {}", session.getId());

        } catch (Exception e) {
            logger.error("Error generating CSRF token", e);
            sendError(response, 500, "Internal server error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/refresh")) {
            handleSessionRefresh(request, response);
        } else {
            sendError(response, 404, "Endpoint not found");
        }
    }

    private void handleSessionRefresh(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                sendError(response, 401, "No active session");
                return;
            }

            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                sendError(response, 401, "Authentication required");
                return;
            }

            session.setMaxInactiveInterval(1800);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Session refreshed successfully");
            responseData.put("sessionId", session.getId());
            responseData.put("maxInactiveInterval", session.getMaxInactiveInterval());
            responseData.put("lastAccessedTime", session.getLastAccessedTime());
            responseData.put("expiresAt", System.currentTimeMillis() + (session.getMaxInactiveInterval() * 1000L));

            sendJsonResponse(response, responseData);

            logger.info("Session refreshed for user: {}", currentUser.getEmail());

        } catch (Exception e) {
            logger.error("Error refreshing session", e);
            sendError(response, 500, "Failed to refresh session");
        }
    }

    private String generateCSRFToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        ResponseUtil.sendWrappedSuccess(response, data);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        ResponseUtil.sendWrappedError(response, status, message);
    }
}
