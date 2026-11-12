package ru.yandex.practicum.blog.controller.dto;

import java.util.List;

public record PostSummaryResponse(
        long id,
        String title,
        String text,
        List<String> tags,
        long likesCount,
        long commentsCount
) {
}
