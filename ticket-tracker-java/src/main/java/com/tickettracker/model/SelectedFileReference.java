package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SelectedFileReference {

    private String referenceName;
    private boolean isMandatory;

    public SelectedFileReference() {
        this.isMandatory = false;
    }

    public SelectedFileReference(String referenceName, boolean isMandatory) {
        this.referenceName = referenceName;
        this.isMandatory = isMandatory;
    }

    @JsonProperty("referenceName")
    public String getReferenceName() {
        return referenceName;
    }

    @JsonProperty("referenceName")
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

    @Override
    public String toString() {
        return "SelectedFileReference{" +
                "referenceName='" + referenceName + '\'' +
                ", isMandatory=" + isMandatory +
                '}';
    }
}
