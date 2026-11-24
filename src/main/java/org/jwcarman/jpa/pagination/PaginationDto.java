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
 * A DTO representing pagination information.
 *
 * @param pageIndex The index of the page
 * @param pageSize The size of the page
 * @param totalElementCount The total number of elements to be paginated
 * @param totalPageCount The total number of pages
 * @param hasNext Whether there is a next page
 * @param hasPrevious Whether there is a previous page
 */
public record PaginationDto(
    int pageIndex,
    int pageSize,
    long totalElementCount,
    long totalPageCount,
    boolean hasNext,
    boolean hasPrevious) {}
