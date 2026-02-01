package com.ebingo.backend.system.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.util.List;

@WritingConverter
@RequiredArgsConstructor
public class IntegerListToJsonConverter implements Converter<List<Integer>, String> {

    private final ObjectMapper objectMapper;

    @Override
    public String convert(List<Integer> source) {

        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
