package ru.yandex.practicum.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.blog.controller.dto.CreatePostRequest;
import ru.yandex.practicum.blog.controller.dto.PostResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EmbeddedServerTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void servesPostThroughEmbeddedTomcat() {
        ResponseEntity<PostResponse> created = rest.postForEntity(
                "/api/posts",
                new CreatePostRequest("Проверка сервера", "Текст с кириллицей", List.of("java")),
                PostResponse.class
        );
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        PostResponse post = created.getBody();
        assertNotNull(post);
        assertNotNull(post.id());
        String path = "/api/posts/" + post.id();

        try {
            ResponseEntity<PostResponse> byGet = rest.getForEntity(path, PostResponse.class);
            ResponseEntity<PostResponse> byPost = rest.postForEntity(path, null, PostResponse.class);
            assertEquals(HttpStatus.OK, byGet.getStatusCode());
            assertEquals(HttpStatus.OK, byPost.getStatusCode());
            assertEquals(post, byGet.getBody());
            assertEquals(post, byPost.getBody());
        } finally {
            rest.delete(path);
        }
    }
}
