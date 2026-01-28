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

public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserDAO userDAO;
    private final SecureRandom secureRandom;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.secureRandom = new SecureRandom();
    }

    public User authenticate(String email, String password) throws TicketTrackerException {
        try {
            User user = userDAO.findByEmail(email);
            if (user == null) {
                logger.warn("Authentication failed: User not found - {}", email);
                throw new UnauthorizedException("Invalid email or password");
            }

            if (!user.isActive()) {
                logger.warn("Authentication failed: User inactive - {}", email);
                throw new UnauthorizedException("Account is inactive");
            }

            if (!verifyPassword(password, user.getPasswordHash(), user.getPasswordSalt())) {
                logger.warn("Authentication failed: Invalid password - {}", email);
                throw new UnauthorizedException("Invalid email or password");
            }

            logger.info("User authenticated successfully: {}", email);
            return user;

        } catch (SQLException e) {
            logger.error("Database error during authentication", e);
            throw new DatabaseException("Authentication failed", e);
        }
    }

    public User register(User user, String password) throws TicketTrackerException {
        try {
            validateRegistration(user, password);

            User existingUser = userDAO.findByEmail(user.getEmail());
            if (existingUser != null) {
                throw new ValidationException("email", "Email already registered");
            }

            String salt = generateSalt();
            String passwordHash = hashPassword(password, salt);

            user.setPasswordHash(passwordHash);
            user.setPasswordSalt(salt);
            user.setActive(true);

            User createdUser = userDAO.create(user);

            logger.info("User registered successfully: {}", user.getEmail());
            return createdUser;

        } catch (SQLException e) {
            logger.error("Database error during registration", e);
            throw new DatabaseException("Registration failed", e);
        }
    }

    public void changePassword(byte[] userId, String currentPassword, String newPassword)
            throws TicketTrackerException {
        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                throw new ResourceNotFoundException("User", bytesToHex(userId));
            }

            if (!verifyPassword(currentPassword, user.getPasswordHash(), user.getPasswordSalt())) {
                throw new UnauthorizedException("Current password is incorrect");
            }

            validatePassword(newPassword);

            String newSalt = generateSalt();
            String newPasswordHash = hashPassword(newPassword, newSalt);

            user.setPasswordHash(newPasswordHash);
            user.setPasswordSalt(newSalt);
            userDAO.update(user);

            logger.info("Password changed successfully for user: {}", user.getEmail());

        } catch (SQLException e) {
            logger.error("Database error during password change", e);
            throw new DatabaseException("Password change failed", e);
        }
    }

    public void resetPassword(String email, String newPassword) throws TicketTrackerException {
        try {
            User user = userDAO.findByEmail(email);
            if (user == null) {
                throw new ResourceNotFoundException("User", email);
            }

            validatePassword(newPassword);

            String newSalt = generateSalt();
            String newPasswordHash = hashPassword(newPassword, newSalt);

            user.setPasswordHash(newPasswordHash);
            user.setPasswordSalt(newSalt);
            userDAO.update(user);

            logger.info("Password reset successfully for user: {}", email);

        } catch (SQLException e) {
            logger.error("Database error during password reset", e);
            throw new DatabaseException("Password reset failed", e);
        }
    }

    public boolean hasRole(User user, String... roles) {
        if (user == null || roles == null) {
            return false;
        }

        String userRole = user.getRole();
        for (String role : roles) {
            if (role.equalsIgnoreCase(userRole)) {
                return true;
            }
        }
        return false;
    }

    public void checkPermission(User user, String requiredRole) throws ForbiddenException {
        if (!hasRole(user, requiredRole)) {
            throw new ForbiddenException(
                    String.format("User does not have required role: %s", requiredRole));
        }
    }

    private void validateRegistration(User user, String password) throws ValidationException {
        ValidationException validation = new ValidationException("Registration validation failed");

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

    private boolean verifyPassword(String password, String storedHash, String salt) {
        String computedHash = hashPassword(password, salt);
        return computedHash.equals(storedHash);
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
