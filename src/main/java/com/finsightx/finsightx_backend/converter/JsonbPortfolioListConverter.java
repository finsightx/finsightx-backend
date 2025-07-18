package com.finsightx.finsightx_backend.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.finsightx.finsightx_backend.domain.PortfolioItem;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter(autoApply = false)
public class JsonbPortfolioListConverter implements AttributeConverter<List<PortfolioItem>, String> {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<PortfolioItem> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting List<PortfolioItem> to JSON string", e);
        }
    }

    @Override
    public List<PortfolioItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<PortfolioItem>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON string to List<PortfolioItem>", e);
        }
    }
}
