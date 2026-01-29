package com.tickettracker.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonObjectToStringDeserializer extends JsonDeserializer<String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node == null || node.isNull()) {
            return "{}";
        }

        if (node.isTextual()) {
            String text = node.asText();
            if (text == null || text.trim().isEmpty()) {
                return "{}";
            }
            return text;
        }

        if (node.isObject() || node.isArray()) {
            return objectMapper.writeValueAsString(node);
        }

        return "{}";
    }
}
