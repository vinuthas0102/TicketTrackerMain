package com.tickettracker.util;

import java.util.UUID;

/**
 * Utility class for converting between UUID strings and Oracle RAW(16) byte arrays.
 */
public class UuidUtil {

    /**
     * Convert UUID string to byte array for Oracle RAW(16)
     * Accepts both formats: with hyphens (550e8400-e29b-41d4-a716-446655440000)
     * or without hyphens (550E8400E29B41D4A716446655440000)
     *
     * @param uuidString UUID string in either format
     * @return byte array of 16 bytes
     */
    public static byte[] uuidStringToBytes(String uuidString) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            return null;
        }

        try {
            String normalized = uuidString.trim();

            // If the string doesn't contain hyphens, assume it's a raw hex string
            if (!normalized.contains("-")) {
                // Remove any spaces and validate length
                normalized = normalized.replace(" ", "");

                if (normalized.length() != 32) {
                    throw new IllegalArgumentException("Invalid UUID string length: " + uuidString);
                }

                // Insert hyphens at correct positions for standard UUID format
                normalized = String.format("%s-%s-%s-%s-%s",
                    normalized.substring(0, 8),
                    normalized.substring(8, 12),
                    normalized.substring(12, 16),
                    normalized.substring(16, 20),
                    normalized.substring(20, 32)
                );
            }

            UUID uuid = UUID.fromString(normalized);
            byte[] bytes = new byte[16];

            long mostSigBits = uuid.getMostSignificantBits();
            long leastSigBits = uuid.getLeastSignificantBits();

            for (int i = 0; i < 8; i++) {
                bytes[i] = (byte) (mostSigBits >>> (8 * (7 - i)));
            }
            for (int i = 8; i < 16; i++) {
                bytes[i] = (byte) (leastSigBits >>> (8 * (15 - i)));
            }

            return bytes;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID string: " + uuidString, e);
        }
    }

    /**
     * Convert byte array from Oracle RAW(16) to UUID string
     *
     * @param bytes byte array of 16 bytes
     * @return UUID string like "550e8400-e29b-41d4-a716-446655440000"
     */
    public static String bytesToUuidString(byte[] bytes) {
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

    /**
     * Generate a new random UUID as byte array
     *
     * @return byte array of 16 bytes
     */
    public static byte[] generateUuidBytes() {
        UUID uuid = UUID.randomUUID();
        return uuidStringToBytes(uuid.toString());
    }

    /**
     * Check if a string is a valid UUID format
     *
     * @param uuidString string to check
     * @return true if valid UUID format
     */
    public static boolean isValidUuid(String uuidString) {
        if (uuidString == null) {
            return false;
        }
        try {
            UUID.fromString(uuidString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
