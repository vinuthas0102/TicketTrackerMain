package com.tickettracker.service;

import com.tickettracker.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class FieldValueService {

    private static final Logger logger = LoggerFactory.getLogger(FieldValueService.class);

    public FieldValueService() {
    }

    public Map<String, Object> getFieldValues(byte[] entityId, byte[] currentUserId)
            throws TicketTrackerException {
        logger.info("Fetching field values for entity: {} by user: {}",
            bytesToHex(entityId), bytesToHex(currentUserId));

        Map<String, Object> values = new HashMap<>();
        values.put("entityId", bytesToHex(entityId));
        values.put("values", new HashMap<>());

        return values;
    }

    public void saveFieldValues(byte[] entityId, Map<String, Object> values, byte[] currentUserId)
            throws TicketTrackerException {
        logger.info("Saving field values for entity: {} by user: {}",
            bytesToHex(entityId), bytesToHex(currentUserId));

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
