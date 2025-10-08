package org.jwcarman.jpa.integration;

import org.jwcarman.jpa.pagination.SortPropertyProvider;

public enum TestPersonSort implements SortPropertyProvider {
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    EMAIL("email");

    private final String sortProperty;

    TestPersonSort(String sortProperty) {
        this.sortProperty = sortProperty;
    }

    @Override
    public String getSortProperty() {
        return sortProperty;
    }
}
