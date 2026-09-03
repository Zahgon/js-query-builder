package io.github.coderello.querybuilder.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code Object.entries} with JavaScript's own-property ordering.
 *
 * <p>JavaScript enumerates integer-like keys first, in ascending numeric order, and only then the
 * remaining string keys in insertion order. Verified against the source:
 *
 * <pre>
 * Object.entries({2:'a', 1:'b', b:'c', a:'d'})  ->  1, 2, b, a
 * </pre>
 *
 * <p>Neither Java map does this: {@code HashMap} is unordered and {@code LinkedHashMap} gives
 * pure insertion order ({@code 2, 1, b, a}). The builder sorts its parameters alphabetically
 * before emitting them, so this ordering is usually invisible — but it decides the relative
 * order of <em>duplicate</em> keys, which survives the sort because the sort is stable.
 */
public final class JsObject {

    private JsObject() {
    }

    /** One {@code [key, value]} pair from {@code Object.entries}. */
    public record Entry(String key, Object value) {
    }

    /** {@code Object.entries(obj)} in JavaScript enumeration order. */
    public static List<Entry> entries(Map<?, ?> map) {
        List<Entry> indexKeys = new ArrayList<>();
        List<Entry> stringKeys = new ArrayList<>();

        for (Map.Entry<?, ?> e : map.entrySet()) {
            String key = String.valueOf(e.getKey());
            if (isArrayIndex(key)) {
                indexKeys.add(new Entry(key, e.getValue()));
            } else {
                stringKeys.add(new Entry(key, e.getValue()));
            }
        }

        indexKeys.sort((a, b) -> Long.compare(Long.parseLong(a.key()), Long.parseLong(b.key())));

        List<Entry> out = new ArrayList<>(indexKeys.size() + stringKeys.size());
        out.addAll(indexKeys);
        out.addAll(stringKeys);
        return out;
    }

    /**
     * An "array index" in the ECMAScript sense: the canonical decimal form of a uint32 below
     * 2^32-1. {@code "01"} and {@code "1.0"} are ordinary string keys because they are not
     * canonical, and {@code "-1"} is not an index either.
     */
    static boolean isArrayIndex(String key) {
        if (key.isEmpty() || key.length() > 10) {
            return false;
        }
        for (int i = 0; i < key.length(); i++) {
            if (key.charAt(i) < '0' || key.charAt(i) > '9') {
                return false;
            }
        }
        if (key.length() > 1 && key.charAt(0) == '0') {
            return false; // not canonical
        }
        long value = Long.parseLong(key);
        return value < 4294967295L;
    }

    /** A live {@link List} view of any Java array, including primitive arrays. */
    public static List<Object> arrayToList(Object array) {
        int length = Array.getLength(array);
        List<Object> out = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            out.add(Array.get(array, i));
        }
        return out;
    }

    /** Coerces a JS-array-shaped value ({@link List} or Java array) to a {@link List}. */
    public static List<?> toList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        return arrayToList(value);
    }
}
