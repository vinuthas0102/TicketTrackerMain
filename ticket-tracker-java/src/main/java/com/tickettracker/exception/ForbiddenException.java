package com.tickettracker.exception;

public class ForbiddenException extends TicketTrackerException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message, 403);
    }

    public ForbiddenException() {
        super("FORBIDDEN", "Access denied", 403);
    }
}
