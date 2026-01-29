package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.Document;
import com.tickettracker.model.User;
import com.tickettracker.service.DocumentService;
import com.tickettracker.util.ByteArrayUtil;
import com.tickettracker.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.List;

@WebServlet("/api/files/*")
@MultipartConfig(
    maxFileSize = 52428800,      // 50 MB
    maxRequestSize = 104857600,   // 100 MB
    fileSizeThreshold = 1048576   // 1 MB
)
public class FileServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(FileServlet.class);
    private DocumentService documentService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.documentService = new DocumentService();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetAllDocuments(request, response);
            } else {
                String[] pathParts = pathInfo.split("/");
                if (pathParts.length == 2) {
                    handleGetDocument(request, pathParts[1], response);
                } else {
                    sendError(response, 400, "Invalid request path");
                }
            }
        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/files", e);
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

            String contentType = request.getContentType();
            if (contentType != null && contentType.startsWith("multipart/form-data")) {
                handleFileUpload(request, response, currentUser);
            } else {
                String body = getRequestBody(request);
                Document document = objectMapper.readValue(body, Document.class);

                Document createdDocument = documentService.createDocument(document, currentUser.getId());

                response.setStatus(HttpServletResponse.SC_CREATED);
                sendJsonResponse(response, createdDocument);
            }

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in POST /api/files", e);
            sendError(response, 500, "Internal server error");
        }
    }

    private void handleFileUpload(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws ServletException, IOException, TicketTrackerException {
        Part filePart = request.getPart("file");
        if (filePart == null) {
            sendError(response, 400, "No file provided");
            return;
        }

        String fileName = getFileName(filePart);
        String ticketIdStr = request.getParameter("ticketId");
        String stepIdStr = request.getParameter("stepId");
        String docType = request.getParameter("type");
        String isMandatory = request.getParameter("isMandatory");

        Document document = new Document();
        document.setName(fileName);
        document.setType(docType != null ? docType : "general");
        document.setSize((int) filePart.getSize());
        document.setStoragePath("blob_" + System.currentTimeMillis());
        document.setUploadedBy(currentUser.getId());

        if (ticketIdStr != null) {
            document.setTicketId(ByteArrayUtil.hexToBytes(ticketIdStr));
        }
        if (stepIdStr != null) {
            document.setStepId(ByteArrayUtil.hexToBytes(stepIdStr));
        }
        if (isMandatory != null) {
            document.setMandatory("true".equalsIgnoreCase(isMandatory));
        }

        try (InputStream fileContent = filePart.getInputStream()) {
            byte[] fileData = new byte[(int) filePart.getSize()];
            fileContent.read(fileData);
            document.setFileContent(fileData);
        }

        Document createdDocument = documentService.createDocument(document, currentUser.getId());

        response.setStatus(HttpServletResponse.SC_CREATED);
        sendJsonResponse(response, createdDocument);
    }

    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        for (String token : contentDisposition.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length() - 1);
            }
        }
        return "unknown";
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
                sendError(response, 400, "Document ID required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }

            byte[] documentId = hexToBytes(pathParts[1]);
            documentService.deleteDocument(documentId, currentUser.getId());

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in DELETE /api/files", e);
            sendError(response, 500, "Internal server error");
        }
    }

    private void handleGetAllDocuments(HttpServletRequest request, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        String ticketIdParam = request.getParameter("ticketId");
        String stepIdParam = request.getParameter("stepId");

        if (ticketIdParam != null) {
            byte[] ticketId = hexToBytes(ticketIdParam);
            List<Document> documents = documentService.getDocumentsByTicketId(ticketId);
            sendJsonResponse(response, documents);
        } else if (stepIdParam != null) {
            byte[] stepId = hexToBytes(stepIdParam);
            List<Document> documents = documentService.getDocumentsByStepId(stepId);
            sendJsonResponse(response, documents);
        } else {
            sendError(response, 400, "ticketId or stepId parameter required");
        }
    }

    private void handleGetDocument(HttpServletRequest request, String documentId, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        byte[] id = hexToBytes(documentId);
        Document document = documentService.getDocumentById(id);

        String download = request.getParameter("download");
        if ("true".equalsIgnoreCase(download) && document.getFileContent() != null) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + document.getName() + "\"");
            response.setContentLength((int) document.getSize());

            try (OutputStream out = response.getOutputStream()) {
                out.write(document.getFileContent());
                out.flush();
            }
        } else {
        	sendJsonResponse(response, document);
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
