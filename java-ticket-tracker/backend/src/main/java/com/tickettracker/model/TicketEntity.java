package com.tickettracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "ticket_number", unique = true, nullable = false, length = 50)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private ModuleEntity module;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", nullable = false)
    @Lob
    private String description;

    @Column(name = "status", length = 50)
    private String status = "open";

    @Column(name = "priority", length = 50)
    private String priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private UserEntity assignedTo;

    @Column(name = "due_date")
    private Timestamp dueDate;

    @Column(name = "start_date")
    private Timestamp startDate;

    @Column(name = "data")
    @Lob
    private String data;

    @Column(name = "property_id", length = 100)
    private String propertyId;

    @Column(name = "property_location", length = 200)
    private String propertyLocation;

    @Column(name = "completion_documents_required")
    private Boolean completionDocumentsRequired = true;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    // Relationships
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<WorkflowStepEntity> workflowSteps;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<DocumentEntity> documents;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<AuditLogEntity> auditLogs;
}
