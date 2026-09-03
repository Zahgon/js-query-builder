package io.github.coderello.querybuilder.internal;

import java.nio.charset.StandardCharsets;

/**
 * The JavaScript {@code encodeURIComponent}.
 *
 * <p>{@link java.net.URLEncoder} is not an equivalent — it implements
 * {@code application/x-www-form-urlencoded}, which differs on characters this library emits:
 *
 * <pre>
 * input        encodeURIComponent    URLEncoder.encode(s, UTF_8)
 * "a b"        "a%20b"               "a+b"       (wrong)
 * "~!*()'"     "~!*()'"              "%7E%21*%28%29%27"  (wrong)
 * "filter[a]"  "filter%5Ba%5D"       same
 * "x,y"        "x%2Cy"               same
 * </pre>
 *
 * <p>The unescaped treatment of {@code ~ ! * ' ( )} is directly observable: the source's own
 * behaviour for {@code param("~!*()'", "~!*()'")} is to emit those characters literally.
 */
public final class JsUri {

    private JsUri() {
    }

    /** Characters {@code encodeURIComponent} leaves alone: {@code A-Z a-z 0-9 - _ . ! ~ * ' ( )}. */
    private static final String UNRESERVED =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.!~*'()";

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    public static String encodeURIComponent(String input) {
        StringBuilder out = new StringBuilder(input.length() + 16);
        for (byte b : input.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if (c < 0x80 && UNRESERVED.indexOf(c) >= 0) {
                out.append(c);
            } else {
                out.append('%').append(HEX[(b >> 4) & 0xF]).append(HEX[b & 0xF]);
            }
        }
        return out.toString();
    }
}
