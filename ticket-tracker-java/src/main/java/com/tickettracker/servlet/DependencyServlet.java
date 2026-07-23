package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.User;
import com.tickettracker.model.WorkflowStepDependency;
import com.tickettracker.service.DependencyService;
import com.tickettracker.util.JsonUtil;
import com.tickettracker.util.ResponseUtil;
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

@WebServlet("/api/dependencies/*")
public class DependencyServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(DependencyServlet.class);
    private DependencyService dependencyService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.dependencyService = new DependencyService();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String stepIdParam = request.getParameter("stepId");

            if (stepIdParam != null) {
                byte[] stepId = hexToBytes(stepIdParam);
                List<WorkflowStepDependency> dependencies = dependencyService.getDependenciesByStepId(stepId);
                sendJsonResponse(response, dependencies);
            } else {
                sendError(response, 400, "stepId parameter required");
            }
        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/dependencies", e);
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

            String body = getRequestBody(request);
            WorkflowStepDependency dependency = objectMapper.readValue(body, WorkflowStepDependency.class);

            WorkflowStepDependency createdDependency = dependencyService.createDependency(
                    dependency, currentUser.getId());

            response.setStatus(HttpServletResponse.SC_CREATED);
            sendJsonResponse(response, createdDependency);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in POST /api/dependencies", e);
            sendError(response, 500, "Internal server error");
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
                sendError(response, 400, "Dependency ID required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }

            byte[] dependencyId = hexToBytes(pathParts[1]);
            dependencyService.deleteDependency(dependencyId, currentUser.getId());

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in DELETE /api/dependencies", e);
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

    private String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder buffer = new StringBuilder();
        String line;
        while ((line = request.getReader().readLine()) != null) {
            buffer.append(line);
        }
        return buffer.toString();
    }

    private void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        ResponseUtil.sendWrappedSuccess(response, data);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        ResponseUtil.sendWrappedError(response, status, message);
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

}
