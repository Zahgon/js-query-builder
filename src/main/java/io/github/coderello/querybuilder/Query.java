package io.github.coderello.querybuilder;

/**
 * Port of {@code src/query.js} and the {@code src/index.js} entry point.
 *
 * <pre>{@code
 * export default function query(...args) {
 *     return new QueryBuilder(...args);
 * }
 * }</pre>
 *
 * <p>The source exports {@code query} and {@code QueryBuilder} from {@code src/index.js}. Java
 * has no module-level function, so the factory lives here as a static method; the two overloads
 * cover the source's zero-argument and one-argument forms.
 */
public final class Query {

    private Query() {
    }

    /** {@code query()} — a builder with an empty base URL. */
    public static QueryBuilder query() {
        return new QueryBuilder();
    }

    /** {@code query(baseUrl)}. */
    public static QueryBuilder query(String baseUrl) {
        return new QueryBuilder(baseUrl);
    }
}
