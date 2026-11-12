package ru.yandex.practicum.blog.dao;

import org.springframework.data.repository.ListCrudRepository;
import ru.yandex.practicum.blog.model.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends ListCrudRepository<Comment, Long> {

    List<Comment> findByPostIdOrderByIdAsc(long postId);

    long countByPostId(long postId);

    Optional<Comment> findByIdAndPostId(long id, long postId);
}
