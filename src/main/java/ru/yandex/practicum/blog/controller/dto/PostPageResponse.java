package ru.yandex.practicum.blog.controller.dto;

import java.util.List;

public record PostPageResponse(
        List<PostSummaryResponse> posts,
        boolean hasPrev,
        boolean hasNext,
        long lastPage
) {
}
