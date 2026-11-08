package org.jwcarman.jpa.spring.web;

import org.jwcarman.jpa.pagination.SortDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PageParamsTest.TestController.class)
@ImportAutoConfiguration(PagingWebAutoConfiguration.class)
class PageParamsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestController controller;

    @Test
    void shouldBindAllQueryParameters() throws Exception {
        mockMvc.perform(get("/test")
                        .param("pageIndex", "2")
                        .param("pageSize", "50")
                        .param("sortBy", "LAST_NAME")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk());

        PageParams captured = controller.getLastPageParams();
        assertThat(captured).isNotNull();
        assertThat(captured.pageIndex()).isEqualTo(2);
        assertThat(captured.pageSize()).isEqualTo(50);
        assertThat(captured.sortBy()).isEqualTo("LAST_NAME");
        assertThat(captured.sortDirection()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void shouldHandleNullableParameters() throws Exception {
        mockMvc.perform(get("/test"))
                .andExpect(status().isOk());

        PageParams captured = controller.getLastPageParams();
        assertThat(captured).isNotNull();
        assertThat(captured.pageIndex()).isNull();
        assertThat(captured.pageSize()).isNull();
        assertThat(captured.sortBy()).isNull();
        assertThat(captured.sortDirection()).isNull();
    }

    @Test
    void shouldHandlePartialParameters() throws Exception {
        mockMvc.perform(get("/test")
                        .param("pageIndex", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());

        PageParams captured = controller.getLastPageParams();
        assertThat(captured).isNotNull();
        assertThat(captured.pageIndex()).isZero();
        assertThat(captured.pageSize()).isEqualTo(10);
        assertThat(captured.sortBy()).isNull();
        assertThat(captured.sortDirection()).isNull();
    }

    @Test
    void shouldBindSortParametersOnly() throws Exception {
        mockMvc.perform(get("/test")
                        .param("sortBy", "CREATED_DATE")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk());

        PageParams captured = controller.getLastPageParams();
        assertThat(captured).isNotNull();
        assertThat(captured.pageIndex()).isNull();
        assertThat(captured.pageSize()).isNull();
        assertThat(captured.sortBy()).isEqualTo("CREATED_DATE");
        assertThat(captured.sortDirection()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void shouldHandleZeroValues() throws Exception {
        mockMvc.perform(get("/test")
                        .param("pageIndex", "0")
                        .param("pageSize", "0"))
                .andExpect(status().isOk());

        PageParams captured = controller.getLastPageParams();
        assertThat(captured).isNotNull();
        assertThat(captured.pageIndex()).isZero();
        assertThat(captured.pageSize()).isZero();
    }

    @Test
    void shouldBindLowercaseSortDirection() throws Exception {
        mockMvc.perform(get("/test").param("sortDirection", "desc"))
                .andExpect(status().isOk());

        assertThat(controller.getLastPageParams().sortDirection()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void shouldBindMixedCaseSortDirectionWithWhitespace() throws Exception {
        mockMvc.perform(get("/test").param("sortDirection", " Asc "))
                .andExpect(status().isOk());

        assertThat(controller.getLastPageParams().sortDirection()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void shouldTreatBlankSortDirectionAsAbsent() throws Exception {
        mockMvc.perform(get("/test").param("sortDirection", ""))
                .andExpect(status().isOk());

        assertThat(controller.getLastPageParams().sortDirection()).isNull();
    }

    @Test
    void shouldRejectUnknownSortDirection() throws Exception {
        mockMvc.perform(get("/test").param("sortDirection", "sideways"))
                .andExpect(status().isBadRequest());
    }

    @Configuration
    static class TestConfig {
        @Bean
        public TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {
        private PageParams lastPageParams;

        @GetMapping("/test")
        public String testEndpoint(PageParams pageParams) {
            this.lastPageParams = pageParams;
            return "ok";
        }

        public PageParams getLastPageParams() {
            return lastPageParams;
        }
    }
}
