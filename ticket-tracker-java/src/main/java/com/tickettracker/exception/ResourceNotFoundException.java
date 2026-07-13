package com.tickettracker.exception;

public class ResourceNotFoundException extends TicketTrackerException {

    public ResourceNotFoundException(String resource, String id) {
        super("RESOURCE_NOT_FOUND",
              String.format("%s with ID %s not found", resource, id),
              404);
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, 404);
    }
}
