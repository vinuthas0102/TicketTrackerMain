package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.dao.AuditLogDAO;
import com.tickettracker.dao.DocumentDAO;
import com.tickettracker.dao.ModuleDAO;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.AuditLog;
import com.tickettracker.model.BulkTicketCreateRequest;
import com.tickettracker.model.BulkTicketOperationResult;
import com.tickettracker.model.Document;
import com.tickettracker.model.Module;
import com.tickettracker.model.Ticket;
import com.tickettracker.model.User;
import com.tickettracker.service.DocumentService;
import com.tickettracker.service.TicketService;
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
import java.util.List;

@WebServlet("/api/tickets/*")
@MultipartConfig(
    maxFileSize = 52428800,      // 50 MB
    maxRequestSize = 104857600,   // 100 MB
    fileSizeThreshold = 1048576   // 1 MB
)
public class TicketServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(TicketServlet.class);
    private TicketService ticketService;
    private DocumentService documentService;
    private ObjectMapper objectMapper;
    private ModuleDAO moduleDAO;
    private AuditLogDAO auditLogDAO;
    private DocumentDAO documentDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        this.ticketService = new TicketService();
        this.documentService = new DocumentService();
        this.objectMapper = JsonUtil.getObjectMapper();
        this.moduleDAO = new ModuleDAO();
        this.auditLogDAO = new AuditLogDAO();
        this.documentDAO = new DocumentDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetAllTickets(request, response);
            } else {
                String[] pathParts = pathInfo.split("/");
                if (pathParts.length == 2) {
                    handleGetTicket(pathParts[1], response);
                } else if (pathParts.length == 3 && "files".equals(pathParts[2])) {
                    handleGetTicketFiles(pathParts[1], response);
                } else {
                    sendError(response, 400, "Invalid request path");
                }
            }
        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/tickets", e);
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
                if (!currentUser.isAdmin()) {
                    logger.warn("Permission denied: User {} (role: {}) attempted to bulk create tickets",
                            currentUser.getEmail(), currentUser.getRole());
                    sendError(response, 403, "Forbidden: Only EO users can create tickets");
                    return;
                }
                handleBulkCreate(request, response, currentUser);
                return;
            }

            if (pathInfo != null) {
                String[] pathParts = pathInfo.split("/");
                if (pathParts.length == 3 && "status".equals(pathParts[2])) {
                    String contentType = request.getContentType();
                    if (contentType != null && contentType.startsWith("multipart/form-data")) {
                        handleStatusChangeWithFile(request, response, pathParts[1], currentUser);
                    } else {
                        sendError(response, 405, "Use PUT to change ticket status without a file");
                    }
                    return;
                }
            }

            if (!currentUser.isAdmin() && !currentUser.isTechnician()) {
                logger.warn("Permission denied: User {} (role: {}) attempted to create ticket",
                        currentUser.getEmail(), currentUser.getRole());
                sendError(response, 403, "Forbidden: Only EO users can create tickets");
                return;
            }

            String body = getRequestBody(request);
            logger.debug("Received ticket creation request body: {}", body);

            Ticket ticket;

            try {
                ticket = objectMapper.readValue(body, Ticket.class);
                logger.debug("Parsed ticket: Title={}, ModuleId={}, Department={}, Category={}",
                        ticket.getTitle(),
                        ticket.getModuleIdAsString(),
                        ticket.getDepartment(),
                        ticket.getCategory());
            } catch (IllegalArgumentException e) {
                logger.error("Invalid UUID format in ticket creation request: {}", e.getMessage(), e);
                sendError(response, 400, "Invalid UUID format: " + e.getMessage());
                return;
            } catch (Exception e) {
                logger.error("Error parsing ticket JSON: {}", e.getMessage(), e);
                sendError(response, 400, "Invalid JSON format: " + e.getMessage());
                return;
            }

            Ticket createdTicket = ticketService.createTicket(ticket, currentUser.getId());

            logger.info("Created ticket object - ID: {}, TicketNumber: {}, ModuleId: {}, Data: {}",
                createdTicket.getId() != null ? ByteArrayUtil.bytesToHex(createdTicket.getId()) : "null",
                createdTicket.getTicketNumber(),
                createdTicket.getModuleId() != null ? ByteArrayUtil.bytesToHex(createdTicket.getModuleId()) : "null",
                createdTicket.getData());

            try {
                String jsonTest = objectMapper.writeValueAsString(createdTicket);
                logger.info("Ticket serialized JSON length: {} characters", jsonTest.length());
                logger.debug("Ticket serialized JSON: {}", jsonTest);

                if (jsonTest.equals("{}")) {
                    logger.error("WARNING: Ticket serialized to empty object {}!");
                }
            } catch (Exception e) {
                logger.error("Failed to serialize ticket for logging", e);
            }

            response.setStatus(HttpServletResponse.SC_CREATED);
            sendJsonResponse(response, createdTicket);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in POST /api/tickets: {}", e.getMessage(), e);
            sendError(response, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleBulkCreate(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException, TicketTrackerException {
        String body = getRequestBody(request);
        logger.debug("Received bulk ticket creation request body: {}", body);

        BulkTicketCreateRequest bulkRequest;
        try {
            bulkRequest = objectMapper.readValue(body, BulkTicketCreateRequest.class);
            logger.debug("Parsed bulk request with {} tickets", bulkRequest.getTickets() != null ? bulkRequest.getTickets().size() : 0);
        } catch (Exception e) {
            logger.error("Error parsing bulk ticket request JSON: {}", e.getMessage(), e);
            throw new TicketTrackerException("Invalid JSON format: " + e.getMessage(), 400);
        }

        if (bulkRequest.getTickets() == null || bulkRequest.getTickets().isEmpty()) {
            throw new TicketTrackerException("No tickets provided in bulk request", 400);
        }

        byte[] moduleIdBytes = null;
        if (bulkRequest.getModuleId() != null && !bulkRequest.getModuleId().isEmpty()) {
            logger.debug("Bulk create with moduleId: {}", bulkRequest.getModuleId());
            moduleIdBytes = ByteArrayUtil.hexToBytes(bulkRequest.getModuleId());
        }

        for (Ticket ticket : bulkRequest.getTickets()) {
            if (ticket.getModuleId() == null && moduleIdBytes != null) {
                ticket.setModuleId(moduleIdBytes);
            }
        }

        BulkTicketOperationResult result = ticketService.createTicketsBulk(bulkRequest.getTickets(), currentUser.getId());
        logger.info("Bulk ticket creation result: {} success, {} failed out of {} total",
                result.getSuccessCount(), result.getFailedCount(), result.getTotalCount());

        response.setStatus(HttpServletResponse.SC_CREATED);
        sendJsonResponse(response, result);
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
                sendError(response, 400, "Ticket ID required");
                return;
            }

            String[] pathParts = pathInfo.split("/");

            if (pathParts.length == 3 && "status".equals(pathParts[2])) {
                handleStatusChange(request, response, pathParts[1], currentUser);
                return;
            }

            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }

            String body = getRequestBody(request);
            Ticket ticket;

            try {
                ticket = objectMapper.readValue(body, Ticket.class);
                ticket.setId(UuidUtil.uuidStringToBytes(pathParts[1]));
            } catch (IllegalArgumentException e) {
                logger.error("Invalid UUID format in ticket update request", e);
                sendError(response, 400, "Invalid UUID format: " + e.getMessage());
                return;
            }

            boolean canUpdate = ticketService.canUserAccessTicket(currentUser.getId(), ticket.getId());
            if (!canUpdate) {
                logger.warn("Permission denied: User {} (role: {}) attempted to update ticket {}",
                        currentUser.getEmail(), currentUser.getRole(),
                        ByteArrayUtil.bytesToHex(ticket.getId()));
                sendError(response, 403, "Forbidden: You do not have permission to update this ticket");
                return;
            }

            Ticket updatedTicket = ticketService.updateTicket(ticket, currentUser.getId());

            sendJsonResponse(response, updatedTicket);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in PUT /api/tickets", e);
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

            if (!currentUser.isAdmin()) {
                logger.warn("Permission denied: User {} (role: {}) attempted to delete ticket",
                        currentUser.getEmail(), currentUser.getRole());
                sendError(response, 403, "Forbidden: Only EO users can delete tickets");
                return;
            }

            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Ticket ID required");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }

            logger.debug("Deleting ticket with ID: {}", pathParts[1]);
            byte[] ticketId = ByteArrayUtil.hexToBytes(pathParts[1]);
            ticketService.deleteTicket(ticketId, currentUser.getId());

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in DELETE /api/tickets", e);
            sendError(response, 500, "Internal server error");
        }
    }

    private void handleGetAllTickets(HttpServletRequest request, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        User currentUser = getCurrentUser(request);
        String status = request.getParameter("status");
        String moduleId = request.getParameter("moduleId");
        String search = request.getParameter("search");
        String accessible = request.getParameter("accessible");

        List<Ticket> tickets;

        if (currentUser == null && !"true".equalsIgnoreCase(accessible)) {
            logger.debug("Fetching all tickets (no authentication)");
            tickets = ticketService.getAllTickets();
            sendJsonResponse(response, tickets);
            return;
        }

        if (currentUser != null && (accessible == null || "true".equalsIgnoreCase(accessible))) {
            logger.debug("Fetching accessible tickets for user: {} (role: {})",
                    ByteArrayUtil.bytesToHex(currentUser.getId()), currentUser.getRole());
            if (moduleId != null && !moduleId.trim().isEmpty()) {
                if (!UuidUtil.isValidUuid(moduleId)) {
                    sendError(response, 400, "Invalid module ID format. Expected UUID format.");
                    return;
                }
                byte[] moduleIdBytes = ByteArrayUtil.hexToBytes(moduleId);
                tickets = ticketService.getAccessibleTicketsByModule(currentUser.getId(), moduleIdBytes);
            } else {
                tickets = ticketService.getAccessibleTickets(currentUser.getId());
            }
            sendJsonResponse(response, tickets);
            return;
        }

        if (search != null && !search.trim().isEmpty()) {
            logger.debug("Searching tickets with term: {}", search);
            tickets = ticketService.searchTickets(search);
        } else if (status != null) {
            logger.debug("Fetching tickets by status: {}", status);
            tickets = ticketService.getTicketsByStatus(status);
        } else if (moduleId != null) {
            logger.debug("Fetching tickets by moduleId: {}", moduleId);

            if (!UuidUtil.isValidUuid(moduleId)) {
                logger.error("Invalid UUID format for moduleId: {}", moduleId);
                sendError(response, 400, "Invalid module ID format. Expected UUID format.");
                return;
            }

            byte[] moduleIdBytes = ByteArrayUtil.hexToBytes(moduleId);
            logger.debug("Converted moduleId to bytes: {}", ByteArrayUtil.bytesToHex(moduleIdBytes));
            tickets = ticketService.getTicketsByModule(moduleIdBytes);
            logger.debug("Found {} tickets for moduleId: {}", tickets.size(), moduleId);
        } else {
            logger.debug("Fetching all tickets");
            tickets = ticketService.getAllTickets();
        }

        if (currentUser != null && !currentUser.isAdmin()) {
            List<byte[]> accessibleIds = ticketService.getAccessibleTicketIdsForUser(currentUser.getId());
            tickets.removeIf(ticket -> !containsByteArray(accessibleIds, ticket.getId()));
            logger.debug("Filtered to {} accessible tickets for user", tickets.size());
        }

        sendJsonResponse(response, tickets);
    }

    private boolean containsByteArray(List<byte[]> list, byte[] target) {
        if (target == null) return false;
        for (byte[] item : list) {
            if (java.util.Arrays.equals(item, target)) {
                return true;
            }
        }
        return false;
    }

    private void handleGetTicket(String ticketId, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        logger.debug("Fetching ticket by ID: {}", ticketId);
        byte[] id = ByteArrayUtil.hexToBytes(ticketId);
        Ticket ticket = ticketService.getTicket(id);
        sendJsonResponse(response, ticket);
    }

    private void handleGetTicketFiles(String ticketId, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        logger.debug("Fetching files for ticket ID: {}", ticketId);
        byte[] id = ByteArrayUtil.hexToBytes(ticketId);
        List<Document> documents = documentService.getDocumentsByTicketId(id);
        sendJsonResponse(response, documents);
    }

    private void handleStatusChange(HttpServletRequest request, HttpServletResponse response,
                                     String ticketIdHex, User currentUser)
            throws IOException, TicketTrackerException {
        logger.debug("Handling status change for ticket: {}", ticketIdHex);

        String body = getRequestBody(request);
        logger.debug("Status change request body: {}", body);

        StatusChangeRequest statusRequest;
        try {
            statusRequest = objectMapper.readValue(body, StatusChangeRequest.class);
        } catch (Exception e) {
            logger.error("Error parsing status change request: {}", e.getMessage(), e);
            sendError(response, 400, "Invalid request format: " + e.getMessage());
            return;
        }

        if (statusRequest.newStatus == null || statusRequest.newStatus.trim().isEmpty()) {
            sendError(response, 400, "New status is required");
            return;
        }

        if (statusRequest.remarks == null || statusRequest.remarks.trim().isEmpty()) {
            boolean isRemarksOptional = false;
            try {
                Ticket existingTicketForCheck = ticketService.getTicket(ByteArrayUtil.hexToBytes(ticketIdHex));
                if (existingTicketForCheck != null && existingTicketForCheck.getModuleId() != null) {
                    Module module = moduleDAO.findById(existingTicketForCheck.getModuleId());
                    if (module != null && module.getConfig() != null) {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode configNode = mapper.readTree(module.getConfig());
                        boolean reviewByEORequired = configNode.has("reviewByEORequired") ? configNode.get("reviewByEORequired").asBoolean() : true;
                        if (!reviewByEORequired && "active".equalsIgnoreCase(statusRequest.newStatus.trim())) {
                            isRemarksOptional = true;
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error checking module config for remarks validation: {}", e.getMessage());
            }
            if (!isRemarksOptional) {
                sendError(response, 400, "Remarks are required for status change");
                return;
            }
        } else if (statusRequest.remarks.trim().length() < 10) {
            boolean isRemarksOptional = false;
            try {
                Ticket existingTicketForCheck = ticketService.getTicket(ByteArrayUtil.hexToBytes(ticketIdHex));
                if (existingTicketForCheck != null && existingTicketForCheck.getModuleId() != null) {
                    Module module = moduleDAO.findById(existingTicketForCheck.getModuleId());
                    if (module != null && module.getConfig() != null) {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode configNode = mapper.readTree(module.getConfig());
                        boolean reviewByEORequired = configNode.has("reviewByEORequired") ? configNode.get("reviewByEORequired").asBoolean() : true;
                        if (!reviewByEORequired && "active".equalsIgnoreCase(statusRequest.newStatus.trim())) {
                            isRemarksOptional = true;
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error checking module config for remarks validation: {}", e.getMessage());
            }
            if (!isRemarksOptional) {
                sendError(response, 400, "Remarks must be at least 10 characters");
                return;
            }
        }

        byte[] ticketId = ByteArrayUtil.hexToBytes(ticketIdHex);

        boolean canUpdate = ticketService.canUserAccessTicket(currentUser.getId(), ticketId);
        if (!canUpdate) {
            logger.warn("Permission denied: User {} (role: {}) attempted to change status of ticket {}",
                    currentUser.getEmail(), currentUser.getRole(), ticketIdHex);
            sendError(response, 403, "Forbidden: You do not have permission to change this ticket's status");
            return;
        }

        Ticket existingTicket = ticketService.getTicket(ticketId);
        if (existingTicket != null && "COMPLETED".equalsIgnoreCase(existingTicket.getStatus())) {
            logger.warn("Rejected status change for completed ticket {} by user {}",
                    ticketIdHex, currentUser.getEmail());
            sendError(response, 409, "Status cannot be changed for completed tickets");
            return;
        }

        String normalizedStatus = statusRequest.newStatus.toLowerCase().trim();

        if ("completed".equals(normalizedStatus) && existingTicket != null
                && existingTicket.isCompletionDocumentsRequired()) {
            boolean hasCompletionCert = false;
            try {
                List<Document> ticketDocs = documentService.getDocumentsByTicketId(ticketId);
                for (Document doc : ticketDocs) {
                    if (doc.isCompletionCertificate()) {
                        hasCompletionCert = true;
                        break;
                    }
                }
            } catch (Exception e) {
                logger.warn("Error checking completion certificate for ticket {}: {}",
                        ticketIdHex, e.getMessage());
            }
            if (!hasCompletionCert) {
                sendError(response, 400,
                        "Completion certificate is required. Please upload evidence/completion document before marking this ticket as completed.");
                return;
            }
        }

        ticketService.updateTicketStatus(ticketId, normalizedStatus, currentUser.getId());

        logger.info("Ticket status changed: {} to {} by user: {}",
                ticketIdHex, normalizedStatus, currentUser.getEmail());

        response.setStatus(HttpServletResponse.SC_OK);
        sendJsonResponse(response, new StatusChangeResponse(true, "Status updated successfully"));
    }

    private void handleStatusChangeWithFile(HttpServletRequest request, HttpServletResponse response,
                                             String ticketIdHex, User currentUser)
            throws IOException, ServletException, TicketTrackerException {
        logger.debug("Handling status change with file for ticket: {}", ticketIdHex);

        String newStatus = request.getParameter("newStatus");
        String remarks = request.getParameter("remarks");

        if (newStatus == null || newStatus.trim().isEmpty()) {
            sendError(response, 400, "New status is required");
            return;
        }

        String normalizedStatus = newStatus.toLowerCase().trim();

        boolean isRemarksOptional = false;
        if (remarks == null || remarks.trim().isEmpty()) {
            try {
                Ticket existingTicketForCheck = ticketService.getTicket(ByteArrayUtil.hexToBytes(ticketIdHex));
                if (existingTicketForCheck != null && existingTicketForCheck.getModuleId() != null) {
                    Module module = moduleDAO.findById(existingTicketForCheck.getModuleId());
                    if (module != null && module.getConfig() != null) {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode configNode = mapper.readTree(module.getConfig());
                        boolean reviewByEORequired = configNode.has("reviewByEORequired") ? configNode.get("reviewByEORequired").asBoolean() : true;
                        if (!reviewByEORequired && "active".equalsIgnoreCase(newStatus.trim())) {
                            isRemarksOptional = true;
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error checking module config for remarks validation: {}", e.getMessage());
            }
            if (!isRemarksOptional) {
                sendError(response, 400, "Remarks are required for status change");
                return;
            }
        } else if (remarks.trim().length() < 10) {
            try {
                Ticket existingTicketForCheck = ticketService.getTicket(ByteArrayUtil.hexToBytes(ticketIdHex));
                if (existingTicketForCheck != null && existingTicketForCheck.getModuleId() != null) {
                    Module module = moduleDAO.findById(existingTicketForCheck.getModuleId());
                    if (module != null && module.getConfig() != null) {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode configNode = mapper.readTree(module.getConfig());
                        boolean reviewByEORequired = configNode.has("reviewByEORequired") ? configNode.get("reviewByEORequired").asBoolean() : true;
                        if (!reviewByEORequired && "active".equalsIgnoreCase(newStatus.trim())) {
                            isRemarksOptional = true;
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error checking module config for remarks validation: {}", e.getMessage());
            }
            if (!isRemarksOptional) {
                sendError(response, 400, "Remarks must be at least 10 characters");
                return;
            }
        }

        byte[] ticketId = ByteArrayUtil.hexToBytes(ticketIdHex);

        boolean canUpdate = ticketService.canUserAccessTicket(currentUser.getId(), ticketId);
        if (!canUpdate) {
            logger.warn("Permission denied: User {} (role: {}) attempted to change status of ticket {}",
                    currentUser.getEmail(), currentUser.getRole(), ticketIdHex);
            sendError(response, 403, "Forbidden: You do not have permission to change this ticket's status");
            return;
        }

        Ticket existingTicket = ticketService.getTicket(ticketId);
        if (existingTicket != null && "COMPLETED".equalsIgnoreCase(existingTicket.getStatus())) {
            logger.warn("Rejected status change for completed ticket {} by user {}",
                    ticketIdHex, currentUser.getEmail());
            sendError(response, 409, "Status cannot be changed for completed tickets");
            return;
        }

        Part filePart = request.getPart("file");
        if (filePart == null) {
            sendError(response, 400, "Completion certificate file is required");
            return;
        }

        String fileName = getFileName(filePart);

        Document document = new Document();
        document.setName(fileName);
        String mimeType = filePart.getContentType();
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = "application/octet-stream";
        }
        document.setType(mimeType);
        document.setSize(filePart.getSize());
        document.setStoragePath("blob_" + System.currentTimeMillis());
        document.setUploadedBy(currentUser.getId());
        document.setTicketId(ticketId);
        document.setCompletionCertificate(true);

        try (InputStream fileContent = filePart.getInputStream()) {
            byte[] fileData = new byte[(int) filePart.getSize()];
            fileContent.read(fileData);
            document.setFileContent(fileData);
        }

        Document createdDocument = documentService.createDocument(document, currentUser.getId());

        try {
            String description = String.format("%s uploaded completion certificate '%s'",
                    currentUser.getName() != null ? currentUser.getName() : currentUser.getEmail(),
                    fileName);

            AuditLog auditLog = new AuditLog();
            auditLog.setTicketId(ticketId);
            auditLog.setPerformedBy(currentUser.getId());
            auditLog.setAction("COMPLETION_CERTIFICATE_UPLOADED");
            auditLog.setActionCategory("document_action");
            auditLog.setDescription(description);
            auditLog.setMetadata("{\"fileName\":\"" + fileName.replace("\"", "\\\"")
                    + "\",\"documentId\":\"" + ByteArrayUtil.bytesToHex(createdDocument.getId()) + "\"}");
            AuditLog createdAuditLog = auditLogDAO.create(auditLog);

            documentDAO.updateAuditLogId(createdDocument.getId(), createdAuditLog.getId());
            createdDocument.setAuditLogId(createdAuditLog.getId());
            logger.info("Created audit log {} for completion certificate upload",
                    ByteArrayUtil.bytesToHex(createdAuditLog.getId()));
        } catch (Exception e) {
            logger.error("Failed to create audit log for completion certificate upload", e);
        }

        ticketService.updateTicketStatus(ticketId, normalizedStatus, currentUser.getId());

        logger.info("Ticket status changed with completion certificate: {} to {} by user: {}",
                ticketIdHex, normalizedStatus, currentUser.getEmail());

        createdDocument.setFileContent(null);

        response.setStatus(HttpServletResponse.SC_OK);
        sendJsonResponse(response, new StatusChangeResponse(true, "Status updated successfully"));
    }

    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) {
            return "unknown";
        }
        for (String token : contentDisposition.split(";")) {
            if (token.trim().startsWith("filename")) {
                String value = token.substring(token.indexOf("=") + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    return value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return "unknown";
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

    private static class StatusChangeRequest {
        public String newStatus;
        public String currentStatus;
        public String remarks;
        public String userId;

        public String getNewStatus() {
            return newStatus;
        }

        public void setNewStatus(String newStatus) {
            this.newStatus = newStatus;
        }

        public String getCurrentStatus() {
            return currentStatus;
        }

        public void setCurrentStatus(String currentStatus) {
            this.currentStatus = currentStatus;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }

    private static class StatusChangeResponse {
        private boolean success;
        private String message;

        public StatusChangeResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

}
