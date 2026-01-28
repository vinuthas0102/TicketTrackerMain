package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.User;
import com.tickettracker.model.WorkflowComment;
import com.tickettracker.service.WorkflowCommentService;
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

@WebServlet("/api/workflow-comments/*")
public class WorkflowCommentServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowCommentServlet.class);
    private WorkflowCommentService commentService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.commentService = new WorkflowCommentService();
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

            String stepIdParam = request.getParameter("stepId");
            if (stepIdParam == null || stepIdParam.isEmpty()) {
                sendError(response, 400, "stepId parameter required");
                return;
            }

            byte[] stepId = UuidUtil.uuidStringToBytes(stepIdParam);
            List<WorkflowComment> comments = commentService.getStepComments(stepId, currentUser.getId());
            sendJsonResponse(response, comments);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/workflow-comments", e);
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
            Map<String, Object> requestData = objectMapper.readValue(body, Map.class);

            String stepIdStr = (String) requestData.get("stepId");
            String content = (String) requestData.get("content");

            if (stepIdStr == null || stepIdStr.isEmpty()) {
                sendError(response, 400, "stepId is required");
                return;
            }

            if (content == null || content.trim().isEmpty()) {
                sendError(response, 400, "content is required");
                return;
            }

            byte[] stepId = UuidUtil.uuidStringToBytes(stepIdStr);
            WorkflowComment createdComment = commentService.createComment(stepId, content, currentUser.getId());

            response.setStatus(HttpServletResponse.SC_CREATED);
            sendJsonResponse(response, createdComment);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in POST /api/workflow-comments", e);
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

            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Comment ID required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }

            String body = getRequestBody(request);
            Map<String, Object> requestData = objectMapper.readValue(body, Map.class);

            String content = (String) requestData.get("content");
            if (content == null || content.trim().isEmpty()) {
                sendError(response, 400, "content is required");
                return;
            }

            byte[] commentId = UuidUtil.uuidStringToBytes(pathParts[1]);
            WorkflowComment updatedComment = commentService.updateComment(commentId, content, currentUser.getId());

            sendJsonResponse(response, updatedComment);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in PUT /api/workflow-comments", e);
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
                sendError(response, 400, "Comment ID required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }

            byte[] commentId = UuidUtil.uuidStringToBytes(pathParts[1]);
            boolean deleted = commentService.deleteComment(commentId, currentUser.getId());

            if (deleted) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                sendError(response, 404, "Comment not found");
            }

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in DELETE /api/workflow-comments", e);
            sendError(response, 500, "Internal server error: " + e.getMessage());
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

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}
