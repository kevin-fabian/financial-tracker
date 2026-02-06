package com.fabiankevin.app.models;

import lombok.Builder;

import java.util.List;

@Builder
public record Page<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last,
        boolean first
) {
}