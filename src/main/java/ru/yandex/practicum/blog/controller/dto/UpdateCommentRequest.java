package ru.yandex.practicum.blog.controller.dto;

public record UpdateCommentRequest(Long id, String text, Long postId) {
}
