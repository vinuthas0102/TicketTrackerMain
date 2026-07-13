package com.tickettracker.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/*")
public class CorsFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CorsFilter.class);
    private String allowedOrigins;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Read allowed origins from environment variable or system property
        // Supports comma-separated list of origins
        allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
            allowedOrigins = System.getProperty("cors.allowed.origins");
        }
        if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
            allowedOrigins = "http://localhost:3000";
        }
        logger.info("CorsFilter initialized with allowed origins: {}", allowedOrigins);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String origin = httpRequest.getHeader("Origin");

        // Check if origin is in allowed list
        if (origin != null && isOriginAllowed(origin)) {
        	httpResponse.setHeader("Access-Control-Allow-Origin", origin);
        }

        httpResponse.setHeader("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        httpResponse.setHeader("Access-Control-Allow-Headers",
                "Content-Type, Authorization, X-CSRF-Token, X-Requested-With, Accept, Origin");
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        httpResponse.setHeader("Access-Control-Max-Age", "3600");

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("CorsFilter destroyed");
    }

    private boolean isOriginAllowed(String origin) {
        if (allowedOrigins == null || origin == null) {
            return false;
        }

        String[] allowedList = allowedOrigins.split(",");
        for (String allowed : allowedList) {
            if (origin.trim().equalsIgnoreCase(allowed.trim())) {
                return true;
            }
        }
        return false;
    }
}
