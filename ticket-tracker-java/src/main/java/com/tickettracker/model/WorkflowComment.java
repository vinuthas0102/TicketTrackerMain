package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tickettracker.util.UuidUtil;
import java.sql.Timestamp;

public class WorkflowComment {

    private byte[] id;
    private byte[] stepId;
    private String content;
    private byte[] createdBy;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    private String createdByName;
    private String createdByRole;

    public WorkflowComment() {
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
        return UuidUtil.bytesToUuidString(id);
    }

    @JsonProperty("id")
    public void setIdAsString(String idStr) {
        this.id = UuidUtil.uuidStringToBytes(idStr);
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
        return UuidUtil.bytesToUuidString(stepId);
    }

    @JsonProperty("stepId")
    public void setStepIdAsString(String stepIdStr) {
        this.stepId = UuidUtil.uuidStringToBytes(stepIdStr);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
        return UuidUtil.bytesToUuidString(createdBy);
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

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCreatedByRole() {
        return createdByRole;
    }

    public void setCreatedByRole(String createdByRole) {
        this.createdByRole = createdByRole;
    }
}
