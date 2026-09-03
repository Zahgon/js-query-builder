package io.github.coderello.querybuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Phase 11 — differential equivalence against the JavaScript implementation.
 *
 * <p>{@code harness/corpus.json} holds 615 builder programs and {@code harness/expected-js.json}
 * holds what the original {@code src/QueryBuilder.js} produced for each — either the built query
 * string or the fact that it threw. This test replays the identical corpus through the Java port
 * and requires identical results.
 *
 * <p>Both files are committed fixtures recorded from the JavaScript original, so {@code mvn test}
 * needs neither Node nor those sources. They must be re-derived from the original if behaviour
 * ever changes deliberately; editing them to match new Java output would defeat the check.
 */
class DifferentialTest {

    private static final Path HARNESS = Path.of("harness");
    private static final ObjectMapper JSON = new ObjectMapper();

    @AfterEach
    void resetGlobalState() {
        QueryBuilder.forgetCustomParameterNames();
    }

    @Test
    @DisplayName("every corpus program produces an identical result in Java and JavaScript")
    void javaMatchesJavaScript() throws IOException {
        Path corpusPath = HARNESS.resolve("corpus.json");
        Path expectedPath = HARNESS.resolve("expected-js.json");
        Assumptions.assumeTrue(Files.exists(corpusPath) && Files.exists(expectedPath),
                "harness/corpus.json and harness/expected-js.json are required fixtures");

        JsonNode corpus = JSON.readTree(Files.readString(corpusPath, StandardCharsets.UTF_8));
        JsonNode expected = JSON.readTree(Files.readString(expectedPath, StandardCharsets.UTF_8));
        assertEquals(corpus.size(), expected.size(), "corpus and baseline are out of sync");
        assertTrue(corpus.size() >= 500, "corpus unexpectedly small: " + corpus.size());

        List<String> diffs = new ArrayList<>();
        int built = 0;
        int threw = 0;

        for (int i = 0; i < corpus.size(); i++) {
            JsonNode testCase = corpus.get(i);
            JsonNode want = expected.get(i);
            String id = testCase.get("id").asText();

            String actual;
            try {
                applyCustomNames(testCase.get("customNames"));
                QueryBuilder builder = new QueryBuilder(testCase.get("baseUrl").asText());
                applyOps(builder, testCase.get("ops"));
                actual = "OK " + builder.build();
            } catch (RuntimeException e) {
                actual = "THREW";
            } finally {
                QueryBuilder.forgetCustomParameterNames();
            }

            String wantValue;
            if (want.get("ok").asBoolean()) {
                wantValue = "OK " + want.get("value").asText();
                built++;
            } else {
                wantValue = "THREW";
                threw++;
            }

            if (!wantValue.equals(actual)) {
                diffs.add("  [%s]%n      js   : %s%n      java : %s".formatted(id, wantValue, actual));
            }
        }

        assertTrue(built > 400 && threw > 40,
                "corpus must exercise both success and failure paths: %d built, %d threw"
                        .formatted(built, threw));
        assertTrue(diffs.isEmpty(),
                () -> "%d of %d differential cases diverged:%n%s"
                        .formatted(diffs.size(), corpus.size(), String.join("\n", diffs)));
    }

    @Test
    @DisplayName("the harness is non-vacuous — a deliberately wrong result is caught")
    void harnessDetectsDivergence() {
        assertNotEquals("/?a=b", new QueryBuilder("/").param("a", "c").build());
        assertNotEquals("/?page=0", new QueryBuilder("/").page(0).build());
    }

    // -----------------------------------------------------------------------

    private static void applyCustomNames(JsonNode names) {
        if (names == null || names.isNull()) {
            QueryBuilder.forgetCustomParameterNames();
            return;
        }
        Map<String, String> map = new LinkedHashMap<>();
        names.properties().forEach(e -> map.put(e.getKey(), e.getValue().asText()));
        QueryBuilder.defineCustomParameterNames(map);
    }

