package ru.yandex.practicum.blog.dao;

import org.springframework.data.repository.ListCrudRepository;
import ru.yandex.practicum.blog.model.Post;

public interface PostRepository extends ListCrudRepository<Post, Long> {
}
