package ru.yandex.practicum.blog.service;

import ru.yandex.practicum.blog.model.PostImage;

public interface PostImageService {

    void saveImage(long postId, String filename, String contentType, byte[] data);

    PostImage getImage(long postId);
}
