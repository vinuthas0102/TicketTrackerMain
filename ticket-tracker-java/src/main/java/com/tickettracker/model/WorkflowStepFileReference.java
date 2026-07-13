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
    private Long documentSize;

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

    @JsonProperty("id")
    public void setIdAsString(String idStr) {
        this.id = hexToBytes(idStr);
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

    @JsonProperty("stepId")
    public void setStepIdAsString(String stepIdStr) {
        this.stepId = hexToBytes(stepIdStr);
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

    @JsonProperty("templateId")
    public void setTemplateIdAsString(String templateIdStr) {
        this.templateId = hexToBytes(templateIdStr);
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    @JsonProperty("isMandatory")
    public boolean isMandatory() {
        return isMandatory;
    }

    @JsonProperty("isMandatory")
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

    @JsonProperty("documentId")
    public void setDocumentIdAsString(String documentIdStr) {
        this.documentId = hexToBytes(documentIdStr);
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

    @JsonProperty("uploadedBy")
    public void setUploadedByAsString(String uploadedByStr) {
        this.uploadedBy = hexToBytes(uploadedByStr);
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

    public Long getDocumentSize() {
        return documentSize;
    }

    public void setDocumentSize(Long documentSize) {
        this.documentSize = documentSize;
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

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return null;
        }
        String cleanHex = hex.replace("-", "");
        int len = cleanHex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(cleanHex.charAt(i), 16) << 4)
                    + Character.digit(cleanHex.charAt(i + 1), 16));
        }
        return data;
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
