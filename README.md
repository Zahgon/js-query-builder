<p align="center"><b>Java Query Builder</b> provides an easy way to build a query string compatible with <a href="https://github.com/spatie/laravel-query-builder">spatie/laravel-query-builder</a>.</p>

<p align="center">
<a href="https://opensource.org/licenses/MIT"><img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-yellow.svg"></a>
<a href="https://adoptium.net/"><img alt="Java 21+" src="https://img.shields.io/badge/java-21%2B-orange"></a>
</p>

> A behaviour-preserving Java port of [coderello/js-query-builder](https://github.com/coderello/js-query-builder).
> See [Java port](#java-port) for what that guarantees and how it is verified.

## Install

Build and install into your local Maven repository:

```bash
git clone https://github.com/coderello/js-query-builder.git
cd js-query-builder
mvn clean install
```

Then depend on it:

```xml
<dependency>
  <groupId>io.github.coderello</groupId>
  <artifactId>js-query-builder</artifactId>
  <version>0.2.0</version>
</dependency>
```

Requires **Java 21+**. The published jar has **zero runtime dependencies**.

## Usage

### General example

```java
import io.github.coderello.querybuilder.QueryBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

Map<String, Object> fields = new LinkedHashMap<>();
fields.put("posts", List.of("id", "name"));
fields.put("comments", List.of("id", "content"));

String url = new QueryBuilder("/users")
        .filter("age", 20)
        .sort("-created_at", "name")
        .include("posts", "comments")
        .append("fullname", "ranking")
        .fields(fields)
        .param("custom_param", "value")
        .page(1)
        .build();

System.out.println(url);
// /users?append=fullname%2Cranking&custom_param=value&fields%5Bcomments%5D=id%2Ccontent&fields%5Bposts%5D=id%2Cname&filter%5Bage%5D=20&include=posts%2Ccomments&page=1&sort=-created_at%2Cname

System.out.println(URLDecoder.decode(url, StandardCharsets.UTF_8));
// /users?append=fullname,ranking&custom_param=value&fields[comments]=id,content&fields[posts]=id,name&filter[age]=20&include=posts,comments&page=1&sort=-created_at,name
```

`Query.query(...)` is the equivalent of the JavaScript `query(...)` factory:

```java
import static io.github.coderello.querybuilder.Query.query;

String url = query("/users").sort("id").build();
```

### Making requests

This library does not make requests, because there is no need. You are not limited to any
particular HTTP client — use the one you want.

```java
HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(query("https://api.test/users")
                .filter("status", "active")
                .sort("-id")
                .page(1)
                .build()))
        .build();
```

### Conditions

Suppose you need to filter by username only if it is longer than 3 characters:

```java
String username = "hi";

String url = query("/users")
        .when(username.length() > 3, q -> q.filter("name", username))
        .build();
// /users?
```

The condition is evaluated for JavaScript-style *truthiness*, not just `true`. Passing a
`Supplier` defers evaluation, matching the source's function-condition branch:

```java
query("/users").when((Supplier<Object>) () -> expensiveCheck(), q -> q.page(2));
```

### Tapping

`.tap()` is `.when()` without a condition:

```java
String url = query("/users")
        .sort("id")
        .tap(q -> System.out.println(q.build()))
        .include("comments")
        .build();
```

### Forgetting

```java
String url = query("/users")
        .include("comments", "posts")
        .sort("name")
        .forgetInclude("comments")
        .build();
// /users?include=posts&sort=name
```

Calling any `forget*` method with no arguments clears that collection entirely.

### Customizing parameter names

```java
// e.g. during application bootstrap
QueryBuilder.defineCustomParameterNames(Map.of(
        "page", "p",
        "sort", "s"));

String url = query("/users").sort("name").page(5).build();
// /users?p=5&s=name
```

This is **global, process-wide state**, exactly as in the JavaScript original: builders created
before the call are affected too, because names are resolved at `build()` time. Call
`QueryBuilder.forgetCustomParameterNames()` to restore the defaults — tests should do this in a
`@BeforeEach`.

### Dynamic and typed calls

Every variadic method keeps the source's dynamic contract and also offers typed overloads:

```java
q.filter("a", "b");                       // typed
q.filter("a", List.of("x", "y"));         // typed
q.filter(Map.of("a", "b"));               // typed
q.filter(new Object[] {"a", "b"});        // dynamic, identical behaviour
```

Invalid arguments throw `QueryBuilderException`, at exactly the points the JavaScript throws
`new Error()`.

## Testing

```bash
mvn test
```

## Java port

- This is a **behaviour-preserving port**. The JavaScript repository is the source of truth; where
  the two disagree, the JavaScript behaviour is correct by definition.
- Equivalence is enforced by executable comparison, not by review. `harness/corpus.json` holds
  615 builder programs and `harness/expected-js.json` holds what the original
  `src/QueryBuilder.js` produced for each; `DifferentialTest` replays them in Java on every build
  and requires identical output. The comparison is mutation-tested to prove it is not vacuously
  passing.
- JavaScript semantics with no Java equivalent live in `internal/` and are individually
  unit-tested: `gettype`, truthiness, `String()` / `Number::toString`, `Object.entries` ordering,
  and `encodeURIComponent`.
- **Quirks were preserved, not fixed.** `page(0)`, `page("")` and `page(Double.NaN)` all emit no
  page parameter, because `build()` guards with a truthiness test. `page("0")` does emit one.
- Validation failures throw `QueryBuilderException` with **no message**, matching the source's
  bare `new Error()`.
- **One accepted difference:** integral `long` / `BigInteger` values are rendered exactly rather
  than through a `double` first. Every number JavaScript can represent is identical either way;
  they diverge only above 2^53, where JavaScript has already lost the value.

Those two files are committed fixtures, so `mvn test` needs neither Node nor the JavaScript
sources. This library ships no JavaScript of its own.

## Contributing

Please see [CONTRIBUTING](CONTRIBUTING.md) for details.

## Credits

- [Ilya Sakovich](https://github.com/hivokas)
- [All Contributors](../../contributors)

Inspired by [robsontenorio/vue-api-query](https://github.com/robsontenorio/vue-api-query).

## License

The MIT License (MIT). Please see [License File](LICENSE.md) for more information.
