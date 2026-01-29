package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.dao.FinanceApprovalDAO.FinanceApproval;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.User;
import com.tickettracker.service.FinanceApprovalService;
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
import java.util.List;
import java.util.Map;

@WebServlet("/api/finance-approvals/*")
public class FinanceApprovalServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(FinanceApprovalServlet.class);
    private FinanceApprovalService financeApprovalService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.financeApprovalService = new FinanceApprovalService();
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetApprovals(request, response);
            } else {
                String[] pathParts = pathInfo.split("/");
                if (pathParts.length == 2) {
                    handleGetApproval(pathParts[1], response);
                } else {
                    sendError(response, 400, "Invalid request path");
                }
            }
        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/finance-approvals", e);
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

            if (pathInfo != null && pathInfo.contains("/approve")) {
                handleApprove(request, response, currentUser);
            } else if (pathInfo != null && pathInfo.contains("/reject")) {
                handleReject(request, response, currentUser);
            } else {
                handleCreateApproval(request, response, currentUser);
            }

        } catch (TicketTrackerException e) {
            handleException(response, e);
        } catch (Exception e) {
            logger.error("Unexpected error in POST /api/finance-approvals", e);
            sendError(response, 500, "Internal server error");
        }
    }

    private void handleGetApprovals(HttpServletRequest request, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        String ticketIdParam = request.getParameter("ticketId");
        String status = request.getParameter("status");

        if (ticketIdParam != null) {
            byte[] ticketId = hexToBytes(ticketIdParam);
            List<FinanceApproval> approvals = financeApprovalService.getFinanceApprovalsByTicketId(ticketId);
            sendJsonResponse(response, approvals);
        } else if ("pending".equals(status)) {
            List<FinanceApproval> approvals = financeApprovalService.getPendingFinanceApprovals();
            sendJsonResponse(response, approvals);
        } else {
            sendError(response, 400, "ticketId parameter required");
        }
    }

    private void handleGetApproval(String approvalId, HttpServletResponse response)
            throws TicketTrackerException, IOException {
        byte[] id = hexToBytes(approvalId);
        FinanceApproval approval = financeApprovalService.getFinanceApprovalById(id);
        sendJsonResponse(response, approval);
    }

    private void handleCreateApproval(HttpServletRequest request, HttpServletResponse response,
                                      User currentUser) throws TicketTrackerException, IOException {
        String body = getRequestBody(request);
        FinanceApproval approval = objectMapper.readValue(body, FinanceApproval.class);

        FinanceApproval createdApproval = financeApprovalService.createFinanceApproval(
                approval, currentUser.getId());

        response.setStatus(HttpServletResponse.SC_CREATED);
        sendJsonResponse(response, createdApproval);
    }

    private void handleApprove(HttpServletRequest request, HttpServletResponse response,
                               User currentUser) throws TicketTrackerException, IOException {
        String pathInfo = request.getPathInfo();
        String[] pathParts = pathInfo.split("/");

        if (pathParts.length < 3) {
            sendError(response, 400, "Approval ID required");
            return;
        }

        byte[] approvalId = hexToBytes(pathParts[1]);
        FinanceApproval approval = financeApprovalService.approveFinanceApproval(
                approvalId, currentUser.getId());

        sendJsonResponse(response, approval);
    }

    private void handleReject(HttpServletRequest request, HttpServletResponse response,
                              User currentUser) throws TicketTrackerException, IOException {
        String pathInfo = request.getPathInfo();
        String[] pathParts = pathInfo.split("/");

        if (pathParts.length < 3) {
            sendError(response, 400, "Approval ID required");
            return;
        }

        String body = getRequestBody(request);
        Map<String, Object> data = objectMapper.readValue(body, Map.class);
        String rejectionReason = (String) data.get("rejectionReason");

        byte[] approvalId = hexToBytes(pathParts[1]);
        FinanceApproval approval = financeApprovalService.rejectFinanceApproval(
                approvalId, rejectionReason, currentUser.getId());

        sendJsonResponse(response, approval);
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
