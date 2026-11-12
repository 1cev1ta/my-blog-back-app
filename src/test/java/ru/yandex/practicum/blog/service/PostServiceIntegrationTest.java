package ru.yandex.practicum.blog.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.practicum.blog.config.RootConfig;
import ru.yandex.practicum.blog.exception.BadRequestException;
import ru.yandex.practicum.blog.exception.NotFoundException;
import ru.yandex.practicum.blog.model.PageResult;
import ru.yandex.practicum.blog.model.PostData;
import ru.yandex.practicum.blog.model.PostDetails;
import ru.yandex.practicum.blog.model.PostSummary;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(RootConfig.class)
@Sql(scripts = "/clean.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PostServiceIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private PostImageService postImageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsPostWithNormalizedTagsAndLoadsItBack() {
        PostDetails created = postService.createPost(new PostData(
                "  My post  ",
                "Hello **world**",
                List.of("java", "#spring", "java")
        ));

        assertNotNull(created.id());
        assertEquals("My post", created.title());
        assertEquals("Hello **world**", created.text());
        assertEquals(Set.of("java", "spring"), Set.copyOf(created.tags()));
        assertEquals(0, created.likesCount());
        assertEquals(0, created.commentsCount());

        PostDetails loaded = postService.getPost(created.id());
        assertEquals(created.id(), loaded.id());
        assertEquals("My post", loaded.title());
        assertEquals("Hello **world**", loaded.text());
        assertEquals(Set.of("java", "spring"), Set.copyOf(loaded.tags()));
    }

    @Test
    void updatesPostAndReplacesTagsKeepingLikes() {
        PostDetails created = postService.createPost(new PostData(
                "Original",
                "Original text",
                List.of("java", "backend")
        ));
        postService.addLike(created.id());

        PostDetails updated = postService.updatePost(created.id(), new PostData(
                "Updated",
                "Updated text",
                List.of("kotlin", "backend")
        ));

        assertEquals("Updated", updated.title());
        assertEquals("Updated text", updated.text());
        assertEquals(1, updated.likesCount());
        assertEquals(Set.of("kotlin", "backend"), Set.copyOf(updated.tags()));

        PostDetails loaded = postService.getPost(created.id());
        assertEquals("Updated", loaded.title());
        assertEquals(1, loaded.likesCount());
        assertEquals(Set.of("kotlin", "backend"), Set.copyOf(loaded.tags()));
        assertEquals(2L, countRows("post_tags", created.id()));
    }

    @Test
    void deletesPostTogetherWithCommentsTagsAndImage() {
        PostDetails created = postService.createPost(new PostData(
                "To delete",
                "Text",
                List.of("java")
        ));
        commentService.createComment(created.id(), "First comment");
        postImageService.saveImage(created.id(), "picture.png", "image/png", new byte[]{1, 2, 3});

        postService.deletePost(created.id());

        assertThrows(NotFoundException.class, () -> postService.getPost(created.id()));
        assertEquals(0L, countRows("comments", created.id()));
        assertEquals(0L, countRows("post_tags", created.id()));
        assertEquals(0L, countRows("post_images", created.id()));
    }

    @Test
    void truncatesTextOnlyInTheFeed() {
        String longText = "a".repeat(200);
        PostDetails created = postService.createPost(new PostData(
                "Long post",
                longText,
                List.of()
        ));

        PageResult<PostSummary> page = postService.getPosts("", 1, 10);
        PostSummary summary = page.items().get(0);

        assertEquals(129, summary.text().codePointCount(0, summary.text().length()));
        assertTrue(summary.text().endsWith("…"));
        assertEquals(128, summary.text().substring(0, summary.text().length() - 1)
                .codePointCount(0, 128));

        assertEquals(longText, postService.getPost(created.id()).text());
    }

    @Test
    void searchesByTitleSubstringAndTagsWithAndLogic() {
        createPost("Spring Boot and Java", List.of("java", "spring"));
        createPost("Spring Framework", List.of("java"));
        createPost("Cooking", List.of("food"));
        createPost("Java Performance", List.of("java", "performance"));

        assertTitles("spring #java", "Spring Boot and Java", "Spring Framework");
        assertTitles("#java #spring", "Spring Boot and Java");
        assertTitles("boot #java", "Spring Boot and Java");
        assertTitles("BOOT #JAVA", "Spring Boot and Java");
        assertTitles("boot #food");
        assertTitles("#java", "Spring Boot and Java", "Spring Framework", "Java Performance");
        assertEquals(4, postService.getPosts("", 1, 10).items().size());
    }

    @Test
    void paginatesPostsFromNewestToOldest() {
        for (int i = 1; i <= 5; i++) {
            createPost("Post " + i, List.of());
        }

        PageResult<PostSummary> firstPage = postService.getPosts("", 1, 2);
        assertEquals(List.of("Post 5", "Post 4"),
                firstPage.items().stream().map(PostSummary::title).toList());
        assertFalse(firstPage.hasPrev());
        assertTrue(firstPage.hasNext());
        assertEquals(3, firstPage.lastPage());

        PageResult<PostSummary> lastPage = postService.getPosts("", 3, 2);
        assertEquals(List.of("Post 1"),
                lastPage.items().stream().map(PostSummary::title).toList());
        assertTrue(lastPage.hasPrev());
        assertFalse(lastPage.hasNext());
        assertEquals(3, lastPage.lastPage());
    }

    @Test
    void returnsEmptyPageWhenPageNumberIsOutOfRange() {
        createPost("Only post", List.of());

        PageResult<PostSummary> page = postService.getPosts("", 10, 5);

        assertTrue(page.items().isEmpty());
        assertTrue(page.hasPrev());
        assertFalse(page.hasNext());
        assertEquals(1, page.lastPage());
    }

    @Test
    void incrementsLikesAndReturnsTheNewValue() {
        PostDetails created = postService.createPost(new PostData("Likes", "Text", List.of()));

        assertEquals(1, postService.addLike(created.id()));
        assertEquals(2, postService.addLike(created.id()));
        assertEquals(2, postService.getPost(created.id()).likesCount());

        assertThrows(NotFoundException.class, () -> postService.addLike(999_999));
    }

    @Test
    void countsCommentsInPostResponses() {
        PostDetails created = postService.createPost(new PostData("Comments", "Text", List.of()));
        commentService.createComment(created.id(), "First");
        commentService.createComment(created.id(), "Second");

        assertEquals(2, postService.getPost(created.id()).commentsCount());
        assertEquals(2, postService.getPosts("", 1, 10).items().get(0).commentsCount());

        commentService.deleteComment(created.id(), commentService.getComments(created.id()).get(0).getId());
        assertEquals(1, postService.getPost(created.id()).commentsCount());
    }

    @Test
    void rejectsInvalidPostDataAndPagination() {
        assertThrows(BadRequestException.class,
                () -> postService.createPost(new PostData(" ", "Text", List.of())));
        assertThrows(BadRequestException.class,
                () -> postService.createPost(new PostData("Title", null, List.of())));
        assertThrows(BadRequestException.class,
                () -> postService.createPost(new PostData("Title", "Text", null)));
        assertThrows(BadRequestException.class,
                () -> postService.getPosts("", 0, 5));
        assertThrows(BadRequestException.class,
                () -> postService.getPosts("", 1, 0));
        assertThrows(NotFoundException.class,
                () -> postService.getPost(999_999));
    }

    private PostDetails createPost(String title, List<String> tags) {
        return postService.createPost(new PostData(title, "Text of " + title, tags));
    }

    private void assertTitles(String search, String... expectedTitles) {
        List<String> actual = postService.getPosts(search, 1, 10).items().stream()
                .map(PostSummary::title)
                .toList();
        assertEquals(Set.of(expectedTitles), Set.copyOf(actual),
                "Unexpected posts for search: " + search);
    }

    private long countRows(String table, long postId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE post_id = ?",
                Long.class,
                postId
        );
        return count == null ? 0L : count;
    }
}
