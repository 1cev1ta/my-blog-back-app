package ru.yandex.practicum.blog.dao;

import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.model.PostSearchCriteria;

import java.util.List;

public interface PostQueryDao {

    List<Post> findPage(PostSearchCriteria criteria, long offset, int limit);

    long count(PostSearchCriteria criteria);

    boolean incrementLikes(long postId);

    Long findLikesCount(long postId);
}
