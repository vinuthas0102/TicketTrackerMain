package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileReferenceTemplate model representing JSON-based file reference templates
 * for defining required documents for workflow steps.
 */
public class FileReferenceTemplate {

    private static final Logger logger = LoggerFactory.getLogger(FileReferenceTemplate.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private byte[] id;
    private String templateName;
    private String description;
    private String jsonContent; // JSON string containing fileReferences array
    private byte[] uploadedBy;
    private boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Transient field for joined data
    private String uploadedByName;

    public FileReferenceTemplate() {
        this.isActive = true;
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

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonIgnore
    public String getJsonContent() {
        return jsonContent;
    }

    public void setJsonContent(String jsonContent) {
        this.jsonContent = jsonContent;
    }

    @JsonProperty("jsonContent")
    public Object getJsonContentAsObject() {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return new HashMap<String, Object>();
        }

        try {
            return objectMapper.readValue(jsonContent, Object.class);
        } catch (Exception e) {
            logger.warn("Failed to parse jsonContent as JSON object, returning as string: {}", e.getMessage());
            return jsonContent;
        }
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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
        return "FileReferenceTemplate{" +
                "templateName='" + templateName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
