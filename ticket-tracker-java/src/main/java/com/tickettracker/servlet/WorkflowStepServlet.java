package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.User;
import com.tickettracker.model.WorkflowStep;
import com.tickettracker.model.WorkflowStepUpdateRequest;
import com.tickettracker.service.WorkflowService;
import com.tickettracker.util.ByteArrayUtil;
import com.tickettracker.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/workflow-steps/*")
public class WorkflowStepServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowStepServlet.class);
    private WorkflowService workflowService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.workflowService = new WorkflowService();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetAllSteps(request, response);
            } else {
                String[] pathParts = pathInfo.split("/");
                if (pathParts.length == 2) {
                    handleGetStep(pathParts[1], response);
                } else {
                    sendError(response, 400, "Invalid request path");
                }
            }
        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/workflow-steps", e);
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

            if (pathInfo != null && pathInfo.equals("/bulk")) {
                handleBulkCreate(request, response, currentUser);
                return;
            }

            String body = getRequestBody(request);
            WorkflowStep step;

            try {
                step = objectMapper.readValue(body, WorkflowStep.class);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid UUID format in workflow step creation request", e);
                sendError(response, 400, "Invalid UUID format: " + e.getMessage());
                return;
            }

            WorkflowStep createdStep = workflowService.createWorkflowStep(step, currentUser.getId());

            response.setStatus(HttpServletResponse.SC_CREATED);
            sendJsonResponse(response, createdStep);

        } catch (TicketTrackerException e) {
        	e.printStackTrace();
        	handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in POST /api/workflow-steps", e);
            sendError(response, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleBulkCreate(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException, TicketTrackerException {
        String body = getRequestBody(request);
        List<WorkflowStep> steps = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructCollectionType(List.class, WorkflowStep.class));

        List<WorkflowStep> createdSteps = workflowService.createWorkflowStepsBulk(steps, currentUser.getId());

        response.setStatus(HttpServletResponse.SC_CREATED);
        sendJsonResponse(response, createdSteps);
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

            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Step ID required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }	
            System.out.println("--before stepid-----"+pathParts[1]);
            byte[] stepId = ByteArrayUtil.hexToBytes(pathParts[1]);
            System.out.println("--after stepid-----"+ByteArrayUtil.bytesToHex(stepId));

            String body = getRequestBody(request);
            WorkflowStepUpdateRequest updateRequest;

            try {
                updateRequest = objectMapper.readValue(body, WorkflowStepUpdateRequest.class);
                updateRequest.setId(stepId);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid UUID format in workflow step update request", e);
                sendError(response, 400, "Invalid UUID format: " + e.getMessage());
                return;
            }

            WorkflowStep updatedStep = workflowService.updateWorkflowStep(updateRequest, currentUser.getId());

            sendJsonResponse(response, updatedStep);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in PUT /api/workflow-steps", e);
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

            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Step ID required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }

            byte[] stepId = hexToBytes(pathParts[1]);
            workflowService.deleteWorkflowStep(stepId, currentUser.getId());

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in DELETE /api/workflow-steps", e);
            sendError(response, 500, "Internal server error");
        }
    }

    private void handleGetAllSteps(HttpServletRequest request, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        String ticketIdParam = request.getParameter("ticketId");

        if (ticketIdParam != null) {
            byte[] ticketId = hexToBytes(ticketIdParam);
            List<WorkflowStep> steps = workflowService.getWorkflowStepsByTicketId(ticketId);
            sendJsonResponse(response, steps);
        } else {
            sendError(response, 400, "ticketId parameter required");
        }
    }

    private void handleGetStep(String stepId, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        byte[] id = hexToBytes(stepId);
        WorkflowStep step = workflowService.getWorkflowStepById(id);
        sendJsonResponse(response, step);
    }

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("currentUser");
        }
        return null;
    }

    private String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder buffer = new StringBuilder();
        String line;
        while ((line = request.getReader().readLine()) != null) {
            buffer.append(line);
        }
        return buffer.toString();
    }

    private void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", true);
        apiResponse.put("data", data);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), apiResponse);
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

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return null;
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
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
