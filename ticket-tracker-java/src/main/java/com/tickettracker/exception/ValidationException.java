package com.tickettracker.exception;

import java.util.ArrayList;
import java.util.List;

public class ValidationException extends TicketTrackerException {

    private List<String> validationErrors;

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message, 400);
        this.validationErrors = new ArrayList<>();
    }

    public ValidationException(List<String> errors) {
        super("VALIDATION_ERROR", "Validation failed", 400);
        this.validationErrors = errors;
    }

    public ValidationException(String field, String message) {
        super("VALIDATION_ERROR", String.format("%s: %s", field, message), 400);
        this.validationErrors = new ArrayList<>();
        this.validationErrors.add(String.format("%s: %s", field, message));
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public void addError(String error) {
        if (validationErrors == null) {
            validationErrors = new ArrayList<>();
        }
        validationErrors.add(error);
    }
}
