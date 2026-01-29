package com.tickettracker.service;

import com.tickettracker.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class UserPreferencesService {

    private static final Logger logger = LoggerFactory.getLogger(UserPreferencesService.class);

    public UserPreferencesService() {
    }

    public Map<String, Object> getUserPreferences(byte[] userId) throws TicketTrackerException {
        logger.info("Fetching user preferences for user: {}", bytesToHex(userId));

        Map<String, Object> preferences = new HashMap<>();
        preferences.put("userId", bytesToHex(userId));
        preferences.put("preferences", new HashMap<>());

        return preferences;
    }

    public void saveUserPreferences(byte[] userId, Map<String, Object> preferences)
            throws TicketTrackerException {
        logger.info("Saving user preferences for user: {}", bytesToHex(userId));

    }

    public void deleteUserPreferences(byte[] userId) throws TicketTrackerException {
        logger.info("Deleting user preferences for user: {}", bytesToHex(userId));

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