    private static void applyOps(QueryBuilder builder, JsonNode ops) {
        for (JsonNode o : ops) {
            String method = o.get("m").asText();
            switch (method) {
                case "tap" -> builder.tap(b -> applyOps(b, o.get("cb")));
                case "when" -> {
                    Object condition = decode(o.get("cond"));
                    if (o.has("cbRaw")) {
                        // A non-callable callback must still be rejected, so the raw value is
                        // passed through the Object-typed overload rather than a lambda.
                        Object raw = decode(o.get("cbRaw"));
                        builder.when(condition, asConsumerOrNull(raw));
                    } else {
                        builder.when(condition, b -> applyOps(b, o.get("cb")));
                    }
                }
                default -> invoke(builder, method, decodeArgs(o.get("a")));
            }
        }
    }

    /**
     * The corpus stores a deliberately invalid callback. Java cannot pass a {@code String} where
     * a {@code Consumer} is required, so it becomes {@code null} — which the port rejects at the
     * same point the source rejects a non-function.
     */
    private static Consumer<QueryBuilder> asConsumerOrNull(Object raw) {
        return raw instanceof Consumer<?> ? castConsumer(raw) : null;
    }

    @SuppressWarnings("unchecked")
    private static Consumer<QueryBuilder> castConsumer(Object raw) {
        return (Consumer<QueryBuilder>) raw;
    }

    private static void invoke(QueryBuilder b, String method, Object[] args) {
        switch (method) {
            case "baseUrl" -> b.baseUrl((String) args[0]);
            case "param" -> b.param(args);
            case "forgetParam" -> b.forgetParam(args);
            case "include" -> b.include(args);
            case "forgetInclude" -> b.forgetInclude(args);
            case "append" -> b.append(args);
            case "forgetAppend" -> b.forgetAppend(args);
            case "filter" -> b.filter(args);
            case "forgetFilter" -> b.forgetFilter(args);
            case "sort" -> b.sort(args);
            case "forgetSort" -> b.forgetSort(args);
            case "fields" -> b.fields(args);
            case "forgetFields" -> b.forgetFields(args);
            case "page" -> b.page(args[0]);
            case "forgetPage" -> b.forgetPage();
            case "tap" -> b.tap(asConsumerOrNull(args.length == 0 ? null : args[0]));
            default -> throw new IllegalArgumentException("corpus uses unknown method: " + method);
        }
    }

    private static Object[] decodeArgs(JsonNode args) {
        if (args == null || args.isNull()) {
            return new Object[0];
        }
        Object[] out = new Object[args.size()];
        for (int i = 0; i < args.size(); i++) {
            out[i] = decode(args.get(i));
        }
        return out;
    }

    /** Reverses the sentinels the harness uses for values JSON cannot carry. */
    private static Object decode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject() && node.has("__t")) {
            return switch (node.get("__t").asText()) {
                // JavaScript distinguishes undefined from null; both are rejected identically by
                // every guard in this library, so null models both.
                case "undefined" -> null;
                case "nan" -> Double.NaN;
                case "infinity" -> Double.POSITIVE_INFINITY;
                case "function" -> (Consumer<QueryBuilder>) b -> { };
                case "fn:true" -> (Supplier<Object>) () -> Boolean.TRUE;
                case "fn:false" -> (Supplier<Object>) () -> Boolean.FALSE;
                default -> throw new IllegalArgumentException("unknown sentinel: " + node);
            };
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isNumber()) {
            // JSON has no integer/double distinction that matches JavaScript, where every number
            // is a double. Integral values are handed over as Integer so they render without a
            // fractional part, exactly as String(5) does.
            double d = node.asDouble();
            return d == Math.rint(d) && !node.isFloatingPointNumber() ? (Object) node.asInt() : d;
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>(node.size());
            node.forEach(child -> list.add(decode(child)));
            return list;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        node.properties().forEach(e -> map.put(e.getKey(), decode(e.getValue())));
        return map;
    }
}
