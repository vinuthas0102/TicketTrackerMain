package com.tickettracker.exception;

import java.sql.SQLException;

public class DatabaseException extends TicketTrackerException {

    public DatabaseException(String message) {
        super("DATABASE_ERROR", message, 500);
    }

    public DatabaseException(String message, SQLException cause) {
        super("DATABASE_ERROR", message, cause, 500);
    }

    public DatabaseException(SQLException cause) {
        super("DATABASE_ERROR",
              String.format("Database error: %s", cause.getMessage()),
              cause,
              500);
    }
}
