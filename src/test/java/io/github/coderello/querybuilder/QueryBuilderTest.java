package io.github.coderello.querybuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 1:1 migration of {@code tests/QueryBuilder.test.js}.
 *
 * <p>Thirty-seven tests, same order, same names, same assertions. Together with
 * {@link GettypeTest} and {@link QueryTest} this file must total exactly 39 so the suite can be
 * compared against the JavaScript baseline of 39 pass / 0 fail. Extra coverage belongs in the
 * sibling classes, not here.
 */
class QueryBuilderTest {

    private static QueryBuilder qb() {
        return new QueryBuilder("/");
    }

    /** A JavaScript object literal. */
    private static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @BeforeEach
    void beforeEach() {
        QueryBuilder.forgetCustomParameterNames();
    }

    @Test
    @DisplayName("should set base url")
    void shouldSetBaseUrl() {
        assertEquals("/b?", qb().baseUrl("/b").build());
    }

    @Test
    @DisplayName("should apply custom parameter names")
    void shouldApplyCustomParameterNames() {
        QueryBuilder.defineCustomParameterNames(Map.of(
                "filter", "FILTER",
                "sort", "SORT",
                "include", "INCLUDE",
                "fields", "FIELDS",
                "page", "PAGE"));

        assertEquals(
                "/?FIELDS%5Bh%5D=i%2Cv&FILTER%5Ba%5D=b&INCLUDE=f%2Ca&PAGE=3&SORT=c%2Cd%2Ce",
                qb().filter("a", "b")
                        .sort("c", "d", "e")
                        .include("f", "a")
                        .fields("h", List.of("i", "v"))
                        .page(3)
                        .build());
    }

    @Test
    @DisplayName("should forget custom parameter names")
    void shouldForgetCustomParameterNames() {
        QueryBuilder.defineCustomParameterNames(Map.of("page", "PAGE"));
        QueryBuilder.forgetCustomParameterNames();

        assertEquals("/?page=5", qb().page(5).build());
    }

    @Test
    @DisplayName("should return parameter name")
    void shouldReturnParameterName() {
        QueryBuilder.defineCustomParameterNames(Map.of("page", "PAGE"));

        assertEquals("PAGE", QueryBuilder.getParameterName("page"));
        assertEquals("filter", QueryBuilder.getParameterName("filter"));
    }

    @Test
    @DisplayName("should add params")
    void shouldAddParams() {
        assertEquals("/?a=b&c=d", qb().param("a", "b").param("c", "d").build());
        assertEquals("/?a=b&c=d", qb().param(obj("a", "b", "c", "d")).build());
    }

    @Test
    @DisplayName("should not add params with invalid arguments")
    void shouldNotAddParamsWithInvalidArguments() {
        assertThrows(QueryBuilderException.class, () -> qb().param("a").build());
        assertThrows(QueryBuilderException.class, () -> qb().param("a", Map.of()).build());
        assertThrows(QueryBuilderException.class, () -> qb().param(obj("b", null)).build());
        assertThrows(QueryBuilderException.class, () -> qb().param("a", "b", "c").build());
    }

    @Test
    @DisplayName("should forget params")
    void shouldForgetParams() {
        assertEquals("/?c=d",
                qb().param(obj("a", "b", "c", "d", "e", "f"))
                        .forgetParam(List.of("a", "e"))
                        .build());

        assertEquals("/?a=b",
                qb().param(obj("a", "b", "c", "d", "e", "f"))
                        .forgetParam("c", "e")
                        .build());

        assertEquals("/?", qb().param(obj("a", "b", "c", "d")).forgetParam().build());
    }

    @Test
    @DisplayName("should not forget params with invalid arguments")
    void shouldNotForgetParamsWithInvalidArguments() {
        assertThrows(QueryBuilderException.class, () -> qb().forgetParam("a", Map.of()).build());
        assertThrows(QueryBuilderException.class,
                () -> qb().forgetParam(Arrays.asList("b", null)).build());
    }

