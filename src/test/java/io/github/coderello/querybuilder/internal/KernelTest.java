package io.github.coderello.querybuilder.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pins the JavaScript semantics the builder depends on. Added by the migration.
 *
 * <p>Every expectation was recorded by executing the construct against the source repository on
 * Node, not derived from the specification.
 */
class KernelTest {

    @Nested
    @DisplayName("gettype")
    class GetType {

        @Test
        @DisplayName("primitives and their boxed forms share a tag")
        void primitiveTags() {
            assertEquals("string", JsType.of("hello"));
            assertEquals("string", JsType.of(new StringBuilder("hello")));
            assertEquals("number", JsType.of(2));
            assertEquals("number", JsType.of(2.5));
            assertEquals("number", JsType.of(Double.NaN), "NaN is still a number");
            assertEquals("number", JsType.of(Double.POSITIVE_INFINITY));
            assertEquals("boolean", JsType.of(true));
            assertEquals("null", JsType.of(null));
        }

        @Test
        @DisplayName("arrays and objects are distinguished, unlike instanceof Object")
        void structuralTags() {
            assertEquals("array", JsType.of(List.of()));
            assertEquals("array", JsType.of(new ArrayList<>()));
            assertEquals("array", JsType.of(new String[] {"a"}));
            assertEquals("array", JsType.of(new int[] {1}));
            assertEquals("object", JsType.of(Map.of()));
            assertEquals("object", JsType.of(new LinkedHashMap<>()));
            assertEquals("object", JsType.of(new Object()));
        }

        @Test
        void callableTags() {
            assertEquals("function", JsType.of((Consumer<Object>) o -> { }));
            assertEquals("function", JsType.of((Supplier<Object>) () -> null));
            assertTrue(JsType.isFunction((Consumer<Object>) o -> { }));
            assertFalse(JsType.isFunction("not callback"));
            assertFalse(JsType.isFunction(null));
        }
    }

    @Nested
    @DisplayName("truthiness")
    class Truthiness {

        @Test
        @DisplayName("the falsy set is exactly false 0 -0 \"\" null NaN")
        void falsy() {
            assertFalse(JsValues.isTruthy(null));
            assertFalse(JsValues.isTruthy(false));
            assertFalse(JsValues.isTruthy(0));
            assertFalse(JsValues.isTruthy(0.0));
            assertFalse(JsValues.isTruthy(-0.0));
            assertFalse(JsValues.isTruthy(""));
            assertFalse(JsValues.isTruthy(Double.NaN));
        }

        @Test
        @DisplayName("\"0\", \" \", [] and {} are all truthy")
        void truthy() {
            assertTrue(JsValues.isTruthy("0"));
            assertTrue(JsValues.isTruthy(" "));
            assertTrue(JsValues.isTruthy(List.of()));
            assertTrue(JsValues.isTruthy(Map.of()));
            assertTrue(JsValues.isTruthy(true));
            assertTrue(JsValues.isTruthy(-1));
        }
    }

    @Nested
    @DisplayName("String() conversion")
    class Stringification {

        @ParameterizedTest(name = "String({0}) === \"{1}\"")
        @CsvSource({
            "5,            5",
            "0,            0",
            "-0,           0",
            "1.5,          1.5",
            "1e21,         1e+21",
            "1e-7,         1e-7",
            "0.000001,     0.000001",
            "100000,       100000",
            "-1,           -1",
        })
        void numbers(double input, String expected) {
            assertEquals(expected, JsValues.numberToString(input));
        }

        @Test
        @DisplayName("Double.toString would diverge")
        void doubleToStringDiverges() {
            assertEquals("5.0", Double.toString(5.0), "documents the Java behaviour being avoided");
            assertEquals("5", JsValues.numberToString(5.0));
            assertEquals("1.0E21", Double.toString(1e21));
            assertEquals("1e+21", JsValues.numberToString(1e21));
        }

        @Test
        void integralBoxesRenderExactly() {
            assertEquals("5", JsValues.numberToString(Integer.valueOf(5)));
            assertEquals("9007199254740993", JsValues.numberToString(9007199254740993L));
        }

        @Test
        void otherValues() {
            assertEquals("null", JsValues.toJsString(null));
            assertEquals("true", JsValues.toJsString(true));
            assertEquals("1,2", JsValues.toJsString(List.of(1, 2)));
            assertEquals("", JsValues.toJsString(List.of()));
            assertEquals("[object Object]", JsValues.toJsString(Map.of()));
        }

        @Test
        @DisplayName("join renders null elements as empty, per Array.prototype.join")
        void joinDropsNulls() {
            assertEquals("a,,b", JsValues.join(Arrays.asList("a", null, "b"), ","));
            assertEquals("", JsValues.join(List.of(), ","));
        }
    }

    @Nested
    @DisplayName("Object.entries ordering")
    class EntryOrdering {

        @Test
        @DisplayName("integer-like keys come first in numeric order, then insertion order")
        void jsOrdering() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("2", "a");
            m.put("1", "b");
            m.put("b", "c");
            m.put("a", "d");

            assertEquals(List.of("1", "2", "b", "a"),
                    JsObject.entries(m).stream().map(JsObject.Entry::key).toList());
            assertEquals(List.of("2", "1", "b", "a"), List.copyOf(m.keySet()),
                    "documents the LinkedHashMap order being corrected");
        }

        @Test
        void numericOrderIsNotLexicographic() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("10", "x");
            m.put("9", "y");
            assertEquals(List.of("9", "10"),
                    JsObject.entries(m).stream().map(JsObject.Entry::key).toList());
        }

        @Test
        @DisplayName("only canonical uint32 forms count as array indices")
        void arrayIndexRules() {
            assertTrue(JsObject.isArrayIndex("0"));
            assertTrue(JsObject.isArrayIndex("42"));
            assertFalse(JsObject.isArrayIndex("01"), "not canonical");
            assertFalse(JsObject.isArrayIndex("1.0"));
            assertFalse(JsObject.isArrayIndex("-1"));
            assertFalse(JsObject.isArrayIndex(""));
            assertFalse(JsObject.isArrayIndex("4294967295"), "2^32-1 is not an index");
        }
    }

    @Nested
    @DisplayName("encodeURIComponent")
    class UriEncoding {

        @ParameterizedTest(name = "encodeURIComponent({0})")
        @CsvSource(delimiter = '|', value = {
            "a b            | a%20b",
            "filter[a]      | filter%5Ba%5D",
            "x,y            | x%2Cy",
            "c&d            | c%26d",
            "~!*()'         | ~!*()'",
            "a=b            | a%3Db",
            "1e+21          | 1e%2B21",
        })
        void matchesNode(String input, String expected) {
            assertEquals(expected, JsUri.encodeURIComponent(input.trim()));
        }

        @Test
        void utf8IsEncodedPerByte() {
            assertEquals("%C3%A9", JsUri.encodeURIComponent("é"));
            assertEquals("%C3%BC", JsUri.encodeURIComponent("ü"));
        }

        @Test
        @DisplayName("URLEncoder is not an equivalent")
        void urlEncoderDiverges() {
            assertEquals("a+b", URLEncoder.encode("a b", StandardCharsets.UTF_8));
            assertEquals("a%20b", JsUri.encodeURIComponent("a b"));
            assertNotEquals(URLEncoder.encode("~!*()'", StandardCharsets.UTF_8),
                    JsUri.encodeURIComponent("~!*()'"));
        }
    }
}
