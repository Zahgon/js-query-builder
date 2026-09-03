package io.github.coderello.querybuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.coderello.querybuilder.internal.JsObject;
import io.github.coderello.querybuilder.internal.JsValues;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Covers the Java-only surface. Added by the migration.
 *
 * <p>The typed overloads and Java-array support have no counterpart in the source, so the
 * differential corpus cannot reach them: it drives the dynamic {@code Object...} entry points
 * only. These tests exist so the additions cannot rot, and assert that each overload produces
 * exactly what the dynamic call produces.
 */
class JavaApiTest {

    @AfterEach
    void resetGlobalState() {
        QueryBuilder.forgetCustomParameterNames();
    }

    private static QueryBuilder qb() {
        return new QueryBuilder("/");
    }

    @Nested
    @DisplayName("typed overloads agree with the dynamic call")
    class TypedOverloads {

        @Test
        void paramOverloads() {
            assertEquals(qb().param(new Object[] {"a", "b"}).build(), qb().param("a", "b").build());
            assertEquals(qb().param(new Object[] {"n", 5}).build(), qb().param("n", 5).build());
            assertEquals(qb().param(new Object[] {"l", List.of("x", "y")}).build(),
                    qb().param("l", List.of("x", "y")).build());
            assertEquals(qb().param(new Object[] {Map.of("a", "b")}).build(),
                    qb().param(Map.of("a", "b")).build());

            assertEquals("/?n=5", qb().param("n", 5).build());
            assertEquals("/?l=x%2Cy", qb().param("l", List.of("x", "y")).build());
        }

        @Test
        void filterOverloads() {
            assertEquals(qb().filter(new Object[] {"a", "b"}).build(),
                    qb().filter("a", "b").build());
            assertEquals(qb().filter(new Object[] {"n", 5}).build(), qb().filter("n", 5).build());
            assertEquals(qb().filter(new Object[] {"l", List.of("x", "y")}).build(),
                    qb().filter("l", List.of("x", "y")).build());
            assertEquals(qb().filter(new Object[] {Map.of("a", "b")}).build(),
                    qb().filter(Map.of("a", "b")).build());

            assertEquals("/?filter%5Bl%5D=x%2Cy", qb().filter("l", List.of("x", "y")).build());
        }

        @Test
        void fieldsOverloads() {
            assertEquals(qb().fields(new Object[] {"a", List.of("b")}).build(),
                    qb().fields("a", List.of("b")).build());
            assertEquals(qb().fields(new Object[] {Map.of("a", List.of("b"))}).build(),
                    qb().fields(Map.of("a", List.of("b"))).build());
        }

        @Test
        @DisplayName("the boolean when() overload matches the Object one")
        void whenOverloads() {
            assertEquals("/?page=2", qb().when(true, b -> b.page(2)).build());
            assertEquals("/?", qb().when(false, b -> b.page(2)).build());
            assertEquals(qb().when((Object) Boolean.TRUE, b -> b.page(2)).build(),
                    qb().when(true, b -> b.page(2)).build());
        }
    }

    @Nested
    @DisplayName("Java arrays are accepted wherever a JavaScript array is")
    class JavaArrays {

        @Test
        void arraysBehaveAsLists() {
            assertEquals("/?include=a%2Cb", qb().include((Object) new String[] {"a", "b"}).build());
            assertEquals("/?sort=a%2Cb", qb().sort((Object) new String[] {"a", "b"}).build());
            assertEquals("/?append=a%2Cb", qb().append((Object) new String[] {"a", "b"}).build());
            assertEquals("/?fields%5Bk%5D=a%2Cb",
                    qb().fields("k", List.of("a", "b")).build());
        }

        @Test
        void arraysWorkForForgetting() {
            assertEquals("/?include=a%2Cd",
                    qb().include("a", "b", "c", "d")
                            .forgetInclude((Object) new String[] {"b", "c"})
                            .build());
        }

        @Test
        void primitiveArraysCoerceElementwise() {
            assertEquals(List.of(1, 2, 3), JsObject.arrayToList(new int[] {1, 2, 3}));
            assertEquals("1,2,3", JsValues.toJsString(new int[] {1, 2, 3}));
            assertEquals(List.of("a"), JsObject.arrayToList(new String[] {"a"}));
        }

        @Test
        void toListPassesListsThrough() {
            List<String> list = List.of("a");
            assertSame(list, JsObject.toList(list));
            assertEquals(List.of("a"), JsObject.toList(new String[] {"a"}));
        }
    }

    @Nested
    @DisplayName("String() covers every branch")
    class Stringification {

        @Test
        void allValueShapes() {
            assertEquals("true", JsValues.toJsString(true));
            assertEquals("false", JsValues.toJsString(false));
            assertEquals("1,2", JsValues.toJsString(List.of(1, 2)));
            assertEquals("1,2", JsValues.toJsString(new int[] {1, 2}));
            assertEquals("[object Object]", JsValues.toJsString(Map.of("a", 1)));
            assertEquals("[object Object]", JsValues.toJsString(new Object()));
            assertEquals("null", JsValues.toJsString(null));
        }

