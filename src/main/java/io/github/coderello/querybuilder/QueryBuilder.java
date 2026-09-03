package io.github.coderello.querybuilder;

import io.github.coderello.querybuilder.internal.JsObject;
import io.github.coderello.querybuilder.internal.JsType;
import io.github.coderello.querybuilder.internal.JsUri;
import io.github.coderello.querybuilder.internal.JsValues;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Port of {@code src/QueryBuilder.js} — builds a query string compatible with
 * {@code spatie/laravel-query-builder}.
 *
 * <p>Every method returns {@code this}, so calls chain exactly as in the source.
 *
 * <p>The variadic methods keep the source's dynamic contract: they accept strings, numbers,
 * arrays and objects, recurse into nested arrays, and throw {@link QueryBuilderException} on
 * anything else. Typed convenience overloads are provided alongside and delegate to the same
 * code, so they cannot drift from it.
 *
 * <p><b>Not thread-safe</b> at the instance level, matching the source. The static custom
 * parameter names are shared process-wide (see {@link #defineCustomParameterNames}).
 */
public class QueryBuilder {

    private static final List<String> SCALAR_OR_ARRAY =
            List.of(JsType.STRING, JsType.NUMBER, JsType.ARRAY);

    /**
     * {@code QueryBuilder._customParameterNames} — static, and therefore shared by every
     * instance in the process, exactly as in the source.
     *
     * <p>Held in a {@code volatile} field containing an immutable snapshot. JavaScript is
     * single-threaded so the source needs no such care; without it, a Java caller reconfiguring
     * names while another thread builds would see a torn map. The observable single-threaded
     * behaviour is unchanged.
     */
    private static volatile Map<String, String> customParameterNames;

    private String baseUrl;
    private Map<String, Object> filters = new LinkedHashMap<>();
    private List<String> sorts = new ArrayList<>();
    private List<String> includes = new ArrayList<>();
    private List<String> appends = new ArrayList<>();
    private Map<String, Object> fields = new LinkedHashMap<>();
    private Object page;
    private Map<String, Object> params = new LinkedHashMap<>();

    /** {@code constructor(baseUrl = '')}. */
    public QueryBuilder() {
        this("");
    }

    public QueryBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // -----------------------------------------------------------------------
    // Custom parameter names (static, process-wide)
    // -----------------------------------------------------------------------

    /**
     * {@code static defineCustomParameterNames(customParameterNames)}.
     *
     * <p>Sets process-wide state: builders created before this call are affected too, because the
     * names are resolved at {@link #build()} time. That leakage is the source's behaviour and is
     * relied on by its tests, which reset it in {@code beforeEach}.
     *
     * <p>A {@code null} argument is accepted and behaves as the source does — the falsy check in
     * {@code getParameterName} then makes every lookup fall through to the default name.
     */
    public static void defineCustomParameterNames(Map<String, String> customParameterNames) {
        QueryBuilder.customParameterNames =
                customParameterNames == null ? null : Map.copyOf(customParameterNames);
    }

    /** {@code static forgetCustomParameterNames()} — idempotent, as {@code delete} is. */
    public static void forgetCustomParameterNames() {
        QueryBuilder.customParameterNames = null;
    }

    /** {@code static getParameterName(parameter)}. */
    public static String getParameterName(String parameter) {
        Map<String, String> names = customParameterNames;
        // `this._customParameterNames && this._customParameterNames.hasOwnProperty(parameter)`
        // — a truthiness check, so a null (or, in JS, any falsy) mapping yields the default.
        return names != null && names.containsKey(parameter) ? names.get(parameter) : parameter;
    }

    // -----------------------------------------------------------------------
    // Base URL
    // -----------------------------------------------------------------------

    public QueryBuilder baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    // -----------------------------------------------------------------------
    // Params
    // -----------------------------------------------------------------------

    /** {@code param(...args)} — either one object, or a key and a string/number/array value. */
    public QueryBuilder param(Object... args) {
        return keyedSetter(args, params, SCALAR_OR_ARRAY, this::param);
    }

    public QueryBuilder param(String key, String value) {
        return param(new Object[] {key, value});
    }

    public QueryBuilder param(String key, Number value) {
        return param(new Object[] {key, value});
    }

    public QueryBuilder param(String key, List<?> value) {
        return param(new Object[] {key, value});
    }

    public QueryBuilder param(Map<String, ?> values) {
        return param(new Object[] {values});
    }

    /** {@code forgetParam(...args)} — with no arguments, clears everything. */
    public QueryBuilder forgetParam(Object... args) {
        return forgetKeyed(args, () -> params = new LinkedHashMap<>(), params::remove,
                this::forgetParam);
    }

    // -----------------------------------------------------------------------
    // Includes
    // -----------------------------------------------------------------------

    public QueryBuilder include(Object... args) {
        return listAppender(args, includes, this::include);
    }

    public QueryBuilder forgetInclude(Object... args) {
        return forgetFromList(args, () -> includes = new ArrayList<>(), includes,
                this::forgetInclude);
    }

    // -----------------------------------------------------------------------
    // Appends
    // -----------------------------------------------------------------------

    public QueryBuilder append(Object... args) {
        return listAppender(args, appends, this::append);
    }

    public QueryBuilder forgetAppend(Object... args) {
        return forgetFromList(args, () -> appends = new ArrayList<>(), appends, this::forgetAppend);
    }

    // -----------------------------------------------------------------------
    // Filters
    // -----------------------------------------------------------------------

    public QueryBuilder filter(Object... args) {
        return keyedSetter(args, filters, SCALAR_OR_ARRAY, this::filter);
    }

    public QueryBuilder filter(String key, String value) {
        return filter(new Object[] {key, value});
    }

    public QueryBuilder filter(String key, Number value) {
        return filter(new Object[] {key, value});
    }

    public QueryBuilder filter(String key, List<?> value) {
        return filter(new Object[] {key, value});
    }

    public QueryBuilder filter(Map<String, ?> values) {
        return filter(new Object[] {values});
    }

    public QueryBuilder forgetFilter(Object... args) {
        return forgetKeyed(args, () -> filters = new LinkedHashMap<>(), filters::remove,
                this::forgetFilter);
    }

    // -----------------------------------------------------------------------
    // Sorts
    // -----------------------------------------------------------------------

    public QueryBuilder sort(Object... args) {
        return listAppender(args, sorts, this::sort);
    }

    public QueryBuilder forgetSort(Object... args) {
        return forgetFromList(args, () -> sorts = new ArrayList<>(), sorts, this::forgetSort);
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /** {@code fields(...args)} — unlike {@code filter}, the value must be an array. */
    public QueryBuilder fields(Object... args) {
        return keyedSetter(args, fields, List.of(JsType.ARRAY), this::fields);
    }

    public QueryBuilder fields(String key, List<?> value) {
        return fields(new Object[] {key, value});
    }

    public QueryBuilder fields(Map<String, ?> values) {
        return fields(new Object[] {values});
    }

    public QueryBuilder forgetFields(Object... args) {
        return forgetKeyed(args, () -> fields = new LinkedHashMap<>(), fields::remove,
                this::forgetFields);
    }

    // -----------------------------------------------------------------------
    // Page
    // -----------------------------------------------------------------------

    /** {@code page(page)} — accepts a number or a string; anything else throws. */
    public QueryBuilder page(Object page) {
        String type = JsType.of(page);
        if (!JsType.NUMBER.equals(type) && !JsType.STRING.equals(type)) {
            throw new QueryBuilderException();
        }
        this.page = page;
        return this;
    }

    public QueryBuilder forgetPage() {
        this.page = null;
        return this;
    }

    // -----------------------------------------------------------------------
    // Control flow
    // -----------------------------------------------------------------------

    /** {@code tap(callback)} — always invoked; the return value is discarded. */
    public QueryBuilder tap(Consumer<QueryBuilder> callback) {
        if (!JsType.isFunction(callback)) {
            throw new QueryBuilderException();
        }
        callback.accept(this);
        return this;
    }

    /**
     * {@code when(condition, callback)}.
     *
     * <p>The condition is evaluated for <em>truthiness</em>, not equality with {@code true}: an
     * empty string skips the callback while {@code []} runs it. A {@link Supplier} condition is
     * invoked first, mirroring the source's {@code condition()} branch.
     */
    public QueryBuilder when(Object condition, Consumer<QueryBuilder> callback) {
        if (!JsType.isFunction(callback)) {
            throw new QueryBuilderException();
        }
        Object resolved = condition instanceof Supplier<?> supplier ? supplier.get() : condition;
        if (JsValues.isTruthy(resolved)) {
            callback.accept(this);
        }
        return this;
    }

    public QueryBuilder when(boolean condition, Consumer<QueryBuilder> callback) {
        return when((Object) condition, callback);
    }

    // -----------------------------------------------------------------------
    // Build
    // -----------------------------------------------------------------------

    /**
     * {@code build()} — assembles the query string.
     *
     * <p>Two details are load-bearing:
     * <ul>
     *   <li>The page is emitted under {@code if (this._page)}, a truthiness test, so
     *       {@code page(0)} and {@code page("")} produce no parameter at all.</li>
     *   <li>Parameters are sorted by key with {@code (a, b) => a[0] < b[0] ? -1 : 1}. That
     *       comparator never returns 0, yet V8 was observed to keep equal keys in insertion order
     *       even across 25 duplicates, so a <em>stable</em> sort on the key reproduces it.</li>
     * </ul>
     */
    public String build() {
        List<String[]> parameters = new ArrayList<>();

        for (JsObject.Entry entry : JsObject.entries(filters)) {
            parameters.add(new String[] {
                    getParameterName("filter") + "[" + entry.key() + "]",
                    JsValues.toJsString(entry.value())});
        }

        if (!sorts.isEmpty()) {
            parameters.add(new String[] {getParameterName("sort"), String.join(",", sorts)});
        }
        if (!includes.isEmpty()) {
            parameters.add(new String[] {getParameterName("include"), String.join(",", includes)});
        }
        if (!appends.isEmpty()) {
            parameters.add(new String[] {getParameterName("append"), String.join(",", appends)});
        }

        for (JsObject.Entry entry : JsObject.entries(fields)) {
            parameters.add(new String[] {
                    getParameterName("fields") + "[" + entry.key() + "]",
                    JsValues.join(JsObject.toList(entry.value()), ",")});
        }

        if (JsValues.isTruthy(page)) {
            parameters.add(new String[] {getParameterName("page"), JsValues.toJsString(page)});
        }

        for (JsObject.Entry entry : JsObject.entries(params)) {
            parameters.add(new String[] {entry.key(), JsValues.toJsString(entry.value())});
        }

        parameters.sort(Comparator.comparing(entry -> entry[0]));

        StringBuilder query = new StringBuilder();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                query.append('&');
            }
            query.append(JsUri.encodeURIComponent(parameters.get(i)[0]))
                    .append('=')
                    .append(JsUri.encodeURIComponent(parameters.get(i)[1]));
        }

        return baseUrl + "?" + query;
    }

    // -----------------------------------------------------------------------
    // Shared shapes
    //
    // param/filter/fields and their forget* counterparts are byte-for-byte identical in the
    // source apart from the target collection and the accepted value types. Factoring them here
    // keeps the validation rules in one place rather than duplicated six times.
    // -----------------------------------------------------------------------

    private interface Recurse {
        QueryBuilder apply(Object... args);
    }

    /** The {@code param} / {@code filter} / {@code fields} shape. */
    private QueryBuilder keyedSetter(Object[] args, Map<String, Object> target,
            List<String> allowedValueTypes, Recurse self) {
        switch (args.length) {
            case 1 -> {
                if (!JsType.OBJECT.equals(JsType.of(args[0]))) {
                    throw new QueryBuilderException();
                }
                for (JsObject.Entry entry : JsObject.entries(asObject(args[0]))) {
                    self.apply(entry.key(), entry.value());
                }
            }
            case 2 -> {
                if (!JsType.STRING.equals(JsType.of(args[0]))
                        || !allowedValueTypes.contains(JsType.of(args[1]))) {
                    throw new QueryBuilderException();
                }
                target.put(args[0].toString(), args[1]);
            }
            default -> throw new QueryBuilderException();
        }
        return this;
    }

    /** The {@code forgetParam} / {@code forgetFilter} / {@code forgetFields} shape. */
    private QueryBuilder forgetKeyed(Object[] args, Runnable clearAll,
            Consumer<String> removeOne, Recurse self) {
        if (args.length == 0) {
            clearAll.run();
            return this;
        }
        for (Object arg : args) {
            switch (JsType.of(arg)) {
                case JsType.ARRAY -> self.apply(JsObject.toList(arg).toArray());
                case JsType.STRING -> removeOne.accept(arg.toString());
                default -> throw new QueryBuilderException();
            }
        }
        return this;
    }

    /** The {@code include} / {@code append} / {@code sort} shape. */
    private QueryBuilder listAppender(Object[] args, List<String> target, Recurse self) {
        for (Object arg : args) {
            switch (JsType.of(arg)) {
                case JsType.ARRAY -> self.apply(JsObject.toList(arg).toArray());
                case JsType.STRING -> target.add(arg.toString());
                default -> throw new QueryBuilderException();
            }
        }
        return this;
    }

    /** The {@code forgetInclude} / {@code forgetAppend} / {@code forgetSort} shape. */
    private QueryBuilder forgetFromList(Object[] args, Runnable clearAll, List<String> target,
            Recurse self) {
        if (args.length == 0) {
            clearAll.run();
            return this;
        }
        for (Object arg : args) {
            switch (JsType.of(arg)) {
                case JsType.ARRAY -> self.apply(JsObject.toList(arg).toArray());
                case JsType.STRING -> target.removeIf(v -> v.equals(arg.toString()));
                default -> throw new QueryBuilderException();
            }
        }
        return this;
    }

    private static Map<?, ?> asObject(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        // JavaScript would enumerate an arbitrary instance's own enumerable properties. The
        // source gives no guidance for translating that, and guessing a reflection rule would
        // invent behaviour, so only a Map models a JS object literal here.
        throw new QueryBuilderException();
    }
}
