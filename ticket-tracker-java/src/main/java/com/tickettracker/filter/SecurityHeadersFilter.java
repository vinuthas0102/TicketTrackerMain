package com.tickettracker.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(urlPatterns = {"/*"})
public class SecurityHeadersFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Security Headers Filter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        httpResponse.setHeader("X-Frame-Options", "DENY");

        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: blob:; " +
                "font-src 'self' data:; " +
                "connect-src 'self'; " +
                "frame-ancestors 'none';");

        httpResponse.setHeader("Permissions-Policy",
                "geolocation=(), microphone=(), camera=(), payment=()");

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("Security Headers Filter destroyed");
    }
}
