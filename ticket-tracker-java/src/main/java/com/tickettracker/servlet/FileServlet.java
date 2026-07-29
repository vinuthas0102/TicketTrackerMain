package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.dao.AuditLogDAO;
import com.tickettracker.dao.WorkflowStepFileReferenceDAO;
import com.tickettracker.dao.WorkflowStepProgressDocumentDAO.ProgressDocument;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.AuditLog;
import com.tickettracker.model.Document;
import com.tickettracker.model.User;
import com.tickettracker.service.DocumentService;
import com.tickettracker.service.WorkflowStepProgressDocumentService;
import com.tickettracker.util.ByteArrayUtil;
import com.tickettracker.util.JsonUtil;
import com.tickettracker.util.ResponseUtil;
import com.tickettracker.util.UuidUtil;
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
import java.util.Map;

@WebServlet("/api/files/*")
@MultipartConfig(
    maxFileSize = 52428800,      // 50 MB
    maxRequestSize = 104857600,   // 100 MB
    fileSizeThreshold = 1048576   // 1 MB
)
public class FileServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(FileServlet.class);
    private DocumentService documentService;
    private WorkflowStepProgressDocumentService progressDocumentService;
    private WorkflowStepFileReferenceDAO fileReferenceDAO;
    private AuditLogDAO auditLogDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.documentService = new DocumentService();
        this.progressDocumentService = new WorkflowStepProgressDocumentService();
        this.fileReferenceDAO = new WorkflowStepFileReferenceDAO();
        this.auditLogDAO = new AuditLogDAO();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetAllDocuments(request, response);
            } else if ("/completion-certificates".equals(pathInfo)) {
                handleGetCompletionCertificates(request, response);
            } else if ("/completion-certificates/check".equals(pathInfo)) {
                handleCheckCompletionCertificate(request, response);
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

            String pathInfo = request.getPathInfo();
            String contentType = request.getContentType();

            if ("/progress-docs".equals(pathInfo)) {
                if (contentType != null && contentType.startsWith("multipart/form-data")) {
                    handleProgressDocumentUpload(request, response, currentUser);
                } else {
                    sendError(response, 400, "Multipart form data required for progress document upload");
                }
            } else if ("/completion-cert".equals(pathInfo)) {
                if (contentType != null && contentType.startsWith("multipart/form-data")) {
                    handleCompletionCertificateUpload(request, response, currentUser);
                } else {
                    sendError(response, 400, "Multipart form data required for completion certificate upload");
                }
            } else if ("/copy-attachments".equals(pathInfo)) {
                handleCopyAttachments(request, response, currentUser);
            } else if ("/upload".equals(pathInfo) || pathInfo == null || "/".equals(pathInfo)) {
                if (contentType != null && contentType.startsWith("multipart/form-data")) {
                    handleFileUpload(request, response, currentUser);
                } else {
                    String body = getRequestBody(request);
                    Document document = objectMapper.readValue(body, Document.class);
                    Document createdDocument = documentService.createDocument(document, currentUser.getId());
                    response.setStatus(HttpServletResponse.SC_CREATED);
                    sendJsonResponse(response, createdDocument);
                }
            } else {
                sendError(response, 400, "Invalid endpoint: " + pathInfo);
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
        String fileReferenceIdStr = request.getParameter("fileReferenceId");
        String docType = request.getParameter("type");
        String isMandatory = request.getParameter("isMandatory");
        String isCompletionCertificateParam = request.getParameter("isCompletionCertificate");

        logger.debug("File upload parameters - ticketId: {}, stepId: {}, fileReferenceId: {}, fileName: {}, isCompletionCertificate: {}",
                     ticketIdStr, stepIdStr, fileReferenceIdStr, fileName, isCompletionCertificateParam);

        Document document = new Document();
        document.setName(fileName);
        String mimeType = filePart.getContentType();
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = docType != null && !docType.isEmpty() ? docType : "application/octet-stream";
        }
        document.setType(mimeType);
        document.setSize((int) filePart.getSize());
        document.setStoragePath("blob_" + System.currentTimeMillis());
        document.setUploadedBy(currentUser.getId());

        if (isValidParameter(ticketIdStr)) {
            document.setTicketId(ByteArrayUtil.hexToBytes(ticketIdStr));
        }
        if (isValidParameter(stepIdStr)) {
            document.setStepId(ByteArrayUtil.hexToBytes(stepIdStr));
        }
        if (isMandatory != null) {
            document.setMandatory("true".equalsIgnoreCase(isMandatory));
        }
        if (isCompletionCertificateParam != null) {
            document.setCompletionCertificate("true".equalsIgnoreCase(isCompletionCertificateParam));
        }

        try (InputStream fileContent = filePart.getInputStream()) {
            byte[] fileData = new byte[(int) filePart.getSize()];
            fileContent.read(fileData);
            document.setFileContent(fileData);
        }

        // Step 1: Create the document
        Document createdDocument = documentService.createDocument(document, currentUser.getId());

        // Step 2: Link document to file reference if provided
        if (isValidParameter(fileReferenceIdStr)) {
            try {
                byte[] fileReferenceId = UuidUtil.uuidStringToBytes(fileReferenceIdStr);
                boolean linked = fileReferenceDAO.updateDocumentLink(
                    fileReferenceId,
                    createdDocument.getId(),
                    currentUser.getId()
                );

                if (linked) {
                    logger.info("Successfully linked document {} to file reference {}",
                               ByteArrayUtil.bytesToHex(createdDocument.getId()),
                               fileReferenceIdStr);
                } else {
                    logger.warn("File reference {} not found or could not be linked", fileReferenceIdStr);
                }
            } catch (Exception e) {
                logger.error("Failed to link document to file reference: {}", e.getMessage(), e);
            }
        }

        createdDocument.setFileContent(null);

        response.setStatus(HttpServletResponse.SC_CREATED);
        sendJsonResponse(response, createdDocument);
    }

    private void handleProgressDocumentUpload(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws ServletException, IOException, TicketTrackerException {
        Part filePart = request.getPart("file");
        if (filePart == null) {
            sendError(response, 400, "No file provided");
            return;
        }

        String fileName = getFileName(filePart);
        String ticketIdStr = request.getParameter("ticketId");
        String stepIdStr = request.getParameter("stepId");
        String auditLogIdStr = request.getParameter("auditLogId");

        logger.debug("Progress document upload parameters - ticketId: {}, stepId: {}, auditLogId: {}, fileName: {}",
                     ticketIdStr, stepIdStr, auditLogIdStr, fileName);

        if (!isValidParameter(stepIdStr)) {
            sendError(response, 400, "stepId is required for progress document upload");
            return;
        }

        if (!isValidParameter(ticketIdStr)) {
            sendError(response, 400, "ticketId is required for progress document upload");
            return;
        }

        ProgressDocument progressDoc = new ProgressDocument();
        progressDoc.setFileName(fileName);
        progressDoc.setFileType(filePart.getContentType());
        progressDoc.setFileSize(filePart.getSize());
        progressDoc.setStepId(ByteArrayUtil.hexToBytes(stepIdStr));
        progressDoc.setTicketId(ByteArrayUtil.hexToBytes(ticketIdStr));
        progressDoc.setUploadedBy(currentUser.getId());

        byte[] auditLogId = null;
        if (isValidParameter(auditLogIdStr)) {
            auditLogId = ByteArrayUtil.hexToBytes(auditLogIdStr);
        } else {
            // No audit log provided - create one so the document is linked to the audit trail
            try {
                AuditLog auditLog = new AuditLog();
                auditLog.setTicketId(ByteArrayUtil.hexToBytes(ticketIdStr));
                auditLog.setStepId(ByteArrayUtil.hexToBytes(stepIdStr));
                auditLog.setPerformedBy(currentUser.getId());
                auditLog.setAction("PROGRESS_DOCUMENTS_UPLOADED");
                auditLog.setActionCategory("document_action");
                auditLog.setDescription("Progress document uploaded: " + fileName);
                auditLog.setMetadata("{\"fileName\":\"" + fileName.replace("\"", "\\\"") + "\",\"autoCreated\":true}");
                AuditLog created = auditLogDAO.create(auditLog);
                auditLogId = created.getId();
                logger.info("Auto-created audit log {} for progress document upload", ByteArrayUtil.bytesToHex(auditLogId));
            } catch (Exception e) {
                logger.error("Failed to auto-create audit log for progress document upload", e);
            }
        }
        if (auditLogId != null) {
            progressDoc.setAuditLogId(auditLogId);
        }

        try (InputStream fileContent = filePart.getInputStream()) {
            byte[] fileData = new byte[(int) filePart.getSize()];
            fileContent.read(fileData);
            progressDoc.setFileContent(fileData);
        }

        ProgressDocument createdDoc = progressDocumentService.uploadProgressDocument(progressDoc);

        createdDoc.setFileContent(null);

        response.setStatus(HttpServletResponse.SC_CREATED);
        sendJsonResponse(response, createdDoc);
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

    private void handleCompletionCertificateUpload(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws ServletException, IOException, TicketTrackerException {
        Part filePart = request.getPart("file");
        if (filePart == null) {
            sendError(response, 400, "No file provided");
            return;
        }

        String fileName = getFileName(filePart);
        String ticketIdStr = request.getParameter("ticketId");
        String stepIdStr = request.getParameter("stepId");

        if (!isValidParameter(ticketIdStr)) {
            sendError(response, 400, "ticketId is required for completion certificate upload");
            return;
        }

        Document document = new Document();
        document.setName(fileName);
        document.setType(filePart.getContentType());
        document.setSize(filePart.getSize());
        document.setTicketId(ByteArrayUtil.hexToBytes(ticketIdStr));
        if (isValidParameter(stepIdStr)) {
            document.setStepId(ByteArrayUtil.hexToBytes(stepIdStr));
        }
        document.setUploadedBy(currentUser.getId());
        document.setCompletionCertificate(true);

        try (InputStream fileContent = filePart.getInputStream()) {
            byte[] fileData = new byte[(int) filePart.getSize()];
            fileContent.read(fileData);
            document.setFileContent(fileData);
        }

        Document createdDoc = documentService.createDocument(document, currentUser.getId());
        createdDoc.setFileContent(null);

        response.setStatus(HttpServletResponse.SC_CREATED);
        sendJsonResponse(response, createdDoc);
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

            byte[] documentId = ByteArrayUtil.hexToBytes(pathParts[1]);
            documentService.deleteDocument(documentId, currentUser.getId());

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in DELETE /api/files", e);
            sendError(response, 500, "Internal server error");
        }
    }

    private void handleGetCompletionCertificates(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String ticketIdStr = request.getParameter("ticketId");
        if (ticketIdStr == null) {
            sendError(response, 400, "ticketId parameter is required");
            return;
        }
        try {
            byte[] ticketId = UuidUtil.uuidStringToBytes(ticketIdStr);
            sendJsonResponse(response, documentService.getTicketCompletionCertificates(ticketId));
        } catch (TicketTrackerException e) {
            sendError(response, e.getHttpStatus(), e.getMessage());
        }
    }

    private void handleCheckCompletionCertificate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String ticketIdStr = request.getParameter("ticketId");
        if (ticketIdStr == null) {
            sendError(response, 400, "ticketId parameter is required");
            return;
        }
        try {
            byte[] ticketId = UuidUtil.uuidStringToBytes(ticketIdStr);
            boolean has = documentService.hasTicketCompletionCertificate(ticketId);
            sendJsonResponse(response, Map.of("hasCompletionCertificate", has));
        } catch (TicketTrackerException e) {
            sendError(response, e.getHttpStatus(), e.getMessage());
        }
    }

    private void handleCopyAttachments(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {
        String sourceTicketIdStr = request.getParameter("sourceTicketId");
        String targetTicketIdStr = request.getParameter("targetTicketId");
        if (sourceTicketIdStr == null || targetTicketIdStr == null) {
            sendError(response, 400, "sourceTicketId and targetTicketId parameters are required");
            return;
        }
        try {
            byte[] sourceTicketId = UuidUtil.uuidStringToBytes(sourceTicketIdStr);
            byte[] targetTicketId = UuidUtil.uuidStringToBytes(targetTicketIdStr);
            documentService.copyTicketAttachments(sourceTicketId, targetTicketId, currentUser.getId());
            sendJsonResponse(response, Map.of("success", true));
        } catch (TicketTrackerException e) {
            sendError(response, e.getHttpStatus(), e.getMessage());
        }
    }

    private void handleGetAllDocuments(HttpServletRequest request, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        String ticketIdParam = request.getParameter("ticketId");
        String stepIdParam = request.getParameter("stepId");

        if (ticketIdParam != null) {
            byte[] ticketId = ByteArrayUtil.hexToBytes(ticketIdParam);
            List<Document> documents = documentService.getDocumentsByTicketId(ticketId);
            sendJsonResponse(response, documents);
        } else if (stepIdParam != null) {
            byte[] stepId = ByteArrayUtil.hexToBytes(stepIdParam);
            List<Document> documents = documentService.getDocumentsByStepId(stepId);
            sendJsonResponse(response, documents);
        } else {
            sendError(response, 400, "ticketId or stepId parameter required");
        }
    }

    private void handleGetDocument(HttpServletRequest request, String documentId, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        byte[] id = ByteArrayUtil.hexToBytes(documentId);
        String download = request.getParameter("download");

        try {
            Document document = documentService.getDocumentById(id);

            if ("true".equalsIgnoreCase(download) && document.getFileContent() != null) {
                String mimeType = (document.getType() != null && !document.getType().isEmpty())
                        ? document.getType() : "application/octet-stream";
                response.setContentType(mimeType);
                String safeFileName = document.getName() != null
                        ? document.getName().replaceAll("[^\\x20-\\x7E]", "_") : "download";
                response.setHeader("Content-Disposition", "attachment; filename=\"" + safeFileName + "\"");
                response.setContentLength((int) document.getSize());

                try (OutputStream out = response.getOutputStream()) {
                    out.write(document.getFileContent());
                    out.flush();
                }
            } else {
                sendJsonResponse(response, document);
            }
        } catch (TicketTrackerException e) {
            logger.debug("Document not found in documents table, checking progress documents table");

            try {
                ProgressDocument progressDoc = progressDocumentService.getProgressDocumentById(id, true);

                if ("true".equalsIgnoreCase(download) && progressDoc.getFileContent() != null) {
                    String mimeType = (progressDoc.getFileType() != null && !progressDoc.getFileType().isEmpty())
                            ? progressDoc.getFileType() : "application/octet-stream";
                    response.setContentType(mimeType);
                    String safeFileName = progressDoc.getFileName() != null
                            ? progressDoc.getFileName().replaceAll("[^\\x20-\\x7E]", "_") : "download";
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + safeFileName + "\"");
                    response.setContentLength((int) progressDoc.getFileSize());

                    try (OutputStream out = response.getOutputStream()) {
                        out.write(progressDoc.getFileContent());
                        out.flush();
                    }
                } else {
                    sendJsonResponse(response, progressDoc);
                }
            } catch (TicketTrackerException progressDocException) {
                logger.error("Document not found in either documents or progress documents table: {}", documentId);
                throw e;
            }
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

    private boolean isValidParameter(String param) {
        if (param == null || param.isEmpty()) {
            return false;
        }
        if ("null".equalsIgnoreCase(param) || "undefined".equalsIgnoreCase(param)) {
            logger.debug("Rejecting invalid parameter value: {}", param);
            return false;
        }
        return true;
    }

}
