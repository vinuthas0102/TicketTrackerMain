package com.tickettracker.service;

import com.tickettracker.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class FieldConfigService {

    private static final Logger logger = LoggerFactory.getLogger(FieldConfigService.class);

    public FieldConfigService() {
    }

    public Map<String, Object> getFieldConfiguration(String entityType, byte[] currentUserId)
            throws TicketTrackerException {
        logger.info("Fetching field configuration for entity type: {} by user: {}",
            entityType, bytesToHex(currentUserId));

        Map<String, Object> config = new HashMap<>();
        config.put("entityType", entityType);
        config.put("fields", new Object[0]);

        return config;
    }

    public Map<String, Object> updateFieldConfiguration(String entityType,
                                                         Map<String, Object> configuration,
                                                         byte[] currentUserId)
            throws TicketTrackerException {
        logger.info("Updating field configuration for entity type: {} by user: {}",
            entityType, bytesToHex(currentUserId));

        return configuration;
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
