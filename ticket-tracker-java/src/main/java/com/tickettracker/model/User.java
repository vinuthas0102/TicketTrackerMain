package com.tickettracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tickettracker.util.UuidUtil;
import java.sql.Timestamp;

/**
 * User model representing system users with role-based access control.
 *
 * Roles:
 * - employee: Regular employee
 * - eo: Executive Officer (Admin)
 * - dept_officer: Department Officer (Manager)
 * - vendor: External vendor
 * - finance: Finance officer
 * - technician: Technician (sub-task assignee within a department)
 */
public class User {

    private byte[] id;
    private String username;
    private String name;
    private String email;
    private String role;
    private String department;

    @JsonIgnore
    private String passwordHash;

    @JsonIgnore
    private String passwordSalt;

    private String avatar;
    private boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp lastLogin;

    // Default constructor
    public User() {
    }

    // Constructor with essential fields
    public User(String name, String email, String role, String department) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.department = department;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Get role in uppercase format for frontend compatibility
     * Maps: eo -> EO, dept_officer -> DO, employee -> EMPLOYEE, vendor -> VENDOR, finance -> FINANCE
     */
    public String getRole() {
        if (role == null) return null;

        switch (role.toLowerCase()) {
            case "eo":
                return "EO";
            case "dept_officer":
                return "DO";
            case "employee":
                return "EMPLOYEE";
            case "vendor":
                return "VENDOR";
            case "finance":
                return "FINANCE";
            default:
                return role.toUpperCase();
        }
    }

    /**
     * Set role from frontend format to database format
     * Maps: EO -> eo, DO -> dept_officer, EMPLOYEE -> employee, VENDOR -> vendor, FINANCE -> finance
     */
    public void setRole(String role) {
        if (role == null) {
            this.role = null;
            return;
        }

        switch (role.toUpperCase()) {
            case "EO":
                this.role = "eo";
                break;
            case "DO":
                this.role = "dept_officer";
                break;
            case "EMPLOYEE":
                this.role = "employee";
                break;
            case "VENDOR":
                this.role = "vendor";
                break;
            case "FINANCE":
                this.role = "finance";
                break;
            default:
                this.role = role.toLowerCase();
        }
    }

    /**
     * Get the raw role value from database (for internal use)
     */
    @JsonIgnore
    public String getRoleInternal() {
        return role;
    }

    /**
     * Set the raw role value from database (for internal use)
     * Does NOT perform any transformation
     */
    @JsonIgnore
    public void setRoleInternal(String role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
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

    @JsonProperty("lastLogin")
    public Timestamp getLastLogin() {
        return lastLogin;
    }

    @JsonProperty("lastLogin")
    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }

    // Utility methods

    /**
     * Check if user is an administrator (EO role)
     */
    public boolean isAdmin() {
        return "eo".equalsIgnoreCase(role);
    }

    /**
     * Check if user is a department officer (manager)
     */
    public boolean isManager() {
        return "dept_officer".equalsIgnoreCase(role);
    }

    /**
     * Check if user is a finance officer
     */
    public boolean isFinanceOfficer() {
        return "finance".equalsIgnoreCase(role);
    }

    /**
     * Check if user is a vendor
     */
    public boolean isVendor() {
        return "vendor".equalsIgnoreCase(role);
    }

    /**
     * Convert byte array (RAW) to UUID string
     */
    private String bytesToUuid(byte[] bytes) {
        return UuidUtil.bytesToUuidString(bytes);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + getIdAsString() +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", department='" + department + '\'' +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return email != null && email.equals(user.email);
    }

    @Override
    public int hashCode() {
        return email != null ? email.hashCode() : 0;
    }
}
