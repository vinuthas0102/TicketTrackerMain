package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.exception.ValidationException;
import com.tickettracker.model.User;
import com.tickettracker.service.MasterDataService;
import com.tickettracker.util.JsonUtil;
import com.tickettracker.util.ResponseUtil;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/master-data/*")
public class MasterDataServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(MasterDataServlet.class);
    private MasterDataService masterDataService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.masterDataService = new MasterDataService();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            User currentUser = getCurrentUser(request);
            if (currentUser == null) {
                sendError(response, 401, "Authentication required");
                return;
            }

            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Path required: /categories, /departments, /locations, /config");
                return;
            }

            String[] parts = pathInfo.split("/");

            if (parts.length >= 2) {
                String type = parts[1];

                if ("config".equals(type)) {
                    String key = request.getParameter("key");
                    if (key == null || key.isEmpty()) {
                        sendError(response, 400, "key parameter required for config");
                        return;
                    }
                    String value = masterDataService.getConfigValue(key);
                    Map<String, Object> result = new HashMap<>();
                    result.put("key", key);
                    result.put("value", value);
                    sendJsonResponse(response, result);
                    return;
                }

                if ("module-codes".equals(type)) {
                    String moduleIdParam = request.getParameter("moduleId");
                    if (moduleIdParam == null || moduleIdParam.isEmpty()) {
                        sendError(response, 400, "moduleId parameter required");
                        return;
                    }
                    byte[] moduleId = UuidUtil.uuidStringToBytes(moduleIdParam);
                    String moduleCode = masterDataService.getModuleCode(moduleId);
                    Map<String, Object> result = new HashMap<>();
                    result.put("moduleId", moduleIdParam);
                    result.put("moduleCode", moduleCode);
                    sendJsonResponse(response, result);
                    return;
                }

                List<MasterDataService_item> items = convertItems(masterDataService.getAll(type));
                sendJsonResponse(response, items);
                return;
            }

            sendError(response, 400, "Invalid path");
        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/master-data", e);
            sendError(response, 500, "Internal server error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            User currentUser = getCurrentUser(request);
            if (currentUser == null) {
                sendError(response, 401, "Authentication required");
                return;
            }

            if (!"eo".equalsIgnoreCase(currentUser.getRole())) {
                sendError(response, 403, "Only EO users can manage master data");
                return;
            }

            if (pathInfo == null) {
                sendError(response, 400, "Path required");
                return;
            }

            String[] parts = pathInfo.split("/");

            if (parts.length >= 2) {
                String type = parts[1];

                @SuppressWarnings("unchecked")
                Map<String, Object> body = objectMapper.readValue(request.getInputStream(), Map.class);

                if ("config".equals(type)) {
                    String key = (String) body.get("key");
                    String value = (String) body.get("value");
                    String description = (String) body.get("description");
                    masterDataService.setConfigValue(key, value, description);
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    sendJsonResponse(response, result);
                    return;
                }

                if ("module-codes".equals(type)) {
                    String moduleIdParam = (String) body.get("moduleId");
                    String moduleCode = (String) body.get("moduleCode");
                    byte[] moduleId = UuidUtil.uuidStringToBytes(moduleIdParam);
                    masterDataService.setModuleCode(moduleId, moduleCode);
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    sendJsonResponse(response, result);
                    return;
                }

                if ("generate-ticket-number".equals(type)) {
                    String locationPrefix = (String) body.get("locationPrefix");
                    String moduleCode = (String) body.get("moduleCode");
                    String ticketNumber = masterDataService.generateTicketNumber(locationPrefix, moduleCode);
                    Map<String, Object> result = new HashMap<>();
                    result.put("ticketNumber", ticketNumber);
                    sendJsonResponse(response, result);
                    return;
                }

                String name = (String) body.get("name");
                com.tickettracker.dao.MasterDataDAO.MapItem item = masterDataService.add(type, name);
                sendJsonResponse(response, convertItem(item));
                return;
            }

            sendError(response, 400, "Invalid path");
        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in POST /api/master-data", e);
            sendError(response, 500, "Internal server error: " + e.getMessage());
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

            if (!"eo".equalsIgnoreCase(currentUser.getRole())) {
                sendError(response, 403, "Only EO users can manage master data");
                return;
            }

            if (pathInfo == null) {
                sendError(response, 400, "Path required");
                return;
            }

            String[] parts = pathInfo.split("/");

            if (parts.length >= 3) {
                String type = parts[1];
                String idParam = parts[2];
                byte[] id = UuidUtil.uuidStringToBytes(idParam);
                masterDataService.remove(type, id);
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                sendJsonResponse(response, result);
                return;
            }

            sendError(response, 400, "Invalid path. Expected: /{type}/{id}");
        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in DELETE /api/master-data", e);
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

            if (!"eo".equalsIgnoreCase(currentUser.getRole())) {
                sendError(response, 403, "Only EO users can manage master data");
                return;
            }

            if (pathInfo == null) {
                sendError(response, 400, "Path required");
                return;
            }

            String[] parts = pathInfo.split("/");

            if (parts.length >= 3) {
                String type = parts[1];
                String idParam = parts[2];
                byte[] id = UuidUtil.uuidStringToBytes(idParam);

                @SuppressWarnings("unchecked")
                Map<String, Object> body = objectMapper.readValue(request.getInputStream(), Map.class);
                Boolean isActive = (Boolean) body.get("is_active");
                if (isActive != null) {
                    masterDataService.toggleActive(type, id, isActive);
                }

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                sendJsonResponse(response, result);
                return;
            }

            sendError(response, 400, "Invalid path. Expected: /{type}/{id}");
        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in PUT /api/master-data", e);
            sendError(response, 500, "Internal server error");
        }
    }

    private List<MasterDataService_item> convertItems(List<com.tickettracker.dao.MasterDataDAO.MapItem> items) {
        List<MasterDataService_item> result = new ArrayList<>();
        for (com.tickettracker.dao.MasterDataDAO.MapItem item : items) {
            result.add(convertItem(item));
        }
        return result;
    }

    private MasterDataService_item convertItem(com.tickettracker.dao.MasterDataDAO.MapItem item) {
        MasterDataService_item dto = new MasterDataService_item();
        dto.id = UuidUtil.bytesToUuidString(item.getId());
        dto.name = item.getName();
        dto.is_active = item.isActive();
        dto.display_order = item.getDisplayOrder();
        dto.created_at = item.getCreatedAt() != null ? item.getCreatedAt().toString() : null;
        dto.updated_at = item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : null;
        return dto;
    }

    public static class MasterDataService_item {
        public String id;
        public String name;
        public boolean is_active;
        public int display_order;
        public String created_at;
        public String updated_at;
    }

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("currentUser");
        }
        return null;
    }

    private void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        ResponseUtil.sendWrappedSuccess(response, data);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        ResponseUtil.sendWrappedError(response, status, message);
    }

    private void handleException(HttpServletResponse response, TicketTrackerException e) throws IOException {
        logger.error("Application error: {}", e.getMessage());
        sendError(response, e.getHttpStatus(), e.getMessage());
    }

}
