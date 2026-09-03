# Contributing

Contributions are **welcome** and will be fully **credited**.

Please read and understand the contribution guide before creating an issue or pull request.

## Requirements

If the project maintainer has any additional requirements, you will find them listed here.

- **Java 21 and Maven 3.9.** Build with `mvn clean verify`; the build treats no warnings as
  acceptable, so keep `-Xlint:all` clean.
- **Add tests!** Your patch won't be accepted if it doesn't have tests.
- **Document any change in behaviour.** Make sure the `README.md` and any other relevant
  documentation are kept up-to-date.
- **Consider our release cycle.** We try to follow [SemVer v2.0.0](http://semver.org/). Randomly
  breaking public APIs is not an option.
- **Create feature branches.** Don't ask us to pull from your master branch.
- **One pull request per feature.** If you want to do more than one thing, send multiple pull
  requests.
- **Send coherent history.** Make sure each individual commit in your pull request is meaningful.
  If you had to make multiple intermediate commits while developing, please
  [squash them](http://www.git-scm.com/book/en/v2/Git-Tools-Rewriting-History#Changing-Multiple-Commit-Messages)
  before submitting.

## Keeping parity with the JavaScript original

This repository is a port of [coderello/js-query-builder](https://github.com/coderello/js-query-builder),
and that repository remains the reference for behaviour. Any behavioural change must keep the
differential harness green:

```bash
mvn test          # replays the committed baseline; needs no Node
```

`harness/corpus.json` and `harness/expected-js.json` are the recorded behaviour of the original
JavaScript implementation across 615 builder programs. They are committed fixtures: this
repository ships no JavaScript and no generator, so `mvn test` needs neither Node nor the
JavaScript sources.

If your change is *intended* to alter behaviour, those fixtures must be re-derived from the
JavaScript original rather than hand-edited — editing them to match new Java output would defeat
the check entirely. Say so explicitly in the pull request. A baseline change that is not explained
will be treated as an accidental regression.

## Codebase layout

| Path | Contents |
| --- | --- |
| `QueryBuilder.java` | The builder. Mirrors `src/QueryBuilder.js` method for method. |
| `Query.java` | The `query(...)` factory, from `src/query.js` and `src/index.js`. |
| `QueryBuilderException.java` | Thrown wherever the source throws `new Error()`. |
| `internal/JsType.java` | `gettype` — the JavaScript internal-class tag. |
| `internal/JsValues.java` | Truthiness, `String()`, ECMAScript `Number::toString`. |
| `internal/JsObject.java` | `Object.entries` ordering, array coercion. |
| `internal/JsUri.java` | `encodeURIComponent` (**not** `URLEncoder`). |

The `internal` package encodes JavaScript semantics that Java does not have. Change it only with
a very good reason — each behaviour there is pinned by a dedicated test, and getting one wrong
produces silently wrong query strings rather than a compile error.

**Happy coding!**
