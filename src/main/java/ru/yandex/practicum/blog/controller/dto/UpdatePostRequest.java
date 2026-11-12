package ru.yandex.practicum.blog.controller.dto;

import java.util.List;

public record UpdatePostRequest(Long id, String title, String text, List<String> tags) {
}