    @Test
    @DisplayName("should add includes")
    void shouldAddIncludes() {
        assertEquals("/?include=a%2Cb", qb().include("a", "b").build());
        assertEquals("/?include=c%2Cd", qb().include(List.of("c", "d")).build());
    }

    @Test
    @DisplayName("should not add includes with invalid arguments")
    void shouldNotAddIncludesWithInvalidArguments() {
        assertThrows(QueryBuilderException.class, () -> qb().include("a", Map.of()).build());
        assertThrows(QueryBuilderException.class,
                () -> qb().include(Arrays.asList("b", null)).build());
    }

    @Test
    @DisplayName("should forget includes")
    void shouldForgetIncludes() {
        assertEquals("/?include=a%2Cb",
                qb().include("a", "d", "f", "b").forgetInclude(List.of("d", "f")).build());

        assertEquals("/?include=d%2Cb",
                qb().include(List.of("a", "d", "f", "b")).forgetInclude("a", "f").build());

        assertEquals("/?", qb().include(List.of("a", "d", "f", "b")).forgetInclude().build());
    }

    @Test
    @DisplayName("should not forget includes with invalid arguments")
    void shouldNotForgetIncludesWithInvalidArguments() {
        assertThrows(QueryBuilderException.class,
                () -> qb().forgetInclude((Object) null).build());
        assertThrows(QueryBuilderException.class, () -> qb().forgetInclude(Map.of()).build());
        assertThrows(QueryBuilderException.class,
                () -> qb().forgetInclude(Arrays.asList(2, null)).build());
    }

    @Test
    @DisplayName("should add appends")
    void shouldAddAppends() {
        assertEquals("/?append=a%2Cb", qb().append("a", "b").build());
        assertEquals("/?append=c%2Cd", qb().append(List.of("c", "d")).build());
    }

    @Test
    @DisplayName("should not add appends with invalid arguments")
    void shouldNotAddAppendsWithInvalidArguments() {
        assertThrows(QueryBuilderException.class, () -> qb().append("a", Map.of()).build());
        assertThrows(QueryBuilderException.class,
                () -> qb().append(Arrays.asList("b", null)).build());
    }

    @Test
    @DisplayName("should forget appends")
    void shouldForgetAppends() {
        assertEquals("/?append=a%2Cb",
                qb().append("a", "d", "f", "b").forgetAppend(List.of("d", "f")).build());

        assertEquals("/?append=d%2Cb",
                qb().append(List.of("a", "d", "f", "b")).forgetAppend("a", "f").build());

        assertEquals("/?", qb().append(List.of("a", "d", "f", "b")).forgetAppend().build());
    }

    @Test
    @DisplayName("should not forget appends with invalid arguments")
    void shouldNotForgetAppendsWithInvalidArguments() {
        assertThrows(QueryBuilderException.class, () -> qb().forgetAppend((Object) null).build());
        assertThrows(QueryBuilderException.class, () -> qb().forgetAppend(Map.of()).build());
        assertThrows(QueryBuilderException.class,
                () -> qb().forgetAppend(Arrays.asList(2, null)).build());
    }

    @Test
    @DisplayName("should apply filters")
    void shouldApplyFilters() {
        assertEquals("/?filter%5Ba%5D=b", qb().filter("a", "b").build());

        assertEquals("/?filter%5Ba%5D=b&filter%5Bc%5D=d",
                qb().filter(obj("a", "b", "c", "d")).build());

        assertEquals("/?filter%5Ba%5D=b&filter%5Bc%5D=f%2Cd",
                qb().filter(obj("a", "b", "c", List.of("f", "d"))).build());
    }

    @Test
    @DisplayName("should not apply filters with invalid arguments")
    void shouldNotApplyFiltersWithInvalidArguments() {
        assertThrows(QueryBuilderException.class, () -> qb().filter("a").build());
        assertThrows(QueryBuilderException.class, () -> qb().filter("a", (Object) null).build());
        assertThrows(QueryBuilderException.class,
                () -> qb().filter(obj("a", Map.of(), "b", "c")).build());
        assertThrows(QueryBuilderException.class, () -> qb().filter("a", "b", "c").build());
    }

