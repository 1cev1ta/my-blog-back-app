package ru.yandex.practicum.blog.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.practicum.blog.config.RootConfig;
import ru.yandex.practicum.blog.model.Comment;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.model.PostImage;
import ru.yandex.practicum.blog.model.PostSearchCriteria;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(RootConfig.class)
@Sql(scripts = "/clean.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PostQueryDaoIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostTagDao postTagDao;

    @Autowired
    private PostQueryDao postQueryDao;

    @Autowired
    private PostImageDao postImageDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findsPostsByTitleSubstringAndTagsWithAndLogic() {
        Post first = savePost("Spring Boot and Java", "First text");
        postTagDao.replaceTags(first.getId(), List.of("java", "spring"));

        Post second = savePost("Spring Framework", "Second text");
        postTagDao.replaceTags(second.getId(), List.of("java"));

        Post third = savePost("Cooking", "Third text");
        postTagDao.replaceTags(third.getId(), List.of("food"));

        PostSearchCriteria criteria = PostSearchCriteria.parse("spring #java");
        List<Post> posts = postQueryDao.findPage(criteria, 0, 10);

        assertEquals(Set.of(first.getId(), second.getId()),
                posts.stream().map(Post::getId).collect(java.util.stream.Collectors.toSet()));
        assertEquals(2, postQueryDao.count(criteria));

        PostSearchCriteria onlySpringTag = PostSearchCriteria.parse("#spring");
        assertEquals(List.of(first.getId()),
                postQueryDao.findPage(onlySpringTag, 0, 10).stream().map(Post::getId).toList());
    }

    @Test
    void escapesLikeWildcardsInTheSearchString() {
        savePost("100% real", "Text");
        savePost("100 percent real", "Text");
        savePost("under_score", "Text");
        savePost("underXscore", "Text");

        assertEquals(1, postQueryDao.count(PostSearchCriteria.parse("100%")));
        assertEquals(2, postQueryDao.count(PostSearchCriteria.parse("100")));
        assertEquals(1, postQueryDao.count(PostSearchCriteria.parse("under_score")));
        assertEquals(2, postQueryDao.count(PostSearchCriteria.parse("under")));

        List<Post> result = postQueryDao.findPage(PostSearchCriteria.parse("100%"), 0, 10);
        assertEquals("100% real", result.get(0).getTitle());
    }

    @Test
    void returnsCommentsCountAndSupportsPagination() {
        Post first = savePost("First", "Text");
        savePost("Second", "Text");
        savePost("Third", "Text");

        commentRepository.save(new Comment(null, first.getId(), "One"));
        commentRepository.save(new Comment(null, first.getId(), "Two"));

        List<Post> firstPage = postQueryDao.findPage(PostSearchCriteria.parse(""), 0, 2);
        assertEquals(2, firstPage.size());
        assertEquals("Third", firstPage.get(0).getTitle());
        assertEquals("Second", firstPage.get(1).getTitle());

        List<Post> lastPage = postQueryDao.findPage(PostSearchCriteria.parse(""), 2, 2);
        assertEquals(1, lastPage.size());
        assertEquals("First", lastPage.get(0).getTitle());
        assertEquals(2, lastPage.get(0).getCommentsCount());
    }

    @Test
    void incrementsLikesAtomically() {
        Post post = savePost("Post", "Text");

        assertTrue(postQueryDao.incrementLikes(post.getId()));
        assertTrue(postQueryDao.incrementLikes(post.getId()));
        assertEquals(2L, postQueryDao.findLikesCount(post.getId()));

        assertFalse(postQueryDao.incrementLikes(999_999));
        assertNull(postQueryDao.findLikesCount(999_999));
    }

    @Test
    void replacesTagsInsteadOfAppendingThem() {
        Post post = savePost("Post", "Text");

        postTagDao.replaceTags(post.getId(), List.of("spring", "java"));
        assertEquals(List.of("java", "spring"), postTagDao.findByPostId(post.getId()));

        postTagDao.replaceTags(post.getId(), List.of("kotlin"));
        assertEquals(List.of("kotlin"), postTagDao.findByPostId(post.getId()));

        Map<Long, List<String>> tags = postTagDao.findByPostIds(List.of(post.getId()));
        assertEquals(List.of("kotlin"), tags.get(post.getId()));
    }

    @Test
    void upsertsPostImage() {
        Post post = savePost("Post", "Text");
        byte[] firstImage = new byte[]{1, 2, 3};
        byte[] secondImage = new byte[]{4, 5, 6, 7};

        assertTrue(postImageDao.findByPostId(post.getId()).isEmpty());

        postImageDao.save(new PostImage(post.getId(), "first.png", "image/png", firstImage));
        PostImage saved = postImageDao.findByPostId(post.getId()).orElseThrow();
        assertEquals("first.png", saved.filename());
        assertEquals("image/png", saved.contentType());
        assertArrayEquals(firstImage, saved.data());

        postImageDao.save(new PostImage(post.getId(), "second.jpg", "image/jpeg", secondImage));
        PostImage updated = postImageDao.findByPostId(post.getId()).orElseThrow();
        assertEquals("second.jpg", updated.filename());
        assertEquals("image/jpeg", updated.contentType());
        assertArrayEquals(secondImage, updated.data());
        assertEquals(1L, countImages(post.getId()));
    }

    private Post savePost(String title, String text) {
        return postRepository.save(new Post(null, title, text, 0));
    }

    private long countImages(long postId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM post_images WHERE post_id = ?",
                Long.class,
                postId
        );
        return count == null ? 0L : count;
    }
}
