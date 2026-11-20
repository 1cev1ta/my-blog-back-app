package ru.yandex.practicum.blog.model;

import java.util.List;

public record PostSummary(
        long id,
        String title,
        String text,
        List<String> tags,
        long likesCount,
        long commentsCount
) {
}
