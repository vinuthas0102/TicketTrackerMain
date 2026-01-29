package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tickettracker.util.UuidUtil;
import java.sql.Timestamp;

/**
 * AuditLog model for tracking all system actions and changes.
 */
public class AuditLog {

    private byte[] id;
    private byte[] ticketId;
    private byte[] stepId;
    private byte[] performedBy;
    private String action;
    private String oldData;
    private String newData;
    private String description;
    private String actionCategory;
    private String metadata; // JSON string
    private Timestamp performedAt;

    // Transient field
    private String performedByName;

    public AuditLog() {
        this.actionCategory = "ticket_action";
    }

    public AuditLog(byte[] ticketId, byte[] performedBy, String action, String description) {
        this.ticketId = ticketId;
        this.performedBy = performedBy;
        this.action = action;
        this.description = description;
        this.actionCategory = "ticket_action";
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

    @JsonIgnore
    public byte[] getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(byte[] performedBy) {
        this.performedBy = performedBy;
    }

    @JsonProperty("performedBy")
    public String getPerformedByAsString() {
        return bytesToUuid(performedBy);
    }

    @JsonProperty("performedBy")
    public void setPerformedByAsString(String performedByStr) {
        this.performedBy = UuidUtil.uuidStringToBytes(performedByStr);
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOldData() {
        return oldData;
    }

    public void setOldData(String oldData) {
        this.oldData = oldData;
    }

    public String getNewData() {
        return newData;
    }

    public void setNewData(String newData) {
        this.newData = newData;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getActionCategory() {
        return actionCategory;
    }

    public void setActionCategory(String actionCategory) {
        this.actionCategory = actionCategory;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Timestamp getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(Timestamp performedAt) {
        this.performedAt = performedAt;
    }

    public String getPerformedByName() {
        return performedByName;
    }

    public void setPerformedByName(String performedByName) {
        this.performedByName = performedByName;
    }

    private String bytesToUuid(byte[] bytes) {
        return UuidUtil.bytesToUuidString(bytes);
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "action='" + action + '\'' +
                ", actionCategory='" + actionCategory + '\'' +
                ", performedAt=" + performedAt +
                '}';
    }
}
