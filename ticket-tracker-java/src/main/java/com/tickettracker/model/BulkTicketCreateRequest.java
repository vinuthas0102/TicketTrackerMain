package com.tickettracker.model;

import java.util.List;

public class BulkTicketCreateRequest {
    private List<Ticket> tickets;
    private String moduleId;
    private String createdBy;

    public BulkTicketCreateRequest() {
    }

    public BulkTicketCreateRequest(List<Ticket> tickets, String moduleId, String createdBy) {
        this.tickets = tickets;
        this.moduleId = moduleId;
        this.createdBy = createdBy;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
