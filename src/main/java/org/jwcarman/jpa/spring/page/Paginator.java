package org.jwcarman.jpa.spring.page;

import org.jwcarman.jpa.pagination.PageDto;
import org.jwcarman.jpa.pagination.PageSpec;
import org.jwcarman.jpa.pagination.PaginationDto;
import org.jwcarman.jpa.pagination.SortDirection;
import org.jwcarman.jpa.pagination.SortPropertyProvider;
import org.jwcarman.jpa.pagination.UnknownSortByValueException;
import org.springframework.data.core.PropertyPath;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Converts framework-agnostic {@link PageSpec} requests into Spring Data {@link Pageable}s, and Spring Data
 * {@link Page}s back into framework-agnostic {@link PageDto}s.
 *
 * <p>A paginator is configured once, for one entity type and sort enum, and reused. It is immutable and
 * thread-safe, so the usual home for one is a {@code static final} constant in the service that pages:</p>
 *
 * <pre>{@code
 * private static final Paginator PAGINATOR = Paginator.of(Person.class, PersonSort.class, cfg -> cfg
 *         .defaultSort(PersonSort.LAST_NAME)
 *         .defaultPageSize(25));
 *
 * public PageDto<PersonDto> search(String term, PageSpec spec) {
 *     return PAGINATOR.paginate(spec, pageable -> repository.search(term, pageable), PersonDto::of);
 * }
 * }</pre>
 *
 * <h2>Stable ordering</h2>
 * <p>SQL only orders rows whose sort values differ; rows that tie may come back in any order, and each page is a
 * separate query, so tied rows can repeat on one page and vanish from another. To prevent that, every
 * {@code Pageable} ends with a <em>tiebreaker</em>: one or more unique properties, sorted ascending, appended after
 * the requested sort (and used alone when nothing else is sorted). The default tiebreaker is {@code "id"}, the
 * identifier of {@link org.jwcarman.jpa.entity.BaseEntity}. Entities whose unique property is named differently
 * configure it with {@link Config#tiebreaker(String...)}; {@link Config#noTiebreaker()} opts out.</p>
 *
 * <h2>Property validation</h2>
 * <p>When a paginator is built, every sort enum constant's {@linkplain SortPropertyProvider#getSortProperty()
 * property} and every tiebreaker property is checked against the entity type, using the same property-path rules
 * Spring Data applies at query time. A typo or a missing property therefore fails when the paginator is created
 * (typically at class initialization or in the first test), not as a {@link PropertyReferenceException} on every
 * request. Queries that sort by something other than entity properties, such as native queries sorting by column
 * names, opt out with {@link Config#skipPropertyValidation()}.</p>
 *
 * <h2>Sort resolution</h2>
 * <ol>
 *   <li>{@link PageSpec#sortBy()} given: the matching enum constant, or {@link UnknownSortByValueException} if
 *       none matches. Surrounding whitespace is ignored; matching is case-sensitive.</li>
 *   <li>{@code sortBy} absent or blank, and a {@linkplain Config#defaultSort(Enum) default sort} configured: the
 *       default.</li>
 *   <li>Otherwise only the tiebreaker (or unsorted, when the tiebreaker is disabled).</li>
 * </ol>
 * <p>The requested {@link PageSpec#sortDirection()} applies to the sort property (ascending when absent); the
 * tiebreaker is always ascending.</p>
 *
 * <h2>Paging defaults</h2>
 * <ul>
 *   <li>Missing or negative page index: {@value #FIRST_PAGE}</li>
 *   <li>Missing page size: the configured default ({@value #DEFAULT_PAGE_SIZE} unless changed)</li>
 *   <li>Requested page sizes are clamped to {@value #MIN_PAGE_SIZE}..maximum ({@value #DEFAULT_MAX_PAGE_SIZE}
 *       unless changed)</li>
 *   <li>A {@code null} spec is treated as a spec with every value missing</li>
 * </ul>
 */
public final class Paginator {

// ------------------------------ FIELDS ------------------------------

    /** The index of the first page. */
    public static final int FIRST_PAGE = 0;

    /** The smallest page size a request can produce. */
    public static final int MIN_PAGE_SIZE = 1;

    /** The page size used when a request does not specify one, unless configured otherwise. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** The largest page size a request can produce, unless configured otherwise. */
    public static final int DEFAULT_MAX_PAGE_SIZE = 100;

    /** The tiebreaker property used unless configured otherwise: {@code BaseEntity}'s identifier. */
    public static final String DEFAULT_TIEBREAKER = "id";

    private final Function<String, SortPropertyProvider> sortResolver;
    private final SortPropertyProvider defaultSort;
    private final List<String> tiebreaker;
    private final int defaultPageSize;
    private final int maxPageSize;

// -------------------------- STATIC METHODS --------------------------

    /**
     * Creates a paginator for the given entity type and sort enum with the default settings: {@code "id"}
     * tiebreaker, no default sort, page size {@value #DEFAULT_PAGE_SIZE}, maximum page size
     * {@value #DEFAULT_MAX_PAGE_SIZE}.
     *
     * @param entityType    the entity the paged queries return; sort and tiebreaker properties are validated
     *                      against it
     * @param sortEnumClass the enum whose constant names are the accepted {@code sortBy} values
     * @param <S>           the sort enum type
     * @return the paginator
     * @throws IllegalArgumentException if a sort or tiebreaker property is not a property of {@code entityType}
     */
    public static <S extends Enum<S> & SortPropertyProvider> Paginator of(Class<?> entityType, Class<S> sortEnumClass) {
        return of(entityType, sortEnumClass, cfg -> {
        });
    }

    /**
     * Creates a paginator for the given entity type and sort enum, customizing the default settings.
     *
     * <pre>{@code
     * Paginator.of(MemberDirectoryEntry.class, MemberSort.class, cfg -> cfg
     *         .tiebreaker("identityId")
     *         .defaultSort(MemberSort.NAME)
     *         .defaultPageSize(25)
     *         .maxPageSize(100));
     * }</pre>
     *
     * @param entityType    the entity the paged queries return; sort and tiebreaker properties are validated
     *                      against it unless {@link Config#skipPropertyValidation()} is set
     * @param sortEnumClass the enum whose constant names are the accepted {@code sortBy} values
     * @param customizer    adjusts the settings
     * @param <S>           the sort enum type
     * @return the paginator
     * @throws IllegalArgumentException if the resulting settings are inconsistent, or a sort or tiebreaker property
     *                                  is not a property of {@code entityType}
     */
    public static <S extends Enum<S> & SortPropertyProvider> Paginator of(Class<?> entityType, Class<S> sortEnumClass, Consumer<Config<S>> customizer) {
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(sortEnumClass, "sortEnumClass must not be null");
        Objects.requireNonNull(customizer, "customizer must not be null");
        final Config<S> config = new Config<>();
        customizer.accept(config);
        config.validate();
        if (config.validateProperties) {
            validateProperties(entityType, sortEnumClass, config.tiebreaker);
        }
        return new Paginator(sortBy -> resolveSortEnum(sortBy, sortEnumClass), config);
    }

    private static <S extends Enum<S> & SortPropertyProvider> void validateProperties(Class<?> entityType, Class<S> sortEnumClass, List<String> tiebreaker) {
        for (S constant : sortEnumClass.getEnumConstants()) {
            final String property = constant.getSortProperty();
            if (property == null || property.isBlank()) {
                throw new IllegalArgumentException(String.format("%s.%s has no sort property",
                        sortEnumClass.getSimpleName(), constant.name()));
            }
            requireProperty(entityType, property, () -> String.format("%s.%s sorts by \"%s\"",
                    sortEnumClass.getSimpleName(), constant.name(), property));
        }
        for (String property : tiebreaker) {
            requireProperty(entityType, property, () -> String.format("tiebreaker \"%s\"", property));
        }
    }

    private static void requireProperty(Class<?> entityType, String property, Supplier<String> subject) {
        try {
            PropertyPath.from(property, entityType);
        } catch (PropertyReferenceException e) {
            throw new IllegalArgumentException(String.format(
                    "%s, which is not a property of %s (use skipPropertyValidation() if the query sorts by something else)",
                    subject.get(), entityType.getName()), e);
        }
    }

    private static <S extends Enum<S> & SortPropertyProvider> S resolveSortEnum(String sortBy, Class<S> sortEnumClass) {
        try {
            return Enum.valueOf(sortEnumClass, sortBy);
        } catch (IllegalArgumentException _) {
            final String[] expectedValues = Arrays.stream(sortEnumClass.getEnumConstants())
                    .map(Enum::name)
                    .toArray(String[]::new);
            throw new UnknownSortByValueException(sortBy, expectedValues);
        }
    }

    private static Sort.Direction directionOf(SortDirection sortDirection) {
        return sortDirection == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private Paginator(Function<String, SortPropertyProvider> sortResolver, Config<?> config) {
        this.sortResolver = sortResolver;
        this.defaultSort = config.defaultSort;
        this.tiebreaker = config.tiebreaker;
        this.defaultPageSize = config.defaultPageSize;
        this.maxPageSize = config.maxPageSize;
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Converts a page request into a Spring Data {@link Pageable}.
     *
     * @param spec the requested page; may be {@code null}
     * @return the pageable, always ending with the tiebreaker unless it is disabled
     * @throws UnknownSortByValueException if {@code spec.sortBy()} names no constant of the sort enum
     */
    public Pageable pageableOf(PageSpec spec) {
        if (spec == null) {
            return PageRequest.of(FIRST_PAGE, defaultPageSize, sortOf(null, null));
        }
        final int pageNumber = Math.max(FIRST_PAGE, Optional.ofNullable(spec.pageIndex()).orElse(FIRST_PAGE));
        final int pageSize = Optional.ofNullable(spec.pageSize())
                .map(size -> Math.clamp(size, MIN_PAGE_SIZE, maxPageSize))
                .orElse(defaultPageSize);
        return PageRequest.of(pageNumber, pageSize, sortOf(spec.sortBy(), spec.sortDirection()));
    }

    /**
     * Converts a Spring Data {@link Page} into a {@link PageDto}.
     *
     * @param page the page
     * @param <T>  the element type
     * @return the page DTO
     */
    public <T> PageDto<T> pageDtoOf(Page<T> page) {
        Objects.requireNonNull(page, "page must not be null");
        return new PageDto<>(page.getContent(), new PaginationDto(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()));
    }

    /**
     * Converts a Spring Data {@link Page} into a {@link PageDto}, mapping each element.
     *
     * @param page   the page
     * @param mapper converts each element, typically entity to DTO
     * @param <E>    the page's element type
     * @param <D>    the DTO's element type
     * @return the page DTO
     */
    public <E, D> PageDto<D> pageDtoOf(Page<E> page, Function<? super E, ? extends D> mapper) {
        Objects.requireNonNull(page, "page must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        return pageDtoOf(page.<D>map(mapper));
    }

    /**
     * Runs a paged query for a request and converts the result into a {@link PageDto}.
     *
     * <pre>{@code
     * PAGINATOR.paginate(spec, repository::findAll);
     * }</pre>
     *
     * @param spec  the requested page; may be {@code null}
     * @param query runs the query for the {@link Pageable} built from {@code spec}
     * @param <E>   the element type
     * @return the page DTO
     * @throws UnknownSortByValueException if {@code spec.sortBy()} names no constant of the sort enum
     */
    public <E> PageDto<E> paginate(PageSpec spec, Function<Pageable, Page<E>> query) {
        Objects.requireNonNull(query, "query must not be null");
        return pageDtoOf(query.apply(pageableOf(spec)));
    }

    /**
     * Runs a paged query for a request and converts the result into a {@link PageDto}, mapping each element.
     *
     * <pre>{@code
     * PAGINATOR.paginate(spec, pageable -> repository.search(term, pageable), PersonDto::of);
     * }</pre>
     *
     * @param spec   the requested page; may be {@code null}
     * @param query  runs the query for the {@link Pageable} built from {@code spec}
     * @param mapper converts each element, typically entity to DTO
     * @param <E>    the query's element type
     * @param <D>    the DTO's element type
     * @return the page DTO
     * @throws UnknownSortByValueException if {@code spec.sortBy()} names no constant of the sort enum
     */
    public <E, D> PageDto<D> paginate(PageSpec spec, Function<Pageable, Page<E>> query, Function<? super E, ? extends D> mapper) {
        Objects.requireNonNull(query, "query must not be null");
        return pageDtoOf(query.apply(pageableOf(spec)), mapper);
    }

    private Sort sortOf(String sortBy, SortDirection sortDirection) {
        Sort sort = Optional.ofNullable(sortBy)
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .map(sortResolver)
                .or(() -> Optional.ofNullable(defaultSort))
                .map(provider -> Sort.by(directionOf(sortDirection), provider.getSortProperty()))
                .orElseGet(Sort::unsorted);
        for (String property : tiebreaker) {
            if (sort.getOrderFor(property) == null) {
                sort = sort.and(Sort.by(Sort.Direction.ASC, property));
            }
        }
        return sort;
    }

// -------------------------- INNER CLASSES --------------------------

    /**
     * Settings for a {@link Paginator}, adjusted by the customizer passed to
     * {@link Paginator#of(Class, Class, Consumer)}. Each method returns this config for chaining; the settings are
     * validated together when the paginator is built.
     *
     * @param <S> the sort enum type
     */
    public static final class Config<S extends Enum<S> & SortPropertyProvider> {

        private SortPropertyProvider defaultSort;
        private List<String> tiebreaker = List.of(DEFAULT_TIEBREAKER);
        private int defaultPageSize = DEFAULT_PAGE_SIZE;
        private int maxPageSize = DEFAULT_MAX_PAGE_SIZE;
        private boolean validateProperties = true;

        private Config() {
        }

        /**
         * Sets the unique property (or properties, for a composite key) appended ascending as the final sort
         * order. Defaults to {@value Paginator#DEFAULT_TIEBREAKER}. The properties together must be unique, or
         * ties remain.
         *
         * @param properties JPA property paths, e.g. {@code "identityId"} or {@code "order.id", "lineNo"}
         * @return this config
         * @throws IllegalArgumentException if no property is given or any is blank
         */
        public Config<S> tiebreaker(String... properties) {
            Objects.requireNonNull(properties, "properties must not be null");
            if (properties.length == 0) {
                throw new IllegalArgumentException("tiebreaker requires at least one property; use noTiebreaker() to disable it");
            }
            for (String property : properties) {
                if (property == null || property.isBlank()) {
                    throw new IllegalArgumentException("tiebreaker properties must not be blank");
                }
            }
            this.tiebreaker = List.of(properties);
            return this;
        }

        /**
         * Disables the tiebreaker. Pages are then only stable when the requested sort is unique on its own.
         *
         * @return this config
         */
        public Config<S> noTiebreaker() {
            this.tiebreaker = List.of();
            return this;
        }

        /**
         * Skips checking the sort and tiebreaker properties against the entity type. Use it only when the paged
         * query does not sort by entity properties, e.g. a native query whose sort keys are column names.
         *
         * @return this config
         */
        public Config<S> skipPropertyValidation() {
            this.validateProperties = false;
            return this;
        }

        /**
         * Sets the sort used when a request has no (or a blank) {@code sortBy}. The request's direction still applies.
         *
         * @param defaultSort the default sort
         * @return this config
         */
        public Config<S> defaultSort(S defaultSort) {
            this.defaultSort = Objects.requireNonNull(defaultSort, "defaultSort must not be null");
            return this;
        }

        /**
         * Sets the page size used when a request has none. Defaults to {@value Paginator#DEFAULT_PAGE_SIZE}.
         *
         * @param defaultPageSize the default page size; between {@value Paginator#MIN_PAGE_SIZE} and the maximum
         * @return this config
         */
        public Config<S> defaultPageSize(int defaultPageSize) {
            this.defaultPageSize = defaultPageSize;
            return this;
        }

        /**
         * Sets the largest page size a request can produce. Defaults to {@value Paginator#DEFAULT_MAX_PAGE_SIZE}.
         *
         * @param maxPageSize the maximum page size; at least the default page size
         * @return this config
         */
        public Config<S> maxPageSize(int maxPageSize) {
            this.maxPageSize = maxPageSize;
            return this;
        }

        private void validate() {
            if (maxPageSize < MIN_PAGE_SIZE) {
                throw new IllegalArgumentException("maxPageSize must be at least " + MIN_PAGE_SIZE + " but was " + maxPageSize);
            }
            if (defaultPageSize < MIN_PAGE_SIZE || defaultPageSize > maxPageSize) {
                throw new IllegalArgumentException("defaultPageSize must be between " + MIN_PAGE_SIZE + " and maxPageSize (" + maxPageSize + ") but was " + defaultPageSize);
            }
        }
    }
}
