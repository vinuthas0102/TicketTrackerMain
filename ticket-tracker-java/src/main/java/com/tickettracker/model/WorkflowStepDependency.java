package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tickettracker.util.UuidUtil;
import java.sql.Timestamp;

public class WorkflowStepDependency {
    private byte[] id;
    private byte[] stepId;
    private byte[] dependsOnStepId;
    private byte[] createdBy;
    private Timestamp createdAt;
    private boolean active;

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

    @JsonProperty("step_id")
    public void setStepIdAsString(String stepIdStr) {
        this.stepId = UuidUtil.uuidStringToBytes(stepIdStr);
    }

    @JsonIgnore
    public byte[] getDependsOnStepId() {
        return dependsOnStepId;
    }

    public void setDependsOnStepId(byte[] dependsOnStepId) {
        this.dependsOnStepId = dependsOnStepId;
    }

    @JsonProperty("dependsOnStepId")
    public String getDependsOnStepIdAsString() {
        return UuidUtil.bytesToUuidString(dependsOnStepId);
    }

    @JsonProperty("depends_on_step_id")
    public void setDependsOnStepIdAsString(String dependsOnStepIdStr) {
        this.dependsOnStepId = UuidUtil.uuidStringToBytes(dependsOnStepIdStr);
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

    @JsonProperty("created_by")
    public void setCreatedByAsString(String createdByStr) {
        this.createdBy = UuidUtil.uuidStringToBytes(createdByStr);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
