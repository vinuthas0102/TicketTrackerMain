package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tickettracker.deserializer.JsonObjectToStringDeserializer;
import com.tickettracker.util.UuidUtil;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * WorkflowStep model representing hierarchical workflow steps for tickets.
 */
public class WorkflowStep {

    private byte[] id;
    private byte[] ticketId;
    private String stepNumber;
    private String title;
    private String description;
    private String status;
    private byte[] assignedTo;
    private byte[] parentStepId;
    private Integer level1;
    private Integer level2;
    private Integer level3;
    @JsonDeserialize(using = JsonObjectToStringDeserializer.class)
    private String dependencies; // Comma-separated or JSON
    private boolean isParallel;
    private String mandatoryDocuments; // Comma-separated or JSON
    private String optionalDocuments; // Comma-separated or JSON
    private boolean completionCertificateRequired;
    private Timestamp dueDate;
    @JsonDeserialize(using = JsonObjectToStringDeserializer.class)
    private String data; // JSON string
    private BigDecimal progress;
    private String dependencyMode; // 'all' or 'any_one'
    private boolean isDependencyLocked;
    private byte[] createdBy;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp completedAt;
    private Timestamp startDate;

    // Transient fields for joined data
    private String assignedToName;
    private String createdByName;

    public WorkflowStep() {
        this.status = "pending";
        this.isParallel = false;
        this.completionCertificateRequired = false;
        this.progress = BigDecimal.ZERO;
        this.dependencyMode = "all";
        this.isDependencyLocked = false;
    }

    // Getters and Setters

    @JsonIgnore
    public byte[] getId() {
        return id;
    }

    public void setId(byte[] id) {
        this.id = id;
    }

    @JsonProperty("id")
    public String getIdAsString() {
        return bytesToUuid(id);
    }

    @JsonProperty("id")
    public void setIdAsString(String idStr) {
        this.id = UuidUtil.uuidStringToBytes(idStr);
    }

    @JsonIgnore
    public byte[] getTicketId() {
        return ticketId;
    }

    public void setTicketId(byte[] ticketId) {
        this.ticketId = ticketId;
    }

    @JsonProperty("ticketId")
    public String getTicketIdAsString() {
        return bytesToUuid(ticketId);
    }

    @JsonProperty("ticketId")
    public void setTicketIdAsString(String ticketIdStr) {
        this.ticketId = UuidUtil.uuidStringToBytes(ticketIdStr);
    }

    public String getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(String stepNumber) {
        this.stepNumber = stepNumber;
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

    @JsonProperty("assignedTo")
    public String getAssignedToAsString() {
        return bytesToUuid(assignedTo);
    }

    @JsonProperty("assignedTo")
    public void setAssignedToAsString(String assignedToStr) {
        this.assignedTo = UuidUtil.uuidStringToBytes(assignedToStr);
    }

    @JsonIgnore
    public byte[] getParentStepId() {
        return parentStepId;
    }

    public void setParentStepId(byte[] parentStepId) {
        this.parentStepId = parentStepId;
    }

    @JsonProperty("parentStepId")
    public String getParentStepIdAsString() {
        return bytesToUuid(parentStepId);
    }

    @JsonProperty("parentStepId")
    public void setParentStepIdAsString(String parentStepIdStr) {
        this.parentStepId = UuidUtil.uuidStringToBytes(parentStepIdStr);
    }

    public Integer getLevel1() {
        return level1;
    }

    public void setLevel1(Integer level1) {
        this.level1 = level1;
    }

    public Integer getLevel2() {
        return level2;
    }

    public void setLevel2(Integer level2) {
        this.level2 = level2;
    }

    public Integer getLevel3() {
        return level3;
    }

    public void setLevel3(Integer level3) {
        this.level3 = level3;
    }

    public String getDependencies() {
        return dependencies;
    }

    public void setDependencies(String dependencies) {
        this.dependencies = dependencies;
    }

    public boolean isParallel() {
        return isParallel;
    }

    public void setParallel(boolean parallel) {
        isParallel = parallel;
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

    public boolean isCompletionCertificateRequired() {
        return completionCertificateRequired;
    }

    public void setCompletionCertificateRequired(boolean completionCertificateRequired) {
        this.completionCertificateRequired = completionCertificateRequired;
    }

    public Timestamp getDueDate() {
        return dueDate;
    }

    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
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

    public boolean isDependencyLocked() {
        return isDependencyLocked;
    }

    public void setDependencyLocked(boolean dependencyLocked) {
        isDependencyLocked = dependencyLocked;
    }

    @JsonIgnore
    public byte[] getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(byte[] createdBy) {
        this.createdBy = createdBy;
    }

    @JsonProperty("createdBy")
    public String getCreatedByAsString() {
        return bytesToUuid(createdBy);
    }

    @JsonProperty("createdBy")
    public void setCreatedByAsString(String createdByStr) {
        this.createdBy = UuidUtil.uuidStringToBytes(createdByStr);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public String getAssignedToName() {
        return assignedToName;
    }

    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    private String bytesToUuid(byte[] bytes) {
        return UuidUtil.bytesToUuidString(bytes);
    }

    @Override
    public String toString() {
        return "WorkflowStep{" +
                "stepNumber='" + stepNumber + '\'' +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", progress=" + progress +
                '}';
    }
}
