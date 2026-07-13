package com.tickettracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "username", unique = true, length = 200)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 200)
    private String email;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "department", nullable = false, length = 200)
    private String department;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "last_login")
    private Timestamp lastLogin;

    // Relationships
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    private List<TicketEntity> createdTickets;

    @OneToMany(mappedBy = "assignedTo")
    private List<TicketEntity> assignedTickets;

    @OneToMany(mappedBy = "createdBy")
    private List<WorkflowStepEntity> createdSteps;

    @OneToMany(mappedBy = "assignedTo")
    private List<WorkflowStepEntity> assignedSteps;
}
