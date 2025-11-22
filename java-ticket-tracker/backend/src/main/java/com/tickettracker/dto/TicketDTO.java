package com.tickettracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {
    private String id;
    private String ticketNumber;
    private String moduleId;
    private String moduleName;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String createdById;
    private String createdByName;
    private String assignedToId;
    private String assignedToName;
    private Timestamp dueDate;
    private Timestamp startDate;
    private String propertyId;
    private String propertyLocation;
    private Boolean completionDocumentsRequired;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