        @Test
        @DisplayName("Number::toString covers each exponent branch")
        void numberBranches() {
            assertEquals("NaN", JsValues.numberToString(Double.NaN));
            assertEquals("Infinity", JsValues.numberToString(Double.POSITIVE_INFINITY));
            assertEquals("-Infinity", JsValues.numberToString(Double.NEGATIVE_INFINITY));
            assertEquals("0", JsValues.numberToString(0.0));
            assertEquals("0", JsValues.numberToString(-0.0));
            assertEquals("-1.5", JsValues.numberToString(-1.5));
            assertEquals("100", JsValues.numberToString(100.0), "k <= n <= 21");
            assertEquals("1.5", JsValues.numberToString(1.5), "0 < n <= 21");
            assertEquals("0.001", JsValues.numberToString(0.001), "-6 < n <= 0");
            assertEquals("1e-7", JsValues.numberToString(1e-7), "negative exponent form");
            assertEquals("1e+21", JsValues.numberToString(1e21), "positive exponent form");
            assertEquals("1.2345e+25", JsValues.numberToString(1.2345e25));
        }
    }

    @Nested
    @DisplayName("exception contract")
    class Exceptions {

        @Test
        @DisplayName("the message is empty, matching the source's bare new Error()")
        void emptyMessage() {
            QueryBuilderException e =
                    assertThrows(QueryBuilderException.class, () -> qb().page(null));
            assertEquals(null, e.getMessage());
        }

        @Test
        void everyValidationPathThrowsTheSameType() {
            assertThrows(QueryBuilderException.class, () -> qb().param("a"));
            assertThrows(QueryBuilderException.class, () -> qb().filter("a"));
            assertThrows(QueryBuilderException.class, () -> qb().fields("a"));
            assertThrows(QueryBuilderException.class, () -> qb().sort((Object) null));
            assertThrows(QueryBuilderException.class, () -> qb().include((Object) null));
            assertThrows(QueryBuilderException.class, () -> qb().append((Object) null));
            assertThrows(QueryBuilderException.class, () -> qb().page(null));
            assertThrows(QueryBuilderException.class, () -> qb().tap(null));
            assertThrows(QueryBuilderException.class, () -> qb().when(true, null));
            assertThrows(QueryBuilderException.class, () -> qb().forgetParam(5));
            assertThrows(QueryBuilderException.class, () -> qb().forgetSort(5));
        }

        @Test
        @DisplayName("a non-Map object cannot stand in for a JavaScript object literal")
        void nonMapObjectRejected() {
            assertThrows(QueryBuilderException.class, () -> qb().param(new Object[] {new Object()}));
        }
    }

    @Nested
    @DisplayName("custom parameter names")
    class CustomNames {

        @Test
        @DisplayName("null clears them, matching the source's falsy check")
        void nullBehavesAsUnset() {
            QueryBuilder.defineCustomParameterNames(null);
            assertEquals("page", QueryBuilder.getParameterName("page"));
            assertEquals("/?page=1", qb().page(1).build());
        }

        @Test
        @DisplayName("the snapshot is immutable, so a later mutation cannot leak in")
        void snapshotIsDefensive() {
            java.util.Map<String, String> names = new java.util.HashMap<>();
            names.put("page", "P");
            QueryBuilder.defineCustomParameterNames(names);

            names.put("sort", "S");

            assertEquals("P", QueryBuilder.getParameterName("page"));
            assertEquals("sort", QueryBuilder.getParameterName("sort"),
                    "mutating the caller's map after the call must not affect the builder");
        }

        @Test
        void forgettingIsIdempotent() {
            QueryBuilder.forgetCustomParameterNames();
            QueryBuilder.forgetCustomParameterNames();
            assertEquals("page", QueryBuilder.getParameterName("page"));
        }
    }

    @Test
    @DisplayName("every mutator returns the same instance so chains are safe")
    void chainingReturnsSameInstance() {
        QueryBuilder b = qb();
        assertSame(b, b.baseUrl("/x"));
        assertSame(b, b.param("a", "b"));
        assertSame(b, b.filter("a", "b"));
        assertSame(b, b.fields("a", List.of("b")));
        assertSame(b, b.sort("a"));
        assertSame(b, b.include("a"));
        assertSame(b, b.append("a"));
        assertSame(b, b.page(1));
        assertSame(b, b.forgetPage());
        assertSame(b, b.tap(x -> { }));
        assertSame(b, b.when(false, x -> { }));
        assertSame(b, b.forgetParam());
        assertSame(b, b.forgetFilter());
        assertSame(b, b.forgetFields());
        assertSame(b, b.forgetSort());
        assertSame(b, b.forgetInclude());
        assertSame(b, b.forgetAppend());
    }
}
