package com.tickettracker.service;

import com.tickettracker.dao.UserDAO;
import com.tickettracker.exception.*;
import com.tickettracker.model.User;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;

public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserDAO userDAO;
    private final SecureRandom secureRandom;

    public UserService() {
        this.userDAO = new UserDAO();
        this.secureRandom = new SecureRandom();
    }

    public User getUserById(byte[] userId) throws TicketTrackerException {
        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                throw new ResourceNotFoundException("User", bytesToHex(userId));
            }
            return user;
        } catch (SQLException e) {
            logger.error("Error fetching user by ID", e);
            throw new DatabaseException("Failed to fetch user", e);
        }
    }

    public User getUserByEmail(String email) throws TicketTrackerException {
        try {
            User user = userDAO.findByEmail(email);
            if (user == null) {
                throw new ResourceNotFoundException("User", email);
            }
            return user;
        } catch (SQLException e) {
            logger.error("Error fetching user by email", e);
            throw new DatabaseException("Failed to fetch user", e);
        }
    }

    public List<User> getAllUsers() throws TicketTrackerException {
        try {
            return userDAO.findAll();
        } catch (SQLException e) {
            logger.error("Error fetching all users", e);
            throw new DatabaseException("Failed to fetch users", e);
        }
    }

    public List<User> getUsersByRole(String role) throws TicketTrackerException {
        try {
            return userDAO.findByRole(role);
        } catch (SQLException e) {
            logger.error("Error fetching users by role", e);
            throw new DatabaseException("Failed to fetch users by role", e);
        }
    }

    public User createUser(User user, String password, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            validateUserCreation(user, password);

            User existingUser = userDAO.findByEmail(user.getEmail());
            if (existingUser != null) {
                throw new ValidationException("email", "Email already exists");
            }

            String salt = generateSalt();
            String passwordHash = hashPassword(password, salt);

            user.setPasswordHash(passwordHash);
            user.setPasswordSalt(salt);
            user.setActive(true);

            User createdUser = userDAO.create(user);

            logger.info("User created: {} by user: {}",
                user.getEmail(), bytesToHex(currentUserId));

            return createdUser;
        } catch (SQLException e) {
            logger.error("Error creating user", e);
            throw new DatabaseException("Failed to create user", e);
        }
    }

    public User updateUser(User user, byte[] currentUserId) throws TicketTrackerException {
        try {
            User existingUser = getUserById(user.getId());

            validateUserUpdate(user);

            if (!existingUser.getEmail().equals(user.getEmail())) {
                User emailCheck = userDAO.findByEmail(user.getEmail());
                if (emailCheck != null && !java.util.Arrays.equals(emailCheck.getId(), user.getId())) {
                    throw new ValidationException("email", "Email already in use by another user");
                }
            }

            boolean updated = userDAO.update(user);
            if (!updated) {
                throw new DatabaseException("Failed to update user");
            }

            logger.info("User updated: {} by user: {}",
                user.getEmail(), bytesToHex(currentUserId));

            return userDAO.findById(user.getId());
        } catch (SQLException e) {
            logger.error("Error updating user", e);
            throw new DatabaseException("Failed to update user", e);
        }
    }

    public void deactivateUser(byte[] userId, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            User user = getUserById(userId);

            if (java.util.Arrays.equals(userId, currentUserId)) {
                throw new ValidationException("userId", "Cannot deactivate your own account");
            }

            user.setActive(false);
            userDAO.update(user);

            logger.info("User deactivated: {} by user: {}",
                user.getEmail(), bytesToHex(currentUserId));

        } catch (SQLException e) {
            logger.error("Error deactivating user", e);
            throw new DatabaseException("Failed to deactivate user", e);
        }
    }

    public void activateUser(byte[] userId, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            User user = getUserById(userId);
            user.setActive(true);
            userDAO.update(user);

            logger.info("User activated: {} by user: {}",
                user.getEmail(), bytesToHex(currentUserId));

        } catch (SQLException e) {
            logger.error("Error activating user", e);
            throw new DatabaseException("Failed to activate user", e);
        }
    }

    public void updateUserPassword(byte[] userId, String newPassword, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            User user = getUserById(userId);

            validatePassword(newPassword);

            String newSalt = generateSalt();
            String newPasswordHash = hashPassword(newPassword, newSalt);

            boolean updated = userDAO.updatePassword(userId, newPasswordHash, newSalt);
            if (!updated) {
                throw new DatabaseException("Failed to update password");
            }

            logger.info("Password updated for user: {} by user: {}",
                user.getEmail(), bytesToHex(currentUserId));

        } catch (SQLException e) {
            logger.error("Error updating user password", e);
            throw new DatabaseException("Failed to update password", e);
        }
    }

    public void updateUserRole(byte[] userId, String newRole, byte[] currentUserId)
            throws TicketTrackerException {
        try {
            User user = getUserById(userId);

            if (newRole == null || newRole.trim().isEmpty()) {
                throw new ValidationException("role", "Role is required");
            }

            if (java.util.Arrays.equals(userId, currentUserId)) {
                throw new ValidationException("userId", "Cannot change your own role");
            }

            user.setRole(newRole);
            userDAO.update(user);

            logger.info("User role updated: {} to {} by user: {}",
                user.getEmail(), newRole, bytesToHex(currentUserId));

        } catch (SQLException e) {
            logger.error("Error updating user role", e);
            throw new DatabaseException("Failed to update user role", e);
        }
    }

    private void validateUserCreation(User user, String password) throws ValidationException {
        ValidationException validation = new ValidationException("User validation failed");

        if (user.getName() == null || user.getName().trim().isEmpty()) {
            validation.addError("Name is required");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            validation.addError("Email is required");
        } else if (!isValidEmail(user.getEmail())) {
            validation.addError("Invalid email format");
        }

        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            validation.addError("Role is required");
        }

        if (user.getDepartment() == null || user.getDepartment().trim().isEmpty()) {
            validation.addError("Department is required");
        }

        validatePassword(password);

        if (validation.getValidationErrors().size() > 0) {
            throw validation;
        }
    }

    private void validateUserUpdate(User user) throws ValidationException {
        ValidationException validation = new ValidationException("User validation failed");

        if (user.getName() == null || user.getName().trim().isEmpty()) {
            validation.addError("Name is required");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            validation.addError("Email is required");
        } else if (!isValidEmail(user.getEmail())) {
            validation.addError("Invalid email format");
        }

        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            validation.addError("Role is required");
        }

        if (user.getDepartment() == null || user.getDepartment().trim().isEmpty()) {
            validation.addError("Department is required");
        }

        if (validation.getValidationErrors().size() > 0) {
            throw validation;
        }
    }

    private void validatePassword(String password) throws ValidationException {
        if (password == null || password.length() < 8) {
            throw new ValidationException("password",
                "Password must be at least 8 characters long");
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private String generateSalt() {
        byte[] salt = new byte[32];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) {
        String saltedPassword = password + salt;
        return DigestUtils.sha256Hex(saltedPassword);
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
