package io.github.coderello.querybuilder.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * JavaScript value semantics with no Java equivalent.
 *
 * <p>Two of them decide observable output in this library:
 * <ul>
 *   <li><b>Truthiness</b> — {@code build()} emits the page only under {@code if (this._page)}, so
 *       {@code page(0)}, {@code page("")} and {@code page(NaN)} are all silently dropped while
 *       {@code page("0")} is kept. A {@code != null} check gets every one of those wrong.</li>
 *   <li><b>Number to string</b> — values are interpolated into the query string, and
 *       {@code Double.toString} disagrees with JavaScript on exactly the shapes that appear
 *       ({@code 5} vs {@code 5.0}, {@code 1e+21} vs {@code 1.0E21}).</li>
 * </ul>
 */
public final class JsValues {

    private JsValues() {
    }

    /**
     * JavaScript truthiness.
     *
     * <p>The falsy set is exactly {@code false}, {@code 0}, {@code -0}, {@code ""}, {@code null},
     * {@code undefined}, {@code NaN}. Everything else — including {@code "0"}, {@code " "},
     * {@code []} and {@code {}} — is truthy.
     */
    public static boolean isTruthy(Object v) {
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof CharSequence s) {
            return !s.isEmpty();
        }
        if (v instanceof Number n) {
            double d = n.doubleValue();
            // Covers 0, -0 and NaN in one comparison.
            return d != 0.0 && !Double.isNaN(d);
        }
        return true;
    }

    /**
     * The JavaScript {@code String(v)} conversion, which is what template-literal interpolation
     * and {@code Array.prototype.join} perform.
     *
     * <pre>
     * String(5)        === "5"          String(1.5)   === "1.5"
     * String(1e21)     === "1e+21"      String(null)  === "null"
     * String([1,2])    === "1,2"        String([])    === ""
     * String({})       === "[object Object]"
     * </pre>
     */
    public static String toJsString(Object v) {
        if (v == null) {
            // String(null) is "null" in JavaScript, and String.valueOf agrees.
            return String.valueOf(v);
        }
        if (v instanceof CharSequence s) {
            return s.toString();
        }
        if (v instanceof Number n) {
            return numberToString(n);
        }
        if (v instanceof Boolean b) {
            return b ? "true" : "false";
        }
        if (v instanceof List<?> list) {
            return join(list, ",");
        }
        if (v.getClass().isArray()) {
            return join(JsObject.arrayToList(v), ",");
        }
        return OBJECT_TAG;
    }

    /**
     * {@code String({})} — the internal-class tag JavaScript produces for any plain object or
     * class instance without a custom {@code toString}.
     */
    private static final String OBJECT_TAG = "[object Object]";

    /**
     * {@code Array.prototype.join} — {@code null} and {@code undefined} elements render as the
     * empty string rather than as {@code "null"}.
     */
    public static String join(List<?> list, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(separator);
            }
            Object e = list.get(i);
            if (e != null) {
                sb.append(toJsString(e));
            }
        }
        return sb.toString();
    }

    /**
     * The ECMAScript {@code Number::toString} algorithm — the shortest decimal that round-trips.
     *
     * <p>Java's {@code Double.toString} is a different algorithm and diverges on the values a
     * query string actually carries: it renders {@code 5.0} where JavaScript gives {@code "5"},
     * and {@code 1.0E21} where JavaScript gives {@code "1e+21"}.
     *
     * <p>Integral {@link Integer}, {@link Long}, {@link Short}, {@link Byte} and
     * {@link java.math.BigInteger} inputs are rendered from their exact value rather than being
     * routed through a {@code double} first. Every number JavaScript can represent produces an
     * identical result either way; they differ only above 2^53, where JavaScript has already lost
     * the value — {@code String(9007199254740993)} is {@code "9007199254740992"} there, because
     * the literal cannot survive parsing. A Java caller passing such a {@code long} means the
     * exact value, so the exact value is emitted. Recorded as an accepted difference.
     *
     * <p>Deliberately the only public overload: an additional {@code numberToString(double)}
     * would silently win overload resolution for {@code long} arguments, because Java prefers
     * widening to boxing, and would reintroduce the precision loss described above.
     */
    public static String numberToString(Number n) {
        if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte
                || n instanceof java.math.BigInteger) {
            return n.toString();
        }
        return doubleToString(n.doubleValue());
    }

    private static String doubleToString(double d) {
        if (!Double.isFinite(d)) {
            // Java and JavaScript render all three non-finite values identically
            // ("NaN", "Infinity", "-Infinity"), so this is a delegation, not a coincidence
            // worth restating as literals.
            return Double.toString(d);
        }
        if (d == 0.0) {
            return "0"; // JavaScript renders -0 as "0"
        }
        if (d < 0) {
            return "-" + doubleToString(-d);
        }

        // Shortest round-tripping decimal expressed as digits `s` with exponent `n`, per
        // ECMA-262 Number::toString: value == s * 10^(n-k) where s has k digits.
        BigDecimal bd = new BigDecimal(shortestRepr(d));
        String digits = bd.unscaledValue().toString().replaceFirst("0+$", "");
        if (digits.isEmpty()) {
            digits = "0";
        }
        int k = digits.length();
        int n = bd.precision() - bd.scale();

        if (k <= n && n <= 21) {
            return digits + "0".repeat(n - k);
        }
        if (0 < n && n <= 21) {
            return digits.substring(0, n) + "." + digits.substring(n);
        }
        if (-6 < n && n <= 0) {
            return "0." + "0".repeat(-n) + digits;
        }
        String mantissa = k == 1 ? digits : digits.charAt(0) + "." + digits.substring(1);
        return mantissa + "e" + (n - 1 >= 0 ? "+" : "-") + Math.abs(n - 1);
    }

    private static String shortestRepr(double d) {
        for (int precision = 1; precision <= 17; precision++) {
            String candidate = String.format("%." + precision + "e", d);
            if (Double.parseDouble(candidate) == d) {
                return new BigDecimal(candidate).stripTrailingZeros().toPlainString();
            }
        }
        return new BigDecimal(d).toPlainString();
    }
}
