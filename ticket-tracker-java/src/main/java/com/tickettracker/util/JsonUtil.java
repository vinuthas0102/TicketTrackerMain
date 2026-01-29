package com.tickettracker.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Utility class for JSON serialization and deserialization using Jackson.
 */
public class JsonUtil {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    /**
     * Convert object to JSON string
     *
     * @param object object to convert
     * @return JSON string
     * @throws IOException if serialization fails
     */
    public static String toJson(Object object) throws IOException {
        if (object == null) {
            return null;
        }
        return objectMapper.writeValueAsString(object);
    }

    /**
     * Convert object to pretty-printed JSON string
     *
     * @param object object to convert
     * @return formatted JSON string
     * @throws IOException if serialization fails
     */
    public static String toPrettyJson(Object object) throws IOException {
        if (object == null) {
            return null;
        }
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    /**
     * Convert JSON string to object of specified class
     *
     * @param json JSON string
     * @param clazz target class
     * @param <T> type parameter
     * @return deserialized object
     * @throws IOException if deserialization fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return objectMapper.readValue(json, clazz);
    }

    /**
     * Convert object to JSON string safely (returns empty JSON object on error)
     *
     * @param object object to convert
     * @return JSON string or "{}" on error
     */
    public static String toJsonSafe(Object object) {
        try {
            return toJson(object);
        } catch (IOException e) {
            logger.error("Error converting object to JSON", e);
            return "{}";
        }
    }

    /**
     * Convert JSON string to object safely (returns null on error)
     *
     * @param json JSON string
     * @param clazz target class
     * @param <T> type parameter
     * @return deserialized object or null on error
     */
    public static <T> T fromJsonSafe(String json, Class<T> clazz) {
        try {
            return fromJson(json, clazz);
        } catch (IOException e) {
            logger.error("Error parsing JSON to object", e);
            return null;
        }
    }

    /**
     * Get the ObjectMapper instance for custom usage
     *
     * @return ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