    @Test
    @DisplayName("should forget filters")
    void shouldForgetFilters() {
        assertEquals("/?filter%5Bc%5D=d",
                qb().filter(obj("a", "b", "c", "d", "e", "f"))
                        .forgetFilter(List.of("a", "e"))
                        .build());

        assertEquals("/?filter%5Bc%5D=d",
                qb().filter(obj("a", "b", "c", "d", "e", "f"))
                        .forgetFilter("a", "e")
                        .build());

        assertEquals("/?",
                qb().filter(obj("a", "b", "c", "d", "e", "f")).forgetFilter().build());
    }

    @Test
    @DisplayName("should not forget filters with invalid arguments")
    void shouldNotForgetFiltersWithInvalidArguments() {
        assertThrows(QueryBuilderException.class,
                () -> qb().forgetFilter(obj("a", "b")).build());
        assertThrows(QueryBuilderException.class,
                () -> qb().forgetFilter(Arrays.asList(3, Map.of())).build());
    }

    @Test
    @DisplayName("should apply sorts")
    void shouldApplySorts() {
        assertEquals("/?sort=a%2Cb", qb().sort("a", "b").build());
        assertEquals("/?sort=a%2Cb", qb().sort(List.of("a", "b")).build());
    }

    @Test
    @DisplayName("should not apply sorts with invalid arguments")
    void shouldNotApplySortsWithInvalidArguments() {
        assertThrows(QueryBuilderException.class, () -> qb().sort((Object) null).build());
        assertThrows(QueryBuilderException.class,
                () -> qb().sort(Arrays.asList("a", Map.of())).build());
    }

    @Test
    @DisplayName("should forget sorts")
    void shouldForgetSorts() {
        assertEquals("/?sort=a%2Cd",
                qb().sort("a", "b", "c", "d").forgetSort(List.of("b", "c")).build());

        assertEquals("/?sort=c%2Cd",
                qb().sort(List.of("a", "b", "c", "d")).forgetSort("a", "b").build());

        assertEquals("/?", qb().sort(List.of("a", "b", "c", "d")).forgetSort().build());
    }

    @Test
    @DisplayName("should not forget sorts with invalid arguments")
    void shouldNotForgetSortsWithInvalidArguments() {
        assertThrows(QueryBuilderException.class, () -> qb().forgetSort((Object) null).build());
        assertThrows(QueryBuilderException.class, () -> qb().forgetSort(Map.of()).build());
        assertThrows(QueryBuilderException.class,
                () -> qb().forgetSort(Arrays.asList(2, null)).build());
    }

    @Test
    @DisplayName("should add fields")
    void shouldAddFields() {
        assertEquals("/?fields%5Ba%5D=b%2Cc%2Cd",
                qb().fields("a", List.of("b", "c", "d")).build());

        assertEquals("/?fields%5Ba%5D=b%2Cc&fields%5Bd%5D=e%2Cf",
                qb().fields(obj("a", List.of("b", "c"), "d", List.of("e", "f"))).build());
    }

    @Test
    @DisplayName("should not add fields with invalid arguments")
    void shouldNotAddFieldsWithInvalidArguments() {
        assertThrows(QueryBuilderException.class, () -> qb().fields("a").build());
        assertThrows(QueryBuilderException.class, () -> qb().fields("a", "b").build());
        assertThrows(QueryBuilderException.class, () -> qb().fields(obj("a", null)).build());
        assertThrows(QueryBuilderException.class, () -> qb().fields("a", "b", "c").build());
    }

    @Test
    @DisplayName("should forget fields")
    void shouldForgetFields() {
        assertEquals("/?fields%5Ba%5D=b%2Cc",
                qb().fields("a", List.of("b", "c"))
                        .fields("d", List.of("e", "f"))
                        .forgetFields("d")
                        .build());

        assertEquals("/?fields%5Bd%5D=e%2Cf",
                qb().fields(obj("a", List.of("b", "c"), "d", List.of("e", "f")))
                        .forgetFields(List.of("a"))
                        .build());

        assertEquals("/?",
                qb().fields(obj("a", List.of("b", "c"), "d", List.of("e", "f")))
                        .forgetFields()
                        .build());
    }

