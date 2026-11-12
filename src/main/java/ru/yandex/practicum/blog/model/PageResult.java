package ru.yandex.practicum.blog.model;

import java.util.List;

public record PageResult<T>(
        List<T> items,
        boolean hasPrev,
        boolean hasNext,
        long lastPage
) {
    public PageResult {
        items = List.copyOf(items);
    }
}
