package com.tickettracker.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ResponseUtil {

    private static final Logger logger = LoggerFactory.getLogger(ResponseUtil.class);
    private static final ObjectMapper objectMapper = JsonUtil.getObjectMapper();

    public static void sendJsonResponse(HttpServletResponse response, int status, Object data)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), data);
    }

    public static void sendJsonResponse(HttpServletResponse response, Object data)
            throws IOException {
        sendJsonResponse(response, HttpServletResponse.SC_OK, data);
    }

    public static void sendSuccessResponse(HttpServletResponse response, String message)
            throws IOException {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        responseData.put("message", message);
        sendJsonResponse(response, responseData);
    }

    public static void sendSuccessResponse(HttpServletResponse response, String message, Object data)
            throws IOException {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        responseData.put("message", message);
        responseData.put("data", data);
        sendJsonResponse(response, responseData);
    }

    public static void sendErrorResponse(HttpServletResponse response, int status, String message)
            throws IOException {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("success", false);
        errorData.put("status", status);
        errorData.put("message", message);
        sendJsonResponse(response, status, errorData);
    }

    public static void sendErrorResponse(HttpServletResponse response, int status,
                                          String message, String errorCode) throws IOException {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("success", false);
        errorData.put("status", status);
        errorData.put("message", message);
        errorData.put("errorCode", errorCode);
        sendJsonResponse(response, status, errorData);
    }

    public static void sendValidationErrors(HttpServletResponse response, Map<String, String> errors)
            throws IOException {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("success", false);
        errorData.put("status", HttpServletResponse.SC_BAD_REQUEST);
        errorData.put("message", "Validation failed");
        errorData.put("errors", errors);
        sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, errorData);
    }

    public static void sendBadRequest(HttpServletResponse response, String message)
            throws IOException {
        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, message);
    }

    public static void sendUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, message);
    }

    public static void sendForbidden(HttpServletResponse response, String message)
            throws IOException {
        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, message);
    }

    public static void sendNotFound(HttpServletResponse response, String message)
            throws IOException {
        sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, message);
    }

    public static void sendInternalError(HttpServletResponse response, String message)
            throws IOException {
        sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
    }

    public static void logAndSendInternalError(HttpServletResponse response, String message,
                                                Exception e) throws IOException {
        logger.error(message, e);
        sendInternalError(response, message);
    }
}
