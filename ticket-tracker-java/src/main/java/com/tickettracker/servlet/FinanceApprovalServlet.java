package com.tickettracker.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickettracker.dao.FinanceApprovalDAO.FinanceApproval;
import com.tickettracker.exception.TicketTrackerException;
import com.tickettracker.model.User;
import com.tickettracker.service.FinanceApprovalService;
import com.tickettracker.util.JsonUtil;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@WebServlet("/api/finance-approvals/*")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 20 * 1024 * 1024)
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
        Map<String, Object> data = objectMapper.readValue(body, Map.class);

        FinanceApproval approval = new FinanceApproval();

        if (data.get("ticketId") != null) {
            approval.setTicketId(hexToBytes(data.get("ticketId").toString()));
        }
        if (data.get("financeOfficerId") != null) {
            approval.setFinanceOfficerId(hexToBytes(data.get("financeOfficerId").toString()));
        }
        if (data.get("tentativeCost") != null) {
            approval.setTentativeCost(new java.math.BigDecimal(data.get("tentativeCost").toString()));
        }
        if (data.get("costDeductedFrom") != null) {
            approval.setCostDeductedFrom(data.get("costDeductedFrom").toString());
        }
        if (data.get("remarks") != null) {
            approval.setRemarks(data.get("remarks").toString());
        }
        if (data.get("requestedBy") != null) {
            approval.setSubmittedBy(hexToBytes(data.get("requestedBy").toString()));
        }

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

        String remarks = null;
        String contentType = request.getContentType();

        if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
            try {
                remarks = getPartAsString(request, "remarks");
            } catch (Exception e) {
                logger.warn("Could not read remarks part from multipart request: {}", e.getMessage());
            }
        } else {
            String body = getRequestBody(request);
            if (body != null && !body.trim().isEmpty()) {
                try {
                    Map<String, Object> data = objectMapper.readValue(body, Map.class);
                    remarks = data.get("remarks") != null ? data.get("remarks").toString() : null;
                } catch (Exception e) {
                    logger.warn("Could not parse JSON body for approve request: {}", e.getMessage());
                }
            }
        }

        String approvalIdStr = pathParts[1];
        if (approvalIdStr == null || approvalIdStr.trim().isEmpty()) {
            sendError(response, 400, "Approval ID required");
            return;
        }
        byte[] approvalId = hexToBytes(approvalIdStr);
        FinanceApproval approval = financeApprovalService.approveFinanceApproval(
                approvalId, remarks, currentUser.getId());

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

        String rejectionReason = null;
        String contentType = request.getContentType();

        if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
            try {
                rejectionReason = getPartAsString(request, "rejectionReason");
            } catch (Exception e) {
                logger.warn("Could not read rejectionReason part from multipart request: {}", e.getMessage());
            }
        } else {
            String body = getRequestBody(request);
            if (body != null && !body.trim().isEmpty()) {
                try {
                    Map<String, Object> data = objectMapper.readValue(body, Map.class);
                    rejectionReason = data.get("rejectionReason") != null ? data.get("rejectionReason").toString() : null;
                } catch (Exception e) {
                    logger.warn("Could not parse JSON body for reject request: {}", e.getMessage());
                }
            }
        }

        String approvalIdStr = pathParts[1];
        if (approvalIdStr == null || approvalIdStr.trim().isEmpty()) {
            sendError(response, 400, "Approval ID required");
            return;
        }
        byte[] approvalId = hexToBytes(approvalIdStr);
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

    private String getPartAsString(HttpServletRequest request, String partName) throws IOException, ServletException {
        Part part = request.getPart(partName);
        if (part == null) return null;
        try (InputStream is = part.getInputStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            String value = buffer.toString(StandardCharsets.UTF_8.name()).trim();
            return value.isEmpty() ? null : value;
        }
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

    private byte[] hexToBytes(String uuidOrHex) {
        if (uuidOrHex == null || uuidOrHex.isEmpty()) {
            return null;
        }
        return UuidUtil.uuidStringToBytes(uuidOrHex);
    }

    private static class ErrorResponse {
        private int status;
        private String message;
        private ErrorDetail error;

        public ErrorResponse(int status, String message) {
            this.status = status;
            this.message = message;
            this.error = new ErrorDetail(message);
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public ErrorDetail getError() {
            return error;
        }
    }

    private static class ErrorDetail {
        private String code = "API_ERROR";
        private String message;

        public ErrorDetail(String message) {
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
