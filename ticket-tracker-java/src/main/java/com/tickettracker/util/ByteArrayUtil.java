package com.tickettracker.util;

import java.util.UUID;

public class ByteArrayUtil {

    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return null;
        }

        if ("null".equalsIgnoreCase(hex) || "undefined".equalsIgnoreCase(hex)) {
            return null;
        }

        hex = hex.replaceAll("-", "");

        if (!hex.matches("^[0-9A-Fa-f]+$")) {
            return null;
        }

        if (hex.length() % 2 != 0) {
            return null;
        }

        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int highNibble = Character.digit(hex.charAt(i), 16);
            int lowNibble = Character.digit(hex.charAt(i + 1), 16);
            if (highNibble == -1 || lowNibble == -1) {
                return null;
            }
            data[i / 2] = (byte) ((highNibble << 4) + lowNibble);
        }
        return data;
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static String bytesToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }

        StringBuilder hex = new StringBuilder(bytesToHex(bytes));
        hex.insert(8, '-');
        hex.insert(13, '-');
        hex.insert(18, '-');
        hex.insert(23, '-');

        return hex.toString();
    }

    public static byte[] uuidToBytes(String uuid) {
        if (uuid == null) {
            return null;
        }
        return hexToBytes(uuid.replaceAll("-", ""));
    }

    public static byte[] generateUuidBytes() {
        UUID uuid = UUID.randomUUID();
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        byte[] bytes = new byte[16];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (8 * (7 - i)));
        }
        for (int i = 8; i < 16; i++) {
            bytes[i] = (byte) (lsb >>> (8 * (7 - i)));
        }
        return bytes;
    }

    public static boolean equals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }
}
