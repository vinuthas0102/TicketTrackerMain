package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tickettracker.deserializer.JsonObjectToStringDeserializer;
import com.tickettracker.serializer.StringToJsonObjectSerializer;
import com.tickettracker.util.UuidUtil;
import java.sql.Timestamp;

/**
 * Module model representing workflow modules/categories.
 * Examples: Maintenance Tracker, Complaints Tracker, RTI Tracker, etc.
 */
public class Module {

    private byte[] id;
    private String name;
    private String description;
    private String icon;
    private String color;
    private String schemaId;
    @JsonDeserialize(using = JsonObjectToStringDeserializer.class)
    @JsonSerialize(using = StringToJsonObjectSerializer.class)
    private String config; // JSON string
    private boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Module() {
    }

    public Module(String name, String schemaId) {
        this.name = name;
        this.schemaId = schemaId;
        this.active = true;
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
        return id != null ? bytesToUuid(id) : null;
    }

    @JsonProperty("id")
    public void setIdAsString(String idStr) {
        this.id = UuidUtil.uuidStringToBytes(idStr);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @JsonProperty("schema_id")
    public String getSchemaId() {
        return schemaId;
    }

    @JsonProperty("schema_id")
    public void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @JsonProperty("created_at")
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @JsonProperty("created_at")
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @JsonProperty("updated_at")
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    @JsonProperty("updated_at")
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    private String bytesToUuid(byte[] bytes) {
        return UuidUtil.bytesToUuidString(bytes);
    }

    @Override
    public String toString() {
        return "Module{" +
                "id=" + getIdAsString() +
                ", name='" + name + '\'' +
                ", schemaId='" + schemaId + '\'' +
                ", active=" + active +
                '}';
    }
}
