package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tickettracker.util.UuidUtil;
import java.sql.Timestamp;

/**
 * Document model representing file attachments for tickets and workflow steps.
 */
public class Document {

    private byte[] id;
    private byte[] ticketId;
    private byte[] stepId;
    private String name;
    private String type;
    private long size;
    private String url;
    private String storagePath;
    private byte[] uploadedBy;
    private Timestamp uploadedAt;
    private boolean isMandatory;
    private boolean isCompletionCertificate;
    private byte[] fileContent;  // For BLOB storage

    // Transient field
    private String uploadedByName;

    public Document() {
    }

    public Document(String name, String type, long size, String storagePath, byte[] uploadedBy) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.storagePath = storagePath;
        this.uploadedBy = uploadedBy;
        this.isMandatory = false;
        this.isCompletionCertificate = false;
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
        this.stepId = UuidUtil.uuidStringToBytes(stepIdStr);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
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
        this.uploadedBy = UuidUtil.uuidStringToBytes(uploadedByStr);
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    public boolean isCompletionCertificate() {
        return isCompletionCertificate;
    }

    public void setCompletionCertificate(boolean completionCertificate) {
        isCompletionCertificate = completionCertificate;
    }

    public String getUploadedByName() {
        return uploadedByName;
    }

    public void setUploadedByName(String uploadedByName) {
        this.uploadedByName = uploadedByName;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    private String bytesToUuid(byte[] bytes) {
        return UuidUtil.bytesToUuidString(bytes);
    }

    @Override
    public String toString() {
        return "Document{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", size=" + size +
                ", isMandatory=" + isMandatory +
                '}';
    }
}
