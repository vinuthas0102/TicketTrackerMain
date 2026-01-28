package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;

/**
 * WorkflowStepFileReference model representing file references linked to workflow steps
 * and tracking their upload status.
 */
public class WorkflowStepFileReference {

    private byte[] id;
    private byte[] stepId;
    private byte[] templateId;
    private String referenceName;
    private boolean isMandatory;
    private byte[] documentId;
    private byte[] uploadedBy;
    private Timestamp uploadedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Transient fields for joined data
    private String uploadedByName;
    private String documentName;

    public WorkflowStepFileReference() {
        this.isMandatory = false;
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

    @JsonIgnore
    public byte[] getStepId() {
        return stepId;
    }

    public void setStepId(byte[] stepId) {
        this.stepId = stepId;
    }

    @JsonProperty("stepId")
    public String getStepIdAsString() {
        return bytesToUuid(stepId);
    }

    @JsonIgnore
    public byte[] getTemplateId() {
        return templateId;
    }

    public void setTemplateId(byte[] templateId) {
        this.templateId = templateId;
    }

    @JsonProperty("templateId")
    public String getTemplateIdAsString() {
        return bytesToUuid(templateId);
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    @JsonIgnore
    public byte[] getDocumentId() {
        return documentId;
    }

    public void setDocumentId(byte[] documentId) {
        this.documentId = documentId;
    }

    @JsonProperty("documentId")
    public String getDocumentIdAsString() {
        return bytesToUuid(documentId);
    }

    @JsonIgnore
    public byte[] getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(byte[] uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    @JsonProperty("uploadedBy")
    public String getUploadedByAsString() {
        return bytesToUuid(uploadedBy);
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
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

    public String getUploadedByName() {
        return uploadedByName;
    }

    public void setUploadedByName(String uploadedByName) {
        this.uploadedByName = uploadedByName;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    private String bytesToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02x", bytes[i]));
            if (i == 3 || i == 5 || i == 7 || i == 9) {
                sb.append("-");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "WorkflowStepFileReference{" +
                "referenceName='" + referenceName + '\'' +
                ", isMandatory=" + isMandatory +
                ", documentId=" + (documentId != null ? "uploaded" : "pending") +
                '}';
    }
}