    @Test
    @DisplayName("should not forget fields with invalid arguments")
    void shouldNotForgetFieldsWithInvalidArguments() {
        assertThrows(QueryBuilderException.class, () -> qb().forgetFields(Map.of()).build());
        assertThrows(QueryBuilderException.class,
                () -> qb().forgetFields(Arrays.asList((Object) null)).build());
    }

    @Test
    @DisplayName("should set page")
    void shouldSetPage() {
        assertEquals("/?page=3", qb().page(3).build());
        assertEquals("/?page=5", qb().page("5").build());
    }

    @Test
    @DisplayName("should not set page with invalid arguments")
    void shouldNotSetPageWithInvalidArguments() {
        assertThrows(QueryBuilderException.class, () -> qb().page(List.of()).build());
        assertThrows(QueryBuilderException.class, () -> qb().page(null).build());
    }

    @Test
    @DisplayName("should forget page")
    void shouldForgetPage() {
        assertEquals("/?", qb().page(3).forgetPage().build());
    }

    @Test
    @DisplayName("should tap builder")
    void shouldTapBuilder() {
        assertEquals("/?a=b", qb().tap(b -> b.param("a", "b")).build());
    }

    @Test
    @DisplayName("should not tap builder with invalid arguments")
    void shouldNotTapBuilderWithInvalidArguments() {
        assertThrows(QueryBuilderException.class, () -> qb().tap(null).build());
    }

    @Test
    @DisplayName("should conditionally tap builder using when")
    void shouldConditionallyTapBuilderUsingWhen() {
        assertEquals("/?page=2", qb().when(true, b -> b.page(2)).build());
        assertEquals("/?", qb().when(false, b -> b.page(2)).build());

        assertEquals("/?page=2", qb().when((java.util.function.Supplier<Object>) () -> true,
                b -> b.page(2)).build());
        assertEquals("/?", qb().when((java.util.function.Supplier<Object>) () -> false,
                b -> b.page(2)).build());

        String name = "";

        assertEquals("/?", qb().when(name, b -> b.filter(obj("name", name))).build());
    }

    @Test
    @DisplayName("should not conditionally tap builder using when with invalid arguments")
    void shouldNotConditionallyTapBuilderUsingWhenWithInvalidArguments() {
        assertThrows(QueryBuilderException.class, () -> qb().when(true, null).build());
    }

    @Test
    @DisplayName("should build query string correctly")
    void shouldBuildQueryStringCorrectly() {
        assertEquals(
                "/?a=b&append=b%2Cd&fields%5Bt%5D=g%2Ca&filter%5Ba%5D=b&filter%5Bf%5D=c"
                        + "&include=y%2Cs&page=3&sort=c%2Cd%2Ce",
                qb().filter("a", "b")
                        .filter(obj("d", "e", "f", "c"))
                        .sort("c")
                        .when(true, b -> b.forgetFilter("d"))
                        .sort(List.of("d", "e", "a"))
                        .tap(b -> b.forgetSort(List.of("a")))
                        .include("y", "e", "s")
                        .forgetInclude(List.of("e"))
                        .fields(obj("h", List.of("d"), "t", List.of("g", "a")))
                        .forgetFields("h")
                        .append("b", "d")
                        .page(3)
                        .param(obj("a", "b", "u", "a", "n", "p"))
                        .forgetParam("u", "n")
                        .build());
    }

    @Test
    @DisplayName("should alphabetically sort params in built string")
    void shouldAlphabeticallySortParamsInBuiltString() {
        assertEquals("/?a=d&fields%5Ba%5D=h&fields%5Bb%5D=a&fields%5Bc%5D=d&z=d",
                qb().fields("b", List.of("a"))
                        .fields("c", List.of("d"))
                        .param("a", "d")
                        .fields("a", List.of("h"))
                        .param("z", "d")
                        .build());
    }
}
