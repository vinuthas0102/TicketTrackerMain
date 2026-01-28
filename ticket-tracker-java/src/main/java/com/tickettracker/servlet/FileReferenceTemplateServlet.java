package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.exception.ResourceNotFoundException;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.exception.ValidationException;
import com.tickettracker.model.FileReferenceTemplate;
import com.tickettracker.model.User;
import com.tickettracker.service.FileReferenceService;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/api/file-reference-templates/*")
public class FileReferenceTemplateServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(FileReferenceTemplateServlet.class);
    private FileReferenceService fileReferenceService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.fileReferenceService = new FileReferenceService();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetAllTemplates(request, response);
            } else {
                String[] pathParts = pathInfo.split("/");
                if (pathParts.length == 2) {
                    handleGetTemplateById(pathParts[1], response);
                } else {
                    sendError(response, 400, "Invalid request path");
                }
            }
        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/file-reference-templates", e);
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

            if (!checkEORole(currentUser)) {
                sendError(response, 403, "Only Engineering Officers can manage templates");
                return;
            }

            String body = getRequestBody(request);
            Map<String, Object> requestData = objectMapper.readValue(body, Map.class);

            String templateName = (String) requestData.get("name");
            if (templateName == null || templateName.trim().isEmpty()) {
                templateName = (String) requestData.get("templateName");
            }

            String description = (String) requestData.get("description");
            Object jsonContentObj = requestData.get("jsonContent");

            if (templateName == null || templateName.trim().isEmpty()) {
                sendError(response, 400, "Template name is required");
                return;
            }

            if (jsonContentObj == null) {
                sendError(response, 400, "JSON content is required");
                return;
            }

            String jsonContent;
            if (jsonContentObj instanceof String) {
                jsonContent = (String) jsonContentObj;
            } else {
                jsonContent = objectMapper.writeValueAsString(jsonContentObj);
            }

            Map<String, Object> validationResult = fileReferenceService.validateTemplateJSON(jsonContent);
            if (!(Boolean) validationResult.get("valid")) {
                sendError(response, 400, (String) validationResult.get("error"));
                return;
            }

            FileReferenceTemplate existingTemplate = fileReferenceService.getTemplateByName(templateName);
            if (existingTemplate != null) {
                sendError(response, 400, "Template with this name already exists");
                return;
            }

            FileReferenceTemplate template = new FileReferenceTemplate();
            template.setTemplateName(templateName);
            template.setDescription(description);
            template.setJsonContent(jsonContent);
            template.setUploadedBy(currentUser.getId());
            template.setActive(true);

            FileReferenceTemplate createdTemplate = fileReferenceService.createTemplate(template);

            response.setStatus(HttpServletResponse.SC_CREATED);
            sendJsonResponse(response, createdTemplate);

            logger.info("Created file reference template: {} by user: {}",
                templateName, currentUser.getEmail());

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in POST /api/file-reference-templates", e);
            sendError(response, 500, "Internal server error: " + e.getMessage());
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

            if (!checkEORole(currentUser)) {
                sendError(response, 403, "Only Engineering Officers can manage templates");
                return;
            }

            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Template ID is required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }

            byte[] templateId = parsePathId(pathParts[1]);
            if (templateId == null) {
                sendError(response, 400, "Invalid template ID format");
                return;
            }

            FileReferenceTemplate existingTemplate = fileReferenceService.getTemplateById(templateId);
            if (existingTemplate == null) {
                sendError(response, 404, "Template not found");
                return;
            }

            String body = getRequestBody(request);
            Map<String, Object> updates = objectMapper.readValue(body, Map.class);

            if (updates.containsKey("name") || updates.containsKey("templateName")) {
                String newName = (String) updates.getOrDefault("name", updates.get("templateName"));
                if (newName != null && !newName.trim().isEmpty()) {
                    existingTemplate.setTemplateName(newName);
                }
            }

            if (updates.containsKey("description")) {
                existingTemplate.setDescription((String) updates.get("description"));
            }

            if (updates.containsKey("jsonContent")) {
                Object jsonContentObj = updates.get("jsonContent");
                String jsonContent;

                if (jsonContentObj instanceof String) {
                    jsonContent = (String) jsonContentObj;
                } else {
                    jsonContent = objectMapper.writeValueAsString(jsonContentObj);
                }

                Map<String, Object> validationResult = fileReferenceService.validateTemplateJSON(jsonContent);
                if (!(Boolean) validationResult.get("valid")) {
                    sendError(response, 400, (String) validationResult.get("error"));
                    return;
                }

                existingTemplate.setJsonContent(jsonContent);
            }

            if (updates.containsKey("isActive")) {
                existingTemplate.setActive((Boolean) updates.get("isActive"));
            }

            FileReferenceTemplate updatedTemplate = fileReferenceService.updateTemplate(existingTemplate);

            sendJsonResponse(response, updatedTemplate);

            logger.info("Updated file reference template: {} by user: {}",
                existingTemplate.getTemplateName(), currentUser.getEmail());

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in PUT /api/file-reference-templates", e);
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

            if (!checkEORole(currentUser)) {
                sendError(response, 403, "Only Engineering Officers can manage templates");
                return;
            }

            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Template ID is required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }

            byte[] templateId = parsePathId(pathParts[1]);
            if (templateId == null) {
                sendError(response, 400, "Invalid template ID format");
                return;
            }

            boolean deleted = fileReferenceService.deleteTemplate(templateId);
            if (!deleted) {
                sendError(response, 404, "Template not found");
                return;
            }

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

            logger.info("Deleted file reference template ID: {} by user: {}",
                pathParts[1], currentUser.getEmail());

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in DELETE /api/file-reference-templates", e);
            sendError(response, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleGetAllTemplates(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String activeOnlyParam = request.getParameter("activeOnly");
        boolean activeOnly = "true".equalsIgnoreCase(activeOnlyParam);

        List<FileReferenceTemplate> templates;
        if (activeOnly) {
            templates = fileReferenceService.getActiveTemplates();
        } else {
            templates = fileReferenceService.getAllTemplates();
        }

        sendJsonResponse(response, templates);
    }

    private void handleGetTemplateById(String idStr, HttpServletResponse response)
            throws Exception {
        byte[] templateId = parsePathId(idStr);
        if (templateId == null) {
            sendError(response, 400, "Invalid template ID format");
            return;
        }

        FileReferenceTemplate template = fileReferenceService.getTemplateById(templateId);
        if (template == null) {
            sendError(response, 404, "Template not found");
            return;
        }

        sendJsonResponse(response, template);
    }

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("currentUser");
        }
        return null;
    }

    private boolean checkEORole(User user) {
        return user != null && "eo".equalsIgnoreCase(user.getRole());
    }

    private byte[] parsePathId(String idStr) {
        try {
            return UuidUtil.uuidStringToBytes(idStr);
        } catch (Exception e) {
            logger.warn("Failed to parse UUID: {}", idStr);
            return null;
        }
    }

    private String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
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

        ErrorResponse error = new ErrorResponse(status, message);
        objectMapper.writeValue(response.getWriter(), error);
    }

    private void handleException(HttpServletResponse response, TicketTrackerException e) throws IOException {
        logger.error("TicketTrackerException: {}", e.getMessage(), e);

        if (e instanceof ResourceNotFoundException) {
            sendError(response, 404, e.getMessage());
        } else if (e instanceof ValidationException) {
            sendError(response, 400, e.getMessage());
        } else {
            sendError(response, 500, "Internal server error");
        }
    }

    private static class ErrorResponse {
        public int status;
        public String message;
        public String timestamp;

        public ErrorResponse(int status, String message) {
            this.status = status;
            this.message = message;
            this.timestamp = new java.util.Date().toString();
        }
    }
}
