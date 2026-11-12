package ru.yandex.practicum.blog.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.blog.dao.CommentRepository;
import ru.yandex.practicum.blog.dao.PostQueryDao;
import ru.yandex.practicum.blog.dao.PostRepository;
import ru.yandex.practicum.blog.dao.PostTagDao;
import ru.yandex.practicum.blog.exception.BadRequestException;
import ru.yandex.practicum.blog.exception.NotFoundException;
import ru.yandex.practicum.blog.model.PageResult;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.model.PostData;
import ru.yandex.practicum.blog.model.PostDetails;
import ru.yandex.practicum.blog.model.PostSearchCriteria;
import ru.yandex.practicum.blog.model.PostSummary;
import ru.yandex.practicum.blog.service.PostService;
import ru.yandex.practicum.blog.util.TextUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_TEXT_LENGTH = 1_000_000;
    private static final int MAX_TAG_LENGTH = 255;
    private static final int MAX_PAGE_SIZE = 100;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostQueryDao postQueryDao;
    private final PostTagDao postTagDao;

    public PostServiceImpl(PostRepository postRepository,
                           CommentRepository commentRepository,
                           PostQueryDao postQueryDao,
                           PostTagDao postTagDao) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postQueryDao = postQueryDao;
        this.postTagDao = postTagDao;
    }

    @Override
    public PageResult<PostSummary> getPosts(String search, int pageNumber, int pageSize) {
        validatePagination(pageNumber, pageSize);

        PostSearchCriteria criteria = PostSearchCriteria.parse(search);
        long total = postQueryDao.count(criteria);
        long lastPage = Math.max(1L, (total + pageSize - 1) / pageSize);
        long offset = (long) (pageNumber - 1) * pageSize;

        List<Post> posts;
        if (offset >= total) {
            posts = List.of();
        } else {
            posts = postQueryDao.findPage(criteria, offset, pageSize);
            attachTags(posts);
        }

        List<PostSummary> items = posts.stream()
                .map(this::toSummary)
                .toList();

        return new PageResult<>(
                items,
                pageNumber > 1,
                pageNumber < lastPage,
                lastPage
        );
    }

    @Override
    public PostDetails getPost(long id) {
        Post post = findPost(id);
        attachTags(List.of(post));
        post.setCommentsCount(commentRepository.countByPostId(id));
        return toDetails(post);
    }

    @Override
    @Transactional
    public PostDetails createPost(PostData data) {
        NormalizedPost normalized = normalize(data);

        Post saved = postRepository.save(new Post(
                null,
                normalized.title(),
                normalized.text(),
                0L
        ));
        postTagDao.replaceTags(saved.getId(), normalized.tags());

        return new PostDetails(
                saved.getId(),
                saved.getTitle(),
                saved.getText(),
                normalized.tags(),
                saved.getLikesCount(),
                0L
        );
    }

    @Override
    @Transactional
    public PostDetails updatePost(long id, PostData data) {
        Post post = findPost(id);
        NormalizedPost normalized = normalize(data);

        post.setTitle(normalized.title());
        post.setText(normalized.text());
        Post saved = postRepository.save(post);
        postTagDao.replaceTags(id, normalized.tags());

        return new PostDetails(
                saved.getId(),
                saved.getTitle(),
                saved.getText(),
                normalized.tags(),
                saved.getLikesCount(),
                commentRepository.countByPostId(id)
        );
    }

    @Override
    @Transactional
    public void deletePost(long id) {
        if (!postRepository.existsById(id)) {
            throw new NotFoundException("Post with id " + id + " was not found");
        }
        postRepository.deleteById(id);
    }

    @Override
    @Transactional
    public long addLike(long id) {
        if (!postQueryDao.incrementLikes(id)) {
            throw new NotFoundException("Post with id " + id + " was not found");
        }
        Long likesCount = postQueryDao.findLikesCount(id);
        if (likesCount == null) {
            throw new NotFoundException("Post with id " + id + " was not found");
        }
        return likesCount;
    }

    private Post findPost(long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Post with id " + id + " was not found"));
    }

    private void attachTags(List<Post> posts) {
        if (posts.isEmpty()) {
            return;
        }
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Map<Long, List<String>> tagsByPostId = postTagDao.findByPostIds(postIds);
        for (Post post : posts) {
            post.setTags(tagsByPostId.getOrDefault(post.getId(), List.of()));
        }
    }

    private PostSummary toSummary(Post post) {
        return new PostSummary(
                post.getId(),
                post.getTitle(),
                TextUtils.truncateForFeed(post.getText()),
                post.getTags(),
                post.getLikesCount(),
                post.getCommentsCount()
        );
    }

    private PostDetails toDetails(Post post) {
        return new PostDetails(
                post.getId(),
                post.getTitle(),
                post.getText(),
                post.getTags(),
                post.getLikesCount(),
                post.getCommentsCount()
        );
    }

    private void validatePagination(int pageNumber, int pageSize) {
        if (pageNumber < 1) {
            throw new BadRequestException("Parameter 'pageNumber' must be greater than or equal to 1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BadRequestException("Parameter 'pageSize' must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private NormalizedPost normalize(PostData data) {
        if (data == null) {
            throw new BadRequestException("Request body is required");
        }

        String title = data.title();
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Field 'title' is required");
        }
        title = title.trim();
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new BadRequestException("Field 'title' must not exceed " + MAX_TITLE_LENGTH + " characters");
        }

        String text = data.text();
        if (text == null) {
            throw new BadRequestException("Field 'text' is required");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new BadRequestException("Field 'text' must not exceed " + MAX_TEXT_LENGTH + " characters");
        }

        if (data.tags() == null) {
            throw new BadRequestException("Field 'tags' is required");
        }

        Set<String> uniqueTags = new LinkedHashSet<>();
        for (String rawTag : data.tags()) {
            if (rawTag == null) {
                continue;
            }
            String tag = rawTag.trim();
            while (tag.startsWith("#")) {
                tag = tag.substring(1).trim();
            }
            if (tag.isEmpty()) {
                continue;
            }
            if (tag.length() > MAX_TAG_LENGTH) {
                throw new BadRequestException("Each tag must not exceed " + MAX_TAG_LENGTH + " characters");
            }
            uniqueTags.add(tag);
        }

        return new NormalizedPost(title, text, List.copyOf(uniqueTags));
    }

    private record NormalizedPost(String title, String text, List<String> tags) {
    }
}
