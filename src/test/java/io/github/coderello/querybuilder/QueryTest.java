package io.github.coderello.querybuilder;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 1:1 migration of {@code tests/query.test.js}. */
class QueryTest {

    @Test
    @DisplayName("should return QueryBuilder instance")
    void shouldReturnQueryBuilderInstance() {
        assertInstanceOf(QueryBuilder.class, Query.query());
    }
}
