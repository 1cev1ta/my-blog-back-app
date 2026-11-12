package ru.yandex.practicum.blog.model;

import java.util.List;

public record PostDetails(
        long id,
        String title,
        String text,
        List<String> tags,
        long likesCount,
        long commentsCount
) {
}
