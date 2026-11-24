package app.books;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record NewBook(
    @NotBlank @Size(max = 100) String title,
    @NotBlank @Size(max = 100) String author,
    @NotNull LocalDate published) {}
