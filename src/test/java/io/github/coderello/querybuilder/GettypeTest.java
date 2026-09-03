package io.github.coderello.querybuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.coderello.querybuilder.internal.JsType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 1:1 migration of {@code tests/gettype.test.js}. */
class GettypeTest {

    @Test
    @DisplayName("should detect type correctly")
    void shouldDetectTypeCorrectly() {
        assertEquals("string", JsType.of("hello"));
        assertEquals("string", JsType.of(new StringBuilder("hello")));
        assertEquals("object", JsType.of(Map.of()));
        assertEquals("object", JsType.of(new LinkedHashMap<>()));
        assertEquals("array", JsType.of(List.of()));
        assertEquals("array", JsType.of(new ArrayList<>()));
        assertEquals("number", JsType.of(2));
        assertEquals("number", JsType.of(Integer.valueOf(4)));
    }
}
