package com.tickettracker.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for loading application properties files.
 */
public class PropertiesLoader {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesLoader.class);
    private static Properties applicationProperties;

    static {
        loadApplicationProperties();
    }

    /**
     * Load application.properties file
     */
    private static void loadApplicationProperties() {
        applicationProperties = new Properties();
        try (InputStream input = PropertiesLoader.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.warn("application.properties file not found");
                return;
            }
            applicationProperties.load(input);
            logger.info("Application properties loaded successfully");
        } catch (IOException e) {
            logger.error("Error loading application properties", e);
        }
    }

    /**
     * Get property value by key
     *
     * @param key property key
     * @return property value or null if not found
     */
    public static String getProperty(String key) {
        return applicationProperties.getProperty(key);
    }

    /**
     * Get property value by key with default value
     *
     * @param key property key
     * @param defaultValue default value if property not found
     * @return property value or default value
     */
    public static String getProperty(String key, String defaultValue) {
        return applicationProperties.getProperty(key, defaultValue);
    }

    /**
     * Get property as integer
     *
     * @param key property key
     * @param defaultValue default value if property not found or not a valid integer
     * @return property value as integer
     */
    public static int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for property {}: {}", key, value);
            }
        }
        return defaultValue;
    }

    /**
     * Get property as boolean
     *
     * @param key property key
     * @param defaultValue default value if property not found
     * @return property value as boolean
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    /**
     * Get property as long
     *
     * @param key property key
     * @param defaultValue default value if property not found or not a valid long
     * @return property value as long
     */
    public static long getLongProperty(String key, long defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid long value for property {}: {}", key, value);
            }
        }
        return defaultValue;
    }

    /**
     * Reload application properties (useful for runtime configuration changes)
     */
    public static synchronized void reload() {
        loadApplicationProperties();
    }
}
