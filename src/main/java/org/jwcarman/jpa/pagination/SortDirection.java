/*
 * Copyright © 2025 James Carman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jwcarman.jpa.pagination;

/**
 * Enumeration representing the direction for sorting query results.
 *
 * <p>This enum provides a framework-agnostic way to specify sort order in pagination requests,
 * which is then translated to framework-specific sort directions (e.g., Spring Data's {@code
 * Sort.Direction}) by {@code Paginator}.
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>In REST APIs</h3>
 *
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/products")
 * public class ProductController {
 *
 *     @GetMapping
 *     public PageDto<ProductDto> getProducts(PageParams pageParams) {
 *         // pageParams.sortDirection() will be ASC or DESC from query param
 *         return productService.findAll(pageParams);
 *     }
 * }
 *
 * // Example URLs:
 * // GET /api/products?sortBy=PRICE&sortDirection=ASC   (low to high)
 * // GET /api/products?sortBy=PRICE&sortDirection=DESC  (high to low)
 * }</pre>
 *
 * <h3>In Service Layer</h3>
 *
 * <pre>{@code
 * @Service
 * public class ProductService {
 *
 *     private static final Paginator PAGINATOR = Paginator.of(Product.class, ProductSort.class);
 *
 *     public PageDto<ProductDto> findAll(PageSpec pageSpec) {
 *         // Paginator converts SortDirection to Spring's Sort.Direction
 *         return PAGINATOR.paginate(pageSpec, productRepository::findAll, ProductDto::fromEntity);
 *     }
 * }
 * }</pre>
 *
 * <h3>Creating Page Specifications</h3>
 *
 * <pre>{@code
 * // Sort ascending (A-Z, 0-9, oldest-newest)
 * PageSpec spec1 = new PageRequestDto(0, 20, "NAME", SortDirection.ASC);
 *
 * // Sort descending (Z-A, 9-0, newest-oldest)
 * PageSpec spec2 = new PageRequestDto(0, 20, "CREATED", SortDirection.DESC);
 * }</pre>
 *
 * @see PageSpec#sortDirection()
 * @see org.jwcarman.jpa.spring.page.Paginator
 */
public enum SortDirection {
  /**
   * Ascending sort order.
   *
   * <ul>
   *   <li>Text: A → Z
   *   <li>Numbers: 0 → 9
   *   <li>Dates: oldest → newest
   * </ul>
   */
  ASC,

  /**
   * Descending sort order.
   *
   * <ul>
   *   <li>Text: Z → A
   *   <li>Numbers: 9 → 0
   *   <li>Dates: newest → oldest
   * </ul>
   */
  DESC
}
