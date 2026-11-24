package app.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The HTTP contract the books-audit prompt pins. Each test starts from an empty database (a fresh
 * context).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class BooksAuditAcceptanceTest {

  @Autowired MockMvc mvc;

  private MockHttpServletResponse send(MockHttpServletRequestBuilder request, String user, String json)
      throws Exception {
    request.contentType(MediaType.APPLICATION_JSON).content(json);
    if (user != null) {
      request.header("X-User", user);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private Map<String, Object> add(String user) throws Exception {
    MockHttpServletResponse r =
        send(post("/api/books"), user, "{\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"published\":\"1965-08-01\"}");
    assertThat(r.getStatus()).isEqualTo(201);
    return JsonPath.read(r.getContentAsString(), "$");
  }

  private MockHttpServletResponse change(Map<String, Object> book, String user, String title) throws Exception {
    return send(
        put("/api/books/" + book.get("id")),
        user,
        "{\"title\":\"%s\",\"author\":\"Frank Herbert\",\"published\":\"1965-08-01\",\"version\":%s}"
            .formatted(title, book.get("version")));
  }

  private static Instant instant(Object value) {
    assertThat(value).isInstanceOf(String.class);
    return Instant.parse((String) value).truncatedTo(ChronoUnit.MILLIS);
  }

  @Test
  void adding_a_book_records_who_and_when() throws Exception {
    Instant before = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    Map<String, Object> book = add("alice");
    assertThat(book)
        .containsKeys("id", "title", "author", "published", "createdBy", "createdDate", "modifiedBy", "modifiedDate", "version")
        .containsEntry("createdBy", "alice")
        .containsEntry("modifiedBy", "alice");
    assertThat(book.get("version")).isNotNull();
    assertThat(instant(book.get("createdDate"))).isAfterOrEqualTo(before.minusSeconds(1));
    assertThat(instant(book.get("modifiedDate"))).isAfterOrEqualTo(before.minusSeconds(1));
  }

  @Test
  void a_request_without_a_user_is_made_by_system() throws Exception {
    Map<String, Object> book = add(null);
    assertThat(book).containsEntry("createdBy", "system").containsEntry("modifiedBy", "system");
  }

  @Test
  void changing_a_book_changes_only_who_last_changed_it_and_when() throws Exception {
    Map<String, Object> added = add("alice");
    Thread.sleep(50);
    MockHttpServletResponse r = change(added, "bob", "Dune Messiah");
    assertThat(r.getStatus()).isEqualTo(200);
    Map<String, Object> changed = JsonPath.read(r.getContentAsString(), "$");
    assertThat(changed)
        .containsEntry("id", added.get("id"))
        .containsEntry("title", "Dune Messiah")
        .containsEntry("createdBy", "alice")
        .containsEntry("modifiedBy", "bob");
    assertThat(instant(changed.get("createdDate"))).isEqualTo(instant(added.get("createdDate")));
    assertThat(instant(changed.get("modifiedDate"))).isAfter(instant(added.get("modifiedDate")));
    assertThat(changed.get("version")).isNotEqualTo(added.get("version"));

    String page = mvc.perform(get("/api/books")).andReturn().getResponse().getContentAsString();
    List<Map<String, Object>> data = JsonPath.read(page, "$.data");
    assertThat(data).hasSize(1);
    assertThat(data.get(0)).containsEntry("title", "Dune Messiah").containsEntry("createdBy", "alice").containsEntry("modifiedBy", "bob");
  }

  @Test
  void a_change_from_a_stale_version_is_a_409_and_changes_nothing() throws Exception {
    Map<String, Object> added = add("alice");
    assertThat(change(added, "bob", "First Change").getStatus()).isEqualTo(200);
    assertThat(change(added, "carol", "Second Change").getStatus()).isEqualTo(409);

    String page = mvc.perform(get("/api/books")).andReturn().getResponse().getContentAsString();
    List<Map<String, Object>> data = JsonPath.read(page, "$.data");
    assertThat(data).hasSize(1);
    assertThat(data.get(0)).containsEntry("title", "First Change").containsEntry("modifiedBy", "bob");
  }

  @Test
  void a_change_is_checked_like_a_new_book() throws Exception {
    Map<String, Object> added = add("alice");
    assertThat(change(added, "bob", "").getStatus()).isEqualTo(400);
    assertThat(change(added, "bob", "x".repeat(101)).getStatus()).isEqualTo(400);
  }
}
