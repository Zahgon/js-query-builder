package io.github.coderello.querybuilder;

/**
 * Thrown wherever the source throws {@code new Error()}.
 *
 * <p>The JavaScript original raises a bare {@code Error} with an <b>empty message</b> at every
 * validation failure — there is no text to carry over, and the source's own tests only assert
 * {@code toThrowError()} without inspecting a message.
 *
 * <p>A dedicated unchecked type is used rather than {@link IllegalArgumentException} so callers
 * can distinguish a builder rejection from an argument error raised by the JDK itself.
 */
public class QueryBuilderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Mirrors {@code new Error()} — no message, matching the source exactly. */
    public QueryBuilderException() {
        super();
    }
}
