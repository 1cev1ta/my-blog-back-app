package ru.yandex.practicum.blog.model;

public record PostImage(Long postId, String filename, String contentType, byte[] data) {
}
