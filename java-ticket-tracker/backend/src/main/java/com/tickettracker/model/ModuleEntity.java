package com.tickettracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "modules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "icon", length = 100)
    private String icon = "FileText";

    @Column(name = "color", length = 100)
    private String color = "from-blue-500 to-indigo-500";

    @Column(name = "schema_id", nullable = false, length = 100)
    private String schemaId;

    @Column(name = "config")
    @Lob
    private String config;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    // Relationships
    @OneToMany(mappedBy = "module")
    private List<TicketEntity> tickets;

    @OneToMany(mappedBy = "module")
    private List<ModuleFieldConfigurationEntity> fieldConfigurations;
}
