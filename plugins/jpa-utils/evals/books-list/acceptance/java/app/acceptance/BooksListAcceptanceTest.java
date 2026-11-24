package app.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The HTTP contract the books-list prompt pins. Each test starts from an empty database (a fresh
 * context), so it does not depend on how the run named its entity or table.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class BooksListAcceptanceTest {

  @Autowired MockMvc mvc;

  private String add(String title, String author, String published) throws Exception {
    String body =
        mvc.perform(
                post("/api/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"title\":\"%s\",\"author\":\"%s\",\"published\":\"%s\"}"
                            .formatted(title, author, published)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  private int post400(String json) throws Exception {
    return mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(json))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private String list(String query) throws Exception {
    return mvc.perform(get("/api/books" + query))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String search(String term) throws Exception {
    return mvc.perform(get("/api/books").param("search", term))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private static List<String> titles(String page) {
    return JsonPath.read(page, "$.data[*].title");
  }

  private static String title(int n) {
    return "Book %02d".formatted(n);
  }

  @Test
  void adding_a_book_answers_201_with_it() throws Exception {
    String body =
        mvc.perform(
                post("/api/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"published\":\"1965-08-01\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Map<String, Object> book = JsonPath.read(body, "$");
    assertThat(book).containsKeys("id", "title", "author", "published");
    assertThat(book.get("id")).isNotNull();
    assertThat(book.get("title")).isEqualTo("Dune");
    assertThat(book.get("author")).isEqualTo("Frank Herbert");
    assertThat(book.get("published")).isEqualTo("1965-08-01");
  }

  @Test
  void an_invalid_book_is_a_400() throws Exception {
    String longText = "x".repeat(101);
    assertThat(post400("{\"title\":\"\",\"author\":\"A\",\"published\":\"2020-01-01\"}")).isEqualTo(400);
    assertThat(post400("{\"author\":\"A\",\"published\":\"2020-01-01\"}")).isEqualTo(400);
    assertThat(post400("{\"title\":\"T\",\"author\":\" \",\"published\":\"2020-01-01\"}")).isEqualTo(400);
    assertThat(post400("{\"title\":\"%s\",\"author\":\"A\",\"published\":\"2020-01-01\"}".formatted(longText)))
        .isEqualTo(400);
    assertThat(post400("{\"title\":\"T\",\"author\":\"%s\",\"published\":\"2020-01-01\"}".formatted(longText)))
        .isEqualTo(400);
    assertThat(post400("{\"title\":\"T\",\"author\":\"A\"}")).isEqualTo(400);
    assertThat(post400("{\"title\":\"%s\",\"author\":\"A\",\"published\":\"2020-01-01\"}".formatted("x".repeat(100))))
        .isEqualTo(201);
  }

  @Test
  void the_default_page_is_the_first_20_by_title() throws Exception {
    for (int n = 25; n >= 1; n--) {
      add(title(n), "Author", "2020-01-01");
    }
    String page = list("");
    assertThat(titles(page)).hasSize(20).first().isEqualTo(title(1));
    assertThat(titles(page).get(19)).isEqualTo(title(20));
    Map<String, Object> p = JsonPath.read(page, "$.pagination");
    assertThat(p)
        .containsEntry("pageIndex", 0)
        .containsEntry("pageSize", 20)
        .containsEntry("totalElementCount", 25)
        .containsEntry("totalPageCount", 2)
        .containsEntry("hasNext", true)
        .containsEntry("hasPrevious", false);
  }

  @Test
  void the_middle_page_of_three() throws Exception {
    for (int n = 1; n <= 45; n++) {
      add(title(n), "Author", "2020-01-01");
    }
    String page = list("?pageIndex=1&pageSize=15");
    assertThat(titles(page)).hasSize(15).first().isEqualTo(title(16));
    assertThat(titles(page).get(14)).isEqualTo(title(30));
    Map<String, Object> p = JsonPath.read(page, "$.pagination");
    assertThat(p)
        .containsEntry("pageIndex", 1)
        .containsEntry("pageSize", 15)
        .containsEntry("totalElementCount", 45)
        .containsEntry("totalPageCount", 3)
        .containsEntry("hasNext", true)
        .containsEntry("hasPrevious", true);
  }

  @Test
  void each_sort_both_ways_in_any_case() throws Exception {
    add("Bravo", "Charlie", "2001-01-01");
    add("Alpha", "Bravo", "2003-01-01");
    add("Charlie", "Alpha", "2002-01-01");
    assertThat(titles(list("?sortBy=TITLE"))).containsExactly("Alpha", "Bravo", "Charlie");
    assertThat(titles(list("?sortBy=TITLE&sortDirection=DESC"))).containsExactly("Charlie", "Bravo", "Alpha");
    assertThat(titles(list("?sortBy=AUTHOR&sortDirection=ASC"))).containsExactly("Charlie", "Alpha", "Bravo");
    assertThat(titles(list("?sortBy=AUTHOR&sortDirection=desc"))).containsExactly("Bravo", "Alpha", "Charlie");
    assertThat(titles(list("?sortBy=PUBLISHED"))).containsExactly("Bravo", "Charlie", "Alpha");
    assertThat(titles(list("?sortBy=PUBLISHED&sortDirection=Desc"))).containsExactly("Alpha", "Charlie", "Bravo");
  }

  @Test
  void search_matches_title_or_author_ignoring_case() throws Exception {
    add("The Hobbit", "J. R. R. Tolkien", "1937-09-21");
    add("Mistborn", "Brandon Sanderson", "2006-07-17");
    add("Tolkien: A Biography", "Humphrey Carpenter", "1977-05-04");
    assertThat(titles(list("?search=tolkien"))).containsExactlyInAnyOrder("The Hobbit", "Tolkien: A Biography");
    assertThat(titles(list("?search=HOBB"))).containsExactly("The Hobbit");
    assertThat(titles(list("?search=sanderson"))).containsExactly("Mistborn");
    assertThat(titles(list("?search=nothing-like-this"))).isEmpty();
    assertThat((Integer) JsonPath.read(list("?search=tolkien"), "$.pagination.totalElementCount")).isEqualTo(2);
  }

  @Test
  void percent_and_underscore_in_a_search_match_themselves() throws Exception {
    add("100% Kitchen", "Cook", "2010-01-01");
    add("1000 Recipes", "Cook", "2011-01-01");
    add("snake_case", "Coder", "2012-01-01");
    add("snakeXcase", "Coder", "2013-01-01");
    assertThat(titles(search("100%"))).containsExactly("100% Kitchen");
    assertThat(titles(search("snake_case"))).containsExactly("snake_case");
  }

  @Test
  void an_unknown_sort_by_is_a_400() throws Exception {
    add("Dune", "Frank Herbert", "1965-08-01");
    mvc.perform(get("/api/books?sortBy=COLOR")).andExpect(status().isBadRequest());
  }

  @Test
  void paging_through_ties_shows_every_book_once() throws Exception {
    Set<String> added = new HashSet<>();
    LocalDate day = LocalDate.of(2020, 1, 1);
    for (int n = 1; n <= 12; n++) {
      added.add(add("Same Title", "Same Author", day.toString()));
    }
    List<String> seen = new ArrayList<>();
    for (int pageIndex = 0; pageIndex < 3; pageIndex++) {
      String page = list("?sortBy=AUTHOR&pageSize=5&pageIndex=" + pageIndex);
      List<String> ids = JsonPath.read(page, "$.data[*].id");
      seen.addAll(ids);
    }
    assertThat(seen).hasSize(12).doesNotHaveDuplicates();
    assertThat(new HashSet<>(seen)).isEqualTo(added);
  }
}
