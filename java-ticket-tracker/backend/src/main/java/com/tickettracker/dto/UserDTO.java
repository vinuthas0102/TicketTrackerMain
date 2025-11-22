package com.tickettracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String id;
    private String name;
    private String email;
    private String role;
    private String department;
    private String avatar;
    private Boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
