package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.dao.FieldDropdownOptionDAO.DropdownOption;
import com.tickettracker.dao.ModuleFieldConfigurationDAO.FieldConfig;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.User;
import com.tickettracker.service.FieldConfigService;
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

@WebServlet("/api/field-config/*")
public class FieldConfigServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(FieldConfigServlet.class);
    private FieldConfigService fieldConfigService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.fieldConfigService = new FieldConfigService();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User currentUser = getCurrentUser(request);
            if (currentUser == null) {
                sendError(response, 401, "Authentication required");
                return;
            }

            String pathInfo = request.getPathInfo();
            String moduleIdStr = request.getParameter("moduleId");
            String context = request.getParameter("context");

            if (pathInfo != null && pathInfo.startsWith("/options/")) {
                String configIdStr = pathInfo.substring("/options/".length());
                byte[] configId = UuidUtil.uuidStringToBytes(configIdStr);
                List<DropdownOption> options = fieldConfigService.getDropdownOptions(configId);
                sendJsonResponse(response, options);
                return;
            }

            if (moduleIdStr != null) {
                byte[] moduleId = UuidUtil.uuidStringToBytes(moduleIdStr);
                List<FieldConfig> configs = fieldConfigService.getConfigurationsByModule(moduleId, context);
                sendJsonResponse(response, configs);
                return;
            }

            Map<String, Object> config = fieldConfigService.getFieldConfiguration(
                    request.getParameter("entityType"), currentUser.getId());
            sendJsonResponse(response, config);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/field-config", e);
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

            if (pathInfo != null && pathInfo.equals("/options")) {
                DropdownOption option = objectMapper.readValue(request.getInputStream(), DropdownOption.class);
                DropdownOption created = fieldConfigService.createDropdownOption(option, currentUser);
                sendJsonResponse(response, created);
                return;
            }

            if (pathInfo != null && pathInfo.equals("/reorder")) {
                @SuppressWarnings("unchecked")
                List<String> orderedIdStrs = objectMapper.readValue(request.getInputStream(), List.class);
                List<byte[]> orderedIds = new java.util.ArrayList<>();
                for (String idStr : orderedIdStrs) {
                    orderedIds.add(UuidUtil.uuidStringToBytes(idStr));
                }
                String moduleIdStr = request.getParameter("moduleId");
                byte[] moduleId = UuidUtil.uuidStringToBytes(moduleIdStr);
                fieldConfigService.reorderFields(moduleId, orderedIds, currentUser);
                sendJsonResponse(response, Map.of("success", true));
                return;
            }

            FieldConfig config = objectMapper.readValue(request.getInputStream(), FieldConfig.class);
            FieldConfig created = fieldConfigService.createConfiguration(config, currentUser);
            sendJsonResponse(response, created);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in POST /api/field-config", e);
            sendError(response, 500, "Internal server error");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User currentUser = getCurrentUser(request);
            if (currentUser == null) {
                sendError(response, 401, "Authentication required");
                return;
            }

            String pathInfo = request.getPathInfo();

            if (pathInfo != null && pathInfo.startsWith("/options/")) {
                String optionIdStr = pathInfo.substring("/options/".length());
                DropdownOption option = objectMapper.readValue(request.getInputStream(), DropdownOption.class);
                option.setId(UuidUtil.uuidStringToBytes(optionIdStr));
                fieldConfigService.updateDropdownOption(option, currentUser);
                sendJsonResponse(response, Map.of("success", true));
                return;
            }

            FieldConfig config = objectMapper.readValue(request.getInputStream(), FieldConfig.class);
            if (pathInfo != null && pathInfo.length() > 1) {
                config.setId(UuidUtil.uuidStringToBytes(pathInfo.substring(1)));
            }
            FieldConfig updated = fieldConfigService.updateConfiguration(config, currentUser);
            sendJsonResponse(response, updated);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in PUT /api/field-config", e);
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

            if (pathInfo != null && pathInfo.startsWith("/options/")) {
                String optionIdStr = pathInfo.substring("/options/".length());
                fieldConfigService.deleteDropdownOption(UuidUtil.uuidStringToBytes(optionIdStr), currentUser);
                sendJsonResponse(response, Map.of("success", true));
                return;
            }

            if (pathInfo != null && pathInfo.length() > 1) {
                String configIdStr = pathInfo.substring(1);
                fieldConfigService.deleteConfiguration(UuidUtil.uuidStringToBytes(configIdStr), currentUser);
                sendJsonResponse(response, Map.of("success", true));
                return;
            }

            sendError(response, 400, "Configuration ID required");

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in DELETE /api/field-config", e);
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
