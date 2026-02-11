package com.tickettracker.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebFilter(urlPatterns = {"/api/*"})
public class RateLimitFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int DEFAULT_LIMIT = 100;
    private static final int LOGIN_LIMIT = 5;
    private static final int FILE_UPLOAD_LIMIT = 10;
    private static final long WINDOW_SIZE_MINUTES = 1;

    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService cleanupScheduler;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Rate Limit Filter initialized");

        cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        cleanupScheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredEntries();
            } catch (Exception e) {
                logger.error("Error during rate limit cleanup", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientKey = getClientKey(httpRequest);
        String path = httpRequest.getRequestURI();
        int limit = getRateLimitForPath(path);

        RateLimitInfo limitInfo = rateLimitMap.computeIfAbsent(clientKey, k -> new RateLimitInfo());

        if (!limitInfo.allowRequest(limit)) {
            long resetTime = limitInfo.getResetTime();
            logger.warn("Rate limit exceeded for client: {} on path: {} (limit: {})",
                    clientKey, path, limit);

            httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            httpResponse.setHeader("X-RateLimit-Remaining", "0");
            httpResponse.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));
            httpResponse.setHeader("Retry-After", String.valueOf((resetTime - System.currentTimeMillis()) / 1000));

            sendErrorResponse(httpResponse, 429, "Too many requests. Please try again later.");
            return;
        }

        httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(limitInfo.getRemainingRequests(limit)));
        httpResponse.setHeader("X-RateLimit-Reset", String.valueOf(limitInfo.getResetTime()));

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        if (cleanupScheduler != null) {
            cleanupScheduler.shutdown();
            try {
                if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Rate Limit Filter destroyed");
    }

    private String getClientKey(HttpServletRequest request) {
        Object userId = request.getSession(false) != null
                ? request.getSession(false).getAttribute("userId")
                : null;

        if (userId != null) {
            return "user:" + userId.toString();
        }

        return "ip:" + request.getRemoteAddr();
    }

    private int getRateLimitForPath(String path) {
        if (path.contains("/auth/login")) {
            return LOGIN_LIMIT;
        } else if (path.contains("/files") || path.contains("/upload") || path.contains("/documents")) {
            return FILE_UPLOAD_LIMIT;
        }
        return DEFAULT_LIMIT;
    }

    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        rateLimitMap.entrySet().removeIf(entry -> {
            RateLimitInfo info = entry.getValue();
            return now > info.getResetTime() && info.getRequestCount() == 0;
        });

        logger.debug("Rate limit cleanup completed. Active entries: {}", rateLimitMap.size());
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> error = new HashMap<>();
        error.put("status", status);
        error.put("message", message);
        error.put("error", "RATE_LIMIT_EXCEEDED");

        new ObjectMapper().writeValue(response.getWriter(), error);
    }

    private static class RateLimitInfo {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private volatile long windowStart;

        public RateLimitInfo() {
            this.windowStart = System.currentTimeMillis();
        }

        public synchronized boolean allowRequest(int limit) {
            long now = System.currentTimeMillis();
            long windowEnd = windowStart + TimeUnit.MINUTES.toMillis(WINDOW_SIZE_MINUTES);

            if (now > windowEnd) {
                windowStart = now;
                requestCount.set(0);
            }

            int current = requestCount.incrementAndGet();
            return current <= limit;
        }

        public int getRequestCount() {
            return requestCount.get();
        }

        public int getRemainingRequests(int limit) {
            return Math.max(0, limit - requestCount.get());
        }

        public long getResetTime() {
            return windowStart + TimeUnit.MINUTES.toMillis(WINDOW_SIZE_MINUTES);
        }
    }
}
