package com.fabiankevin.app.services.queries;

import lombok.Builder;

@Builder
public record PageQuery(
        int page,
        int size,
        String sort,
        String direction
) {

    public static PageQuery withDefaults() {
        return PageQuery.builder()
                .page(0)
                .size(10)
                .sort("id")
                .direction("DESC")
                .build();
    }
}