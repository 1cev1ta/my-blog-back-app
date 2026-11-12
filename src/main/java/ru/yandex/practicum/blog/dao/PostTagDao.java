package ru.yandex.practicum.blog.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PostTagDao {

    Map<Long, List<String>> findByPostIds(Collection<Long> postIds);

    List<String> findByPostId(long postId);

    void replaceTags(long postId, List<String> tags);
}
