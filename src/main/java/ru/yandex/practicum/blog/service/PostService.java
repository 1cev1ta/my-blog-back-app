package ru.yandex.practicum.blog.service;

import ru.yandex.practicum.blog.model.PageResult;
import ru.yandex.practicum.blog.model.PostData;
import ru.yandex.practicum.blog.model.PostDetails;
import ru.yandex.practicum.blog.model.PostSummary;

public interface PostService {

    PageResult<PostSummary> getPosts(String search, int pageNumber, int pageSize);

    PostDetails getPost(long id);

    PostDetails createPost(PostData data);

    PostDetails updatePost(long id, PostData data);

    void deletePost(long id);

    long addLike(long id);
}
