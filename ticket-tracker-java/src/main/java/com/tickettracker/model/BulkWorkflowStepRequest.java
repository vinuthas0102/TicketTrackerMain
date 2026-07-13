package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tickettracker.util.UuidUtil;

import java.util.List;

public class BulkWorkflowStepRequest {

    private byte[] ticketId;
    private List<WorkflowStep> steps;
    private byte[] userId;
    private byte[] parentStepId;

    public BulkWorkflowStepRequest() {}

    @JsonIgnore
    public byte[] getTicketId() {
        return ticketId;
    }

    @JsonIgnore
    public void setTicketId(byte[] ticketId) {
        this.ticketId = ticketId;
    }

    @JsonProperty("ticketId")
    public String getTicketIdAsString() {
        return UuidUtil.bytesToUuidString(ticketId);
    }

    @JsonProperty("ticketId")
    public void setTicketIdAsString(String ticketIdStr) {
        this.ticketId = UuidUtil.uuidStringToBytes(ticketIdStr);
    }

    public List<WorkflowStep> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkflowStep> steps) {
        this.steps = steps;
    }

    @JsonIgnore
    public byte[] getUserId() {
        return userId;
    }

    @JsonIgnore
    public void setUserId(byte[] userId) {
        this.userId = userId;
    }

    @JsonProperty("userId")
    public String getUserIdAsString() {
        return UuidUtil.bytesToUuidString(userId);
    }

    @JsonProperty("userId")
    public void setUserIdAsString(String userIdStr) {
        this.userId = UuidUtil.uuidStringToBytes(userIdStr);
    }

    @JsonIgnore
    public byte[] getParentStepId() {
        return parentStepId;
    }

    @JsonIgnore
    public void setParentStepId(byte[] parentStepId) {
        this.parentStepId = parentStepId;
    }

    @JsonProperty("parentStepId")
    public String getParentStepIdAsString() {
        return UuidUtil.bytesToUuidString(parentStepId);
    }

    @JsonProperty("parentStepId")
    public void setParentStepIdAsString(String parentStepIdStr) {
        this.parentStepId = parentStepIdStr != null ? UuidUtil.uuidStringToBytes(parentStepIdStr) : null;
    }
}
