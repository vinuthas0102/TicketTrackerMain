package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tickettracker.util.UuidUtil;
import java.sql.Timestamp;

public class FileAttachment {

    private byte[] id;
    private byte[] ticketId;
    private byte[] stepId;
    private String fileName;
    private long fileSize;
    private String fileType;
    private String fileUrl;
    private byte[] uploadedBy;
    private Timestamp createdAt;

    public FileAttachment() {
    }

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

    @JsonProperty("ticket_id")
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

    @JsonProperty("step_id")
    public void setStepIdAsString(String stepIdStr) {
        this.stepId = UuidUtil.uuidStringToBytes(stepIdStr);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
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

    @JsonProperty("uploaded_by")
    public void setUploadedByAsString(String uploadedByStr) {
        this.uploadedBy = UuidUtil.uuidStringToBytes(uploadedByStr);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    private String bytesToUuid(byte[] bytes) {
        return UuidUtil.bytesToUuidString(bytes);
    }

    @Override
    public String toString() {
        return "FileAttachment{" +
                "fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", fileType='" + fileType + '\'' +
                '}';
    }
}
