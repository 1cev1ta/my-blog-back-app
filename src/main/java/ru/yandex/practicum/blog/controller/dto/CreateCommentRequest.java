package ru.yandex.practicum.blog.controller.dto;

public record CreateCommentRequest(String text, Long postId) {
}
