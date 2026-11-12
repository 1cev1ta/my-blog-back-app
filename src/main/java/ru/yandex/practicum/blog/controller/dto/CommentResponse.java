package ru.yandex.practicum.blog.controller.dto;

public record CommentResponse(Long id, String text, Long postId) {
}
