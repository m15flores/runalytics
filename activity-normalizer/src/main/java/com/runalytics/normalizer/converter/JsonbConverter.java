package com.runalytics.normalizer.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.runalytics.normalizer.dto.ActivitySample;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class JsonbConverter implements AttributeConverter<List<ActivitySample>, String> {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public String convertToDatabaseColumn(List<ActivitySample> attribute) {
        if(attribute == null || attribute.isEmpty()) return null;
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting list to JSON", e);
        }
    }

    @Override
    public List<ActivitySample> convertToEntityAttribute(String dbData) {
        if(dbData == null || dbData.isEmpty()) return List.of();
        try {
            return mapper.readValue(dbData, new TypeReference<List<ActivitySample>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON to list", e);
        }
    }
}