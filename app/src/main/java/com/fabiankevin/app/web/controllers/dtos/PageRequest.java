package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.services.queries.PageQuery;

public record PageRequest(
        int page,
        int size,
        String sort,
        String direction) {

    public PageQuery toQuery() {
        return new PageQuery(page, size, sort, direction);
    }
}