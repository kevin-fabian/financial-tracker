package com.fabiankevin.app.services.queries;

public record PageQuery(
        int page,
        int size,
        String sort,
        String direction
) {
}