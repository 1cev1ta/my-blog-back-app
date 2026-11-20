package ru.yandex.practicum.blog.dao;

import ru.yandex.practicum.blog.model.PostImage;

import java.util.Optional;

public interface PostImageDao {

    void save(PostImage image);

    Optional<PostImage> findByPostId(long postId);
}
