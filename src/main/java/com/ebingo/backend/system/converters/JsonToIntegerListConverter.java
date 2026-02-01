package com.ebingo.backend.system.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.util.List;

@ReadingConverter
@RequiredArgsConstructor
public class JsonToIntegerListConverter implements Converter<String, List<Integer>> {

    private final ObjectMapper objectMapper;

    @Override
    public List<Integer> convert(String source) {

        try {
            return objectMapper.readValue(source, new TypeReference<List<Integer>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
