package com.tickettracker.exception;

public class UnauthorizedException extends TicketTrackerException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message, 401);
    }

    public UnauthorizedException() {
        super("UNAUTHORIZED", "Authentication required", 401);
    }
}
