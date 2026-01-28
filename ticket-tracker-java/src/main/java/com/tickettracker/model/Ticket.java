package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tickettracker.util.UuidUtil;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Ticket model representing main workflow instances.
 */
public class Ticket {

    private byte[] id;
    private String ticketNumber;
    private byte[] moduleId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private byte[] createdBy;
    private byte[] assignedTo;
    private Timestamp dueDate;
    private String data; // JSON string
    private String category;
    private String department;
    private String propertyId;
    private String propertyLocation;
    private boolean completionDocumentsRequired;
    private byte[] financeOfficerId;
    private int financeSubmissionCount;
    private String latestFinanceStatus;
    private boolean requiresFinanceApproval;
    private Timestamp startDate;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Transient fields for joined data
    private String createdByName;
    private String assignedToName;
    private String moduleName;
    private String financeOfficerName;

    // Transient fields for related entities
    private List<WorkflowStep> workflow;
    private List<FileAttachment> attachments;
    private List<AuditLog> auditTrail;

    public Ticket() {
        this.workflow = new ArrayList<>();
        this.attachments = new ArrayList<>();
        this.auditTrail = new ArrayList<>();
    }

    public Ticket(String ticketNumber, byte[] moduleId, String title, String description, byte[] createdBy) {
        this();
        this.ticketNumber = ticketNumber;
        this.moduleId = moduleId;
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
        this.status = "open";
        this.completionDocumentsRequired = true;
        this.requiresFinanceApproval = true;
        this.financeSubmissionCount = 0;
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

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    @JsonIgnore
    public byte[] getModuleId() {
        return moduleId;
    }

    public void setModuleId(byte[] moduleId) {
        this.moduleId = moduleId;
    }

    @JsonProperty("moduleId")
    public String getModuleIdAsString() {
        return bytesToUuid(moduleId);
    }

    @JsonProperty("module_id")
    public void setModuleIdAsString(String moduleIdStr) {
        this.moduleId = UuidUtil.uuidStringToBytes(moduleIdStr);
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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
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

    @JsonProperty("created_by")
    public void setCreatedByAsString(String createdByStr) {
        this.createdBy = UuidUtil.uuidStringToBytes(createdByStr);
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

    @JsonProperty("assigned_to")
    public void setAssignedToAsString(String assignedToStr) {
        this.assignedTo = UuidUtil.uuidStringToBytes(assignedToStr);
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getPropertyLocation() {
        return propertyLocation;
    }

    public void setPropertyLocation(String propertyLocation) {
        this.propertyLocation = propertyLocation;
    }

    public boolean isCompletionDocumentsRequired() {
        return completionDocumentsRequired;
    }

    public void setCompletionDocumentsRequired(boolean completionDocumentsRequired) {
        this.completionDocumentsRequired = completionDocumentsRequired;
    }

    @JsonIgnore
    public byte[] getFinanceOfficerId() {
        return financeOfficerId;
    }

    public void setFinanceOfficerId(byte[] financeOfficerId) {
        this.financeOfficerId = financeOfficerId;
    }

    @JsonProperty("financeOfficerId")
    public String getFinanceOfficerIdAsString() {
        return bytesToUuid(financeOfficerId);
    }

    @JsonProperty("finance_officer_id")
    public void setFinanceOfficerIdAsString(String financeOfficerIdStr) {
        this.financeOfficerId = UuidUtil.uuidStringToBytes(financeOfficerIdStr);
    }

    public int getFinanceSubmissionCount() {
        return financeSubmissionCount;
    }

    public void setFinanceSubmissionCount(int financeSubmissionCount) {
        this.financeSubmissionCount = financeSubmissionCount;
    }

    public String getLatestFinanceStatus() {
        return latestFinanceStatus;
    }

    public void setLatestFinanceStatus(String latestFinanceStatus) {
        this.latestFinanceStatus = latestFinanceStatus;
    }

    public boolean isRequiresFinanceApproval() {
        return requiresFinanceApproval;
    }

    public void setRequiresFinanceApproval(boolean requiresFinanceApproval) {
        this.requiresFinanceApproval = requiresFinanceApproval;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
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

    // Transient fields

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getAssignedToName() {
        return assignedToName;
    }

    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getFinanceOfficerName() {
        return financeOfficerName;
    }

    public void setFinanceOfficerName(String financeOfficerName) {
        this.financeOfficerName = financeOfficerName;
    }

    public List<WorkflowStep> getWorkflow() {
        return workflow;
    }

    public void setWorkflow(List<WorkflowStep> workflow) {
        this.workflow = workflow != null ? workflow : new ArrayList<>();
    }

    public List<FileAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<FileAttachment> attachments) {
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }

    public List<AuditLog> getAuditTrail() {
        return auditTrail;
    }

    public void setAuditTrail(List<AuditLog> auditTrail) {
        this.auditTrail = auditTrail != null ? auditTrail : new ArrayList<>();
    }

    // Utility methods

    private String bytesToUuid(byte[] bytes) {
        return UuidUtil.bytesToUuidString(bytes);
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "ticketNumber='" + ticketNumber + '\'' +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", department='" + department + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}
