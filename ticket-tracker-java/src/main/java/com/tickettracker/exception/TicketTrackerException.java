package com.tickettracker.exception;

public class TicketTrackerException extends Exception {

    private String errorCode;
    private int httpStatus;

    public TicketTrackerException(String message) {
        super(message);
        this.httpStatus = 500;
    }

    public TicketTrackerException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = 500;
    }

    public TicketTrackerException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public TicketTrackerException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public TicketTrackerException(String errorCode, String message, Throwable cause, int httpStatus) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
