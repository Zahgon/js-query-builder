package io.github.coderello.querybuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Reproduces the worked examples from the source repository's README. Added by the migration.
 *
 * <p>The README states the exact URL each snippet produces, which makes it an independent
 * specification written before this port existed — separate from the test suite and from the
 * differential corpus.
 */
class ReadmeExampleTest {

    @AfterEach
    void resetGlobalState() {
        QueryBuilder.forgetCustomParameterNames();
    }

    @Test
    @DisplayName("the general example produces the URL documented in the README")
    void generalExample() {
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

        assertEquals(
                "/users?append=fullname%2Cranking&custom_param=value"
                        + "&fields%5Bcomments%5D=id%2Ccontent&fields%5Bposts%5D=id%2Cname"
                        + "&filter%5Bage%5D=20&include=posts%2Ccomments&page=1"
                        + "&sort=-created_at%2Cname",
                url);

        assertEquals(
                "/users?append=fullname,ranking&custom_param=value"
                        + "&fields[comments]=id,content&fields[posts]=id,name"
                        + "&filter[age]=20&include=posts,comments&page=1"
                        + "&sort=-created_at,name",
                URLDecoder.decode(url, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("the conditions example skips the filter for a short username")
    void conditionsExample() {
        String username = "hi";

        assertEquals("/users?",
                new QueryBuilder("/users")
                        .when(username.length() > 3, q -> q.filter("name", username))
                        .build());

        String longer = "hivokas";
        assertEquals("/users?filter%5Bname%5D=hivokas",
                new QueryBuilder("/users")
                        .when(longer.length() > 3, q -> q.filter("name", longer))
                        .build());
    }

    @Test
    @DisplayName("the forgetting example drops the forgotten include")
    void forgettingExample() {
        assertEquals("/users?include=posts&sort=name",
                new QueryBuilder("/users")
                        .include("comments", "posts")
                        .sort("name")
                        .forgetInclude("comments")
                        .build());
    }

    @Test
    @DisplayName("the custom parameter names example renames page and sort")
    void customParameterNamesExample() {
        QueryBuilder.defineCustomParameterNames(Map.of("page", "p", "sort", "s"));

        String url = Query.query("/users").sort("name").page(5).build();

        assertEquals("/users?p=5&s=name",
                URLDecoder.decode(url, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("the tapping example observes the intermediate state without changing the result")
    void tappingExample() {
        StringBuilder observed = new StringBuilder();

        String url = Query.query("/users")
                .sort("id")
                .tap(q -> observed.append(q.build()))
                .include("comments")
                .build();

        assertEquals("/users?sort=id", observed.toString(), "tap sees the state at that point");
        assertEquals("/users?include=comments&sort=id", url);
    }
}
