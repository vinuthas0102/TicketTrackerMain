package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.BulkTicketCreateRequest;
import com.tickettracker.model.BulkTicketOperationResult;
import com.tickettracker.model.Ticket;
import com.tickettracker.model.User;
import com.tickettracker.service.TicketService;
import com.tickettracker.util.ByteArrayUtil;
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

@WebServlet("/api/tickets/*")
public class TicketServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(TicketServlet.class);
    private TicketService ticketService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.ticketService = new TicketService();
        this.objectMapper = JsonUtil.getObjectMapper();
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
                handleBulkCreate(request, response, currentUser);
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
            if (pathParts.length != 2) {
                sendError(response, 400, "Invalid request path");
                return;
            }

            String body = getRequestBody(request);
            Ticket ticket;

            try {
                ticket = objectMapper.readValue(body, Ticket.class);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid UUID format in ticket update request", e);
                sendError(response, 400, "Invalid UUID format: " + e.getMessage());
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
        String status = request.getParameter("status");
        String moduleId = request.getParameter("moduleId");
        String search = request.getParameter("search");
        String accessible = request.getParameter("accessible");

        List<Ticket> tickets;

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
        } else if ("true".equalsIgnoreCase(accessible)) {
            User currentUser = getCurrentUser(request);
            if (currentUser != null) {
                logger.debug("Fetching accessible tickets for user: {}", ByteArrayUtil.bytesToHex(currentUser.getId()));
                tickets = ticketService.getAccessibleTickets(currentUser.getId());
            } else {
                sendError(response, 401, "Authentication required");
                return;
            }
        } else {
            logger.debug("Fetching all tickets");
            tickets = ticketService.getAllTickets();
        }

        sendJsonResponse(response, tickets);
    }

    private void handleGetTicket(String ticketId, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        logger.debug("Fetching ticket by ID: {}", ticketId);
        byte[] id = ByteArrayUtil.hexToBytes(ticketId);
        Ticket ticket = ticketService.getTicket(id);
        sendJsonResponse(response, ticket);
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
