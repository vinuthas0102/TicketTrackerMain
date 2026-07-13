package com.tickettracker.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StringToJsonObjectSerializer extends JsonSerializer<String> {

    private static final Logger logger = LoggerFactory.getLogger(StringToJsonObjectSerializer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.trim().isEmpty()) {
            gen.writeStartObject();
            gen.writeEndObject();
            return;
        }

        try {
            Object jsonObject = objectMapper.readValue(value, Object.class);
            gen.writeObject(jsonObject);
        } catch (Exception e) {
            logger.warn("Failed to parse config string as JSON, writing as empty object: {}", e.getMessage());
            gen.writeStartObject();
            gen.writeEndObject();
        }
    }
}
