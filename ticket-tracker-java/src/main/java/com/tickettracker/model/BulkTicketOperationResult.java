package com.tickettracker.model;

import java.util.ArrayList;
import java.util.List;

public class BulkTicketOperationResult {
    private int successCount;
    private int failedCount;
    private int totalCount;
    private List<BulkTicketError> errors;
    private List<String> createdTicketIds;

    public BulkTicketOperationResult() {
        this.errors = new ArrayList<>();
        this.createdTicketIds = new ArrayList<>();
        this.successCount = 0;
        this.failedCount = 0;
        this.totalCount = 0;
    }

    public BulkTicketOperationResult(int totalCount) {
        this();
        this.totalCount = totalCount;
    }

    public void addSuccess(byte[] ticketId) {
        this.successCount++;
        this.createdTicketIds.add(bytesToHex(ticketId));
    }

    public void addFailure(int index, String title, String error) {
        this.failedCount++;
        this.errors.add(new BulkTicketError(index, title, error));
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<BulkTicketError> getErrors() {
        return errors;
    }

    public void setErrors(List<BulkTicketError> errors) {
        this.errors = errors;
    }

    public List<String> getCreatedTicketIds() {
        return createdTicketIds;
    }

    public void setCreatedTicketIds(List<String> createdTicketIds) {
        this.createdTicketIds = createdTicketIds;
    }

    public static class BulkTicketError {
        private int index;
        private String title;
        private String error;

        public BulkTicketError() {
        }

        public BulkTicketError(int index, String title, String error) {
            this.index = index;
            this.title = title;
            this.error = error;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
