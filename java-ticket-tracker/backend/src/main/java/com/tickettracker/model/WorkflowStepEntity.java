package com.tickettracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "workflow_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketEntity ticket;

    @Column(name = "step_number", nullable = false, length = 50)
    private String stepNumber;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description")
    @Lob
    private String description;

    @Column(name = "status", length = 50)
    private String status = "pending";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private UserEntity assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_step_id")
    private WorkflowStepEntity parentStep;

    @Column(name = "level_1")
    private Integer level1;

    @Column(name = "level_2")
    private Integer level2;

    @Column(name = "level_3")
    private Integer level3;

    @Column(name = "dependencies", length = 4000)
    private String dependencies;

    @Column(name = "is_parallel")
    private Boolean isParallel = false;

    @Column(name = "dependency_mode", length = 20)
    private String dependencyMode;

    @Column(name = "is_dependency_locked")
    private Boolean isDependencyLocked = false;

    @Column(name = "progress", precision = 5, scale = 2)
    private BigDecimal progress = BigDecimal.ZERO;

    @Column(name = "mandatory_documents", length = 4000)
    private String mandatoryDocuments;

    @Column(name = "optional_documents", length = 4000)
    private String optionalDocuments;

    @Column(name = "completion_certificate_required")
    private Boolean completionCertificateRequired = false;

    @Column(name = "due_date")
    private Timestamp dueDate;

    @Column(name = "start_date")
    private Timestamp startDate;

    @Column(name = "data")
    @Lob
    private String data;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    // Relationships
    @OneToMany(mappedBy = "parentStep")
    private List<WorkflowStepEntity> childSteps;

    @OneToMany(mappedBy = "step")
    private List<WorkflowCommentEntity> comments;

    @OneToMany(mappedBy = "step")
    private List<DocumentEntity> documents;
}
