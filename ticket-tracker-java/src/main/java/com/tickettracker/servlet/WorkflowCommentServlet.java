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
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@WebServlet("/api/workflow-comments/*")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,
    maxFileSize = 5 * 1024 * 1024,
    maxRequestSize = 10 * 1024 * 1024
)
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

            String pathInfo = request.getPathInfo();
            if (pathInfo != null && pathInfo.contains("/attachment")) {
                handleDownloadAttachment(request, response);
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

    private void handleDownloadAttachment(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String pathInfo = request.getPathInfo();
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length < 3) {
            sendError(response, 400, "Comment ID required");
            return;
        }

        try {
            byte[] commentId = UuidUtil.uuidStringToBytes(pathParts[1]);
            WorkflowComment comment = commentService.getCommentById(commentId);
            if (comment == null) {
                sendError(response, 404, "Comment not found");
                return;
            }

            if (comment.getAttachmentPath() == null || comment.getAttachmentPath().isEmpty()) {
                sendError(response, 404, "Attachment not found");
                return;
            }

            String uploadDir = getServletContext().getRealPath("/WEB-INF/uploads");
            Path filePath = Paths.get(uploadDir, comment.getAttachmentPath());
            if (!Files.exists(filePath)) {
                sendError(response, 404, "Attachment file not found");
                return;
            }

            String contentType = comment.getAttachmentType() != null ? comment.getAttachmentType() : "application/octet-stream";
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + comment.getAttachmentName() + "\"");
            response.setContentLengthLong(Files.size(filePath));

            try (InputStream is = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    response.getOutputStream().write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            logger.error("Error downloading attachment", e);
            sendError(response, 500, "Error downloading attachment");
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

            String contentType = request.getContentType();
            String stepIdStr;
            String content;
            String channel;
            String attachmentPath = null;
            String attachmentName = null;
            String attachmentType = null;

            if (contentType != null && contentType.startsWith("multipart/form-data")) {
                stepIdStr = request.getParameter("stepId");
                content = request.getParameter("content");
                channel = request.getParameter("channel");

                Part filePart = request.getPart("file");
                if (filePart != null && filePart.getSize() > 0) {
                    attachmentName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                    attachmentType = filePart.getContentType();

                    String uploadDir = getServletContext().getRealPath("/WEB-INF/uploads/chat-attachments");
                    Path dirPath = Paths.get(uploadDir);
                    Files.createDirectories(dirPath);

                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String sanitizedFileName = attachmentName.replaceAll("[^a-zA-Z0-9.-]", "_");
                    String fileName = timestamp + "_" + sanitizedFileName;
                    Path filePath = dirPath.resolve(fileName);

                    try (InputStream is = filePart.getInputStream()) {
                        Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    attachmentPath = "chat-attachments/" + fileName;
                    logger.info("Saved chat attachment: {}", filePath);
                }
            } else {
                String body = getRequestBody(request);
                Map<String, Object> requestData = objectMapper.readValue(body, Map.class);

                stepIdStr = (String) requestData.get("stepId");
                content = (String) requestData.get("content");
                channel = (String) requestData.get("channel");
            }

            if (stepIdStr == null || stepIdStr.isEmpty()) {
                sendError(response, 400, "stepId is required");
                return;
            }

            if (content == null || content.trim().isEmpty()) {
                sendError(response, 400, "content is required");
                return;
            }

            byte[] stepId = UuidUtil.uuidStringToBytes(stepIdStr);
            WorkflowComment createdComment = commentService.createComment(
                    stepId, content, currentUser.getId(),
                    attachmentPath, attachmentName, attachmentType, channel);

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
