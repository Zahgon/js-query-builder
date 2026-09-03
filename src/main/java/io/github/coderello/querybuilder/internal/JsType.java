package io.github.coderello.querybuilder.internal;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Port of {@code src/utils/gettype.js}.
 *
 * <pre>{@code
 * function gettype(value) {
 *     return {}.toString.call(value).match(/\s([a-zA-Z]+)/)[1].toLowerCase();
 * }
 * }</pre>
 *
 * <p>The JavaScript original reads the internal class tag out of
 * {@code Object.prototype.toString}, so it reports {@code "string"} for both a primitive and a
 * boxed {@code new String(...)}, and distinguishes {@code "array"}, {@code "null"},
 * {@code "undefined"} and {@code "function"} — none of which {@code instanceof} or
 * {@code getClass()} give you directly.
 *
 * <p>Tags observed by executing the source (Node): {@code string number boolean null undefined
 * array object function date regexp symbol map bigint}. {@code NaN}, {@code Infinity} and
 * {@code -0} all report {@code "number"}.
 *
 * <p>The Java value model is:
 * <ul>
 *   <li>JS string   &rarr; {@link String} (also {@link CharSequence})</li>
 *   <li>JS number   &rarr; {@link Number}</li>
 *   <li>JS boolean  &rarr; {@link Boolean}</li>
 *   <li>JS array    &rarr; {@link List} or a Java array</li>
 *   <li>JS object   &rarr; {@link Map}</li>
 *   <li>JS function &rarr; the functional interfaces the callbacks use</li>
 *   <li>JS null     &rarr; {@code null}</li>
 * </ul>
 */
public final class JsType {

    private JsType() {
    }

    public static final String STRING = "string";
    public static final String NUMBER = "number";
    public static final String BOOLEAN = "boolean";
    public static final String ARRAY = "array";
    public static final String OBJECT = "object";
    public static final String FUNCTION = "function";
    public static final String NULL = "null";

    /** The {@code gettype(value)} tag. */
    public static String of(Object value) {
        if (value == null) {
            // JavaScript distinguishes null from undefined, but the source only ever compares the
            // tag against "string"/"number"/"array"/"object"/"function", and both null and
            // undefined fall through every one of those to the same throw. Collapsing them is
            // therefore unobservable here.
            return NULL;
        }
        if (value instanceof CharSequence) {
            return STRING;
        }
        if (value instanceof Number) {
            return NUMBER;
        }
        if (value instanceof Boolean) {
            return BOOLEAN;
        }
        if (value instanceof List<?> || value.getClass().isArray()) {
            return ARRAY;
        }
        if (value instanceof Map<?, ?>) {
            return OBJECT;
        }
        if (isCallable(value)) {
            return FUNCTION;
        }
        // Every remaining Java object reports "object", exactly as an arbitrary class instance
        // does in JavaScript.
        return OBJECT;
    }

    /**
     * The callback shapes this library accepts where JavaScript would accept any function.
     *
     * <p>A bare {@link Runnable} is deliberately excluded: it cannot receive the builder, so
     * accepting it would let {@code tap(() -> {})} compile and then silently do nothing.
     */
    public static boolean isCallable(Object value) {
        return value instanceof Consumer<?>
                || value instanceof Supplier<?>
                || value instanceof BooleanSupplier
                || value instanceof Function<?, ?>;
    }

    /** {@code typeof value === 'function'} — used by {@code tap}, which bypasses {@code gettype}. */
    public static boolean isFunction(Object value) {
        return value != null && isCallable(value);
    }
}
