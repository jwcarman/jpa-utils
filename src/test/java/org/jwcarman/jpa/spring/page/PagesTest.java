package org.jwcarman.jpa.spring.page;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PagesTest {

    @Test
    void shouldCreateDtoWithEmptyPage() {
        final var page = Page.<String>empty();
        final var dto = Pages.pageDtoOf(page);

        assertThat(dto.data()).isEmpty();
        assertThat(dto.pagination()).isNotNull();

        assertThat(dto.pagination().pageIndex()).isZero();
        assertThat(dto.pagination().pageSize()).isZero();
        assertThat(dto.pagination().totalElementCount()).isZero();
        assertThat(dto.pagination().hasNext()).isFalse();
        assertThat(dto.pagination().hasPrevious()).isFalse();
        assertThat(dto.pagination().totalPageCount()).isEqualTo(1L);
    }

    @Test
    void shouldCreateValidDto() {
        final var page = new PageImpl<>(List.of("one", "two", "three"));
        final var dto = Pages.pageDtoOf(page);

        assertThat(dto.data()).containsExactly("one", "two", "three");
        assertThat(dto.pagination()).isNotNull();

        assertThat(dto.pagination().pageIndex()).isZero();
        assertThat(dto.pagination().pageSize()).isEqualTo(3);
        assertThat(dto.pagination().totalElementCount()).isEqualTo(3L);
        assertThat(dto.pagination().hasNext()).isFalse();
        assertThat(dto.pagination().hasPrevious()).isFalse();
        assertThat(dto.pagination().totalPageCount()).isEqualTo(1L);

    }
}