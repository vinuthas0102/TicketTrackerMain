package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tickettracker.deserializer.JsonObjectToStringDeserializer;
import com.tickettracker.util.UuidUtil;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class WorkflowStepUpdateRequest {

    private byte[] id;
    private String title;
    private String description;
    private String status;

    private byte[] assignedTo;

    private Timestamp dueDate;

    private Timestamp startDate;

    private Boolean isParallel;

    private BigDecimal progress;

    private String dependencyMode;

    private String mandatoryDocuments;

    private String optionalDocuments;

    private Boolean completionCertificateRequired;

    private Timestamp completedAt;

    private String data;

    private String remarks;

    @JsonIgnore
    public byte[] getId() {
        return id;
    }

    @JsonIgnore
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

    @JsonIgnore
    public void setAssignedTo(byte[] assignedTo) {
        this.assignedTo = assignedTo;
    }

    @JsonProperty("assignedTo")
    public void setAssignedToAsString(String assignedToStr) {
        this.assignedTo = (assignedToStr != null && !assignedToStr.trim().isEmpty())
            ? UuidUtil.uuidStringToBytes(assignedToStr)
            : null;
    }

    @JsonProperty("dueDate")
    public Timestamp getDueDate() {
        return dueDate;
    }

    @JsonProperty("dueDate")
    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    @JsonProperty("startDate")
    public Timestamp getStartDate() {
        return startDate;
    }

    @JsonProperty("startDate")
    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    @JsonProperty("is_parallel")
    public Boolean getIsParallel() {
        return isParallel;
    }

    @JsonProperty("is_parallel")
    public void setIsParallel(Boolean isParallel) {
        this.isParallel = isParallel;
    }

    public BigDecimal getProgress() {
        return progress;
    }

    public void setProgress(BigDecimal progress) {
        this.progress = progress;
    }

    @JsonProperty("dependencyMode")
    public String getDependencyMode() {
        return dependencyMode;
    }

    @JsonDeserialize(using = JsonObjectToStringDeserializer.class)
    @JsonProperty("dependencyMode")
    public void setDependencyMode(String dependencyMode) {
        this.dependencyMode = dependencyMode;
    }

    @JsonProperty("mandatoryDocuments")
    public String getMandatoryDocuments() {
        return mandatoryDocuments;
    }

    @JsonDeserialize(using = JsonObjectToStringDeserializer.class)
    @JsonProperty("mandatoryDocuments")
    public void setMandatoryDocuments(String mandatoryDocuments) {
        this.mandatoryDocuments = mandatoryDocuments;
    }

    @JsonProperty("optionalDocuments")
    public String getOptionalDocuments() {
        return optionalDocuments;
    }

    @JsonDeserialize(using = JsonObjectToStringDeserializer.class)
    @JsonProperty("optionalDocuments")
    public void setOptionalDocuments(String optionalDocuments) {
        this.optionalDocuments = optionalDocuments;
    }

    @JsonProperty("completionCertificateRequired")
    public Boolean getCompletionCertificateRequired() {
        return completionCertificateRequired;
    }

    @JsonProperty("completionCertificateRequired")
    public void setCompletionCertificateRequired(Boolean completionCertificateRequired) {
        this.completionCertificateRequired = completionCertificateRequired;
    }

    @JsonProperty("completedAt")
    public Timestamp getCompletedAt() {
        return completedAt;
    }

    @JsonProperty("completedAt")
    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    @JsonProperty("data")
    public String getData() {
        return data;
    }

    @JsonDeserialize(using = JsonObjectToStringDeserializer.class)
    @JsonProperty("data")
    public void setData(String data) {
        this.data = data;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
