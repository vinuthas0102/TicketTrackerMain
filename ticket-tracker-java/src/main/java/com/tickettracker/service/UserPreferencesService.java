package com.tickettracker.service;

import com.tickettracker.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserPreferencesService {

    private static final Logger logger = LoggerFactory.getLogger(UserPreferencesService.class);

    private static final Map<String, Map<String, Object>> preferencesStore = new ConcurrentHashMap<>();

    private static final String DEFAULT_ICON_DISPLAY_TYPE = "dropdown_menu";
    private static final String DEFAULT_ICON_SIZE = "medium";
    private static final boolean DEFAULT_SHOW_LABELS = true;
    private static final boolean DEFAULT_GROUP_BY_CATEGORY = false;
    private static final boolean DEFAULT_ANIMATION_ENABLED = true;

    public UserPreferencesService() {
    }

    public Map<String, Object> getUserPreferences(byte[] userId) throws TicketTrackerException {
        String userIdHex = bytesToHex(userId);
        logger.info("Fetching user preferences for user: {}", userIdHex);

        Map<String, Object> stored = preferencesStore.get(userIdHex);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userIdHex);
        result.put("iconDisplayType", stored != null ? stored.getOrDefault("iconDisplayType", DEFAULT_ICON_DISPLAY_TYPE) : DEFAULT_ICON_DISPLAY_TYPE);
        result.put("iconSize", stored != null ? stored.getOrDefault("iconSize", DEFAULT_ICON_SIZE) : DEFAULT_ICON_SIZE);
        result.put("showLabels", stored != null ? stored.getOrDefault("showLabels", DEFAULT_SHOW_LABELS) : DEFAULT_SHOW_LABELS);
        result.put("groupByCategory", stored != null ? stored.getOrDefault("groupByCategory", DEFAULT_GROUP_BY_CATEGORY) : DEFAULT_GROUP_BY_CATEGORY);
        result.put("animationEnabled", stored != null ? stored.getOrDefault("animationEnabled", DEFAULT_ANIMATION_ENABLED) : DEFAULT_ANIMATION_ENABLED);

        return result;
    }

    public void saveUserPreferences(byte[] userId, Map<String, Object> preferences)
            throws TicketTrackerException {
        String userIdHex = bytesToHex(userId);
        logger.info("Saving user preferences for user: {}", userIdHex);

        if (preferences == null) return;

        Map<String, Object> existing = preferencesStore.computeIfAbsent(userIdHex, k -> new HashMap<>());

        if (preferences.containsKey("iconDisplayType")) {
            existing.put("iconDisplayType", preferences.get("iconDisplayType"));
        }
        if (preferences.containsKey("iconSize")) {
            existing.put("iconSize", preferences.get("iconSize"));
        }
        if (preferences.containsKey("showLabels")) {
            existing.put("showLabels", preferences.get("showLabels"));
        }
        if (preferences.containsKey("groupByCategory")) {
            existing.put("groupByCategory", preferences.get("groupByCategory"));
        }
        if (preferences.containsKey("animationEnabled")) {
            existing.put("animationEnabled", preferences.get("animationEnabled"));
        }
    }

    public void deleteUserPreferences(byte[] userId) throws TicketTrackerException {
        String userIdHex = bytesToHex(userId);
        logger.info("Deleting user preferences for user: {}", userIdHex);
        preferencesStore.remove(userIdHex);
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
