package com.github.dockerjava.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Monkey patched {@link FiltersEncoder} to avoid a use of jaxrs.
 * See https://github.com/docker-java/docker-java/issues/1176
 */
public class FiltersEncoder {

    private FiltersEncoder() {
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String jsonEncode(Map<String, List<String>> filters) {
        try {
            return OBJECT_MAPPER.writeValueAsString(filters);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
