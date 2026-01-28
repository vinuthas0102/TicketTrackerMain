package com.tickettracker.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.model.User;
import com.tickettracker.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@WebFilter("/api/*")
public class AuthenticationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    private ObjectMapper objectMapper;

    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/api/auth/login",
            "/api/auth/register"
    ));

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.objectMapper = JsonUtil.getObjectMapper();
        logger.info("AuthenticationFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestPath = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = requestPath.substring(contextPath.length());

        logger.debug("Authentication filter processing: {}", path);

        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        User currentUser = null;

        if (session != null) {
            currentUser = (User) session.getAttribute("currentUser");
        }

        if (currentUser == null) {
            logger.warn("Unauthorized access attempt to: {}", path);
            sendUnauthorizedError(httpResponse, "Authentication required");
            return;
        }

        logger.debug("Authenticated user: {} accessing: {}", currentUser.getEmail(), path);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("AuthenticationFilter destroyed");
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    private void sendUnauthorizedError(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse error = new ErrorResponse(401, message);
        objectMapper.writeValue(response.getWriter(), error);
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
