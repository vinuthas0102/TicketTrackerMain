package com.tickettracker.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@WebFilter(urlPatterns = {"/api/*"})
public class CSRFTokenFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CSRFTokenFilter.class);
    private static final String CSRF_TOKEN_ATTRIBUTE = "CSRF_TOKEN";
    private static final String CSRF_TOKEN_HEADER = "X-CSRF-Token";
    private static final int TOKEN_LENGTH = 32;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("CSRF Token Filter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();
        String path = httpRequest.getRequestURI();

        if (path.startsWith("/api/auth/csrf-token")) {
            handleCSRFTokenRequest(httpRequest, httpResponse);
            return;
        }

        if (requiresCSRFValidation(method, path)) {
            if (!validateCSRFToken(httpRequest)) {
                logger.warn("CSRF validation failed for {} {} from IP: {}",
                        method, path, httpRequest.getRemoteAddr());
                sendErrorResponse(httpResponse, 403, "Invalid or missing CSRF token");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("CSRF Token Filter destroyed");
    }

    private boolean requiresCSRFValidation(String method, String path) {
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return false;
        }

        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/logout")) {
            return false;
        }

        return true;
    }

    private boolean validateCSRFToken(HttpServletRequest request) {
        String tokenFromHeader = request.getHeader(CSRF_TOKEN_HEADER);
        if (tokenFromHeader == null || tokenFromHeader.trim().isEmpty()) {
            return false;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        String tokenFromSession = (String) session.getAttribute(CSRF_TOKEN_ATTRIBUTE);
        if (tokenFromSession == null) {
            return false;
        }

        return tokenFromHeader.equals(tokenFromSession);
    }

    private void handleCSRFTokenRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(true);
        String token = generateCSRFToken();
        session.setAttribute(CSRF_TOKEN_ATTRIBUTE, token);

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("csrfToken", token);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), responseBody);

        logger.debug("Generated CSRF token for session: {}", session.getId());
    }

    private String generateCSRFToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> error = new HashMap<>();
        error.put("status", status);
        error.put("message", message);
        error.put("error", "CSRF_VALIDATION_FAILED");

        new ObjectMapper().writeValue(response.getWriter(), error);
    }
}
