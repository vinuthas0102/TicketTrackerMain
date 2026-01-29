package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tickettracker.deserializer.JsonObjectToStringDeserializer;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class WorkflowStepUpdateRequest {

    private byte[] id;
    private String title;
    private String description;
    private String status;

    @JsonProperty("assigned_to")
    private byte[] assignedTo;

    @JsonProperty("due_date")
    private Timestamp dueDate;

    @JsonProperty("start_date")
    private Timestamp startDate;

    @JsonProperty("is_parallel")
    private Boolean isParallel;

    private BigDecimal progress;

    @JsonDeserialize(using = JsonObjectToStringDeserializer.class)
    @JsonProperty("dependency_mode")
    private String dependencyMode;

    @JsonDeserialize(using = JsonObjectToStringDeserializer.class)
    @JsonProperty("mandatory_documents")
    private String mandatoryDocuments;

    @JsonDeserialize(using = JsonObjectToStringDeserializer.class)
    @JsonProperty("optional_documents")
    private String optionalDocuments;

    @JsonProperty("completion_certificate_required")
    private Boolean completionCertificateRequired;

    @JsonProperty("completed_at")
    private Timestamp completedAt;

    @JsonDeserialize(using = JsonObjectToStringDeserializer.class)
    private String data;

    @JsonIgnore
    public byte[] getId() {
        return id;
    }

    public void setId(byte[] id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonIgnore
    public byte[] getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(byte[] assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Timestamp getDueDate() {
        return dueDate;
    }

    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Boolean getIsParallel() {
        return isParallel;
    }

    public void setIsParallel(Boolean isParallel) {
        this.isParallel = isParallel;
    }

    public BigDecimal getProgress() {
        return progress;
    }

    public void setProgress(BigDecimal progress) {
        this.progress = progress;
    }

    public String getDependencyMode() {
        return dependencyMode;
    }

    public void setDependencyMode(String dependencyMode) {
        this.dependencyMode = dependencyMode;
    }

    public String getMandatoryDocuments() {
        return mandatoryDocuments;
    }

    public void setMandatoryDocuments(String mandatoryDocuments) {
        this.mandatoryDocuments = mandatoryDocuments;
    }

    public String getOptionalDocuments() {
        return optionalDocuments;
    }

    public void setOptionalDocuments(String optionalDocuments) {
        this.optionalDocuments = optionalDocuments;
    }

    public Boolean getCompletionCertificateRequired() {
        return completionCertificateRequired;
    }

    public void setCompletionCertificateRequired(Boolean completionCertificateRequired) {
        this.completionCertificateRequired = completionCertificateRequired;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
