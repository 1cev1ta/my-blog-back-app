package ru.yandex.practicum.blog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.practicum.blog.config.RootConfig;
import ru.yandex.practicum.blog.config.WebConfig;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(classes = {RootConfig.class, WebConfig.class})
@Sql(scripts = "/clean.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BlogApiMvcTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void supportsFullPostLifecycle() throws Exception {
        long id = createPost("Original title", "Original text", List.of("java", "spring"));

        mockMvc.perform(post("/api/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) id))
                .andExpect(jsonPath("$.title").value("Original title"))
                .andExpect(jsonPath("$.text").value("Original text"))
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.likesCount").value(0))
                .andExpect(jsonPath("$.commentsCount").value(0));

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "id", id,
                "title", "Updated title",
                "text", "Updated text",
                "tags", List.of("kotlin")
        ));

        mockMvc.perform(put("/api/posts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.text").value("Updated text"))
                .andExpect(jsonPath("$.tags[0]").value("kotlin"));

        mockMvc.perform(delete("/api/posts/{id}", id))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/posts/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void returnsFilteredAndPaginatedFeed() throws Exception {
        createPost("Spring Boot and Java", "First text", List.of("java", "spring"));
        createPost("Spring Framework", "Second text", List.of("java"));
        createPost("Cooking", "Third text", List.of("food"));

        mockMvc.perform(get("/api/posts")
                        .param("search", "spring")
                        .param("pageNumber", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.posts[0].title").value("Spring Framework"))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.lastPage").value(2));

        mockMvc.perform(get("/api/posts")
                        .param("search", "spring")
                        .param("pageNumber", "2")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[0].title").value("Spring Boot and Java"))
                .andExpect(jsonPath("$.hasPrev").value(true))
                .andExpect(jsonPath("$.hasNext").value(false));

        mockMvc.perform(get("/api/posts")
                        .param("search", "#java #spring")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.posts[0].title").value("Spring Boot and Java"));

        mockMvc.perform(get("/api/posts")
                        .param("search", "#food")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.posts[0].title").value("Cooking"));
    }

    @Test
    void truncatesTextInTheFeedButNotOnThePostPage() throws Exception {
        String longText = "a".repeat(200);
        long id = createPost("Long post", longText, List.of());

        MvcResult feed = mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn();

        String feedText = objectMapper.readTree(feed.getResponse().getContentAsByteArray())
                .path("posts").get(0).path("text").asText();
        assertEquals(129, feedText.codePointCount(0, feedText.length()));
        assertTrue(feedText.endsWith("…"));

        mockMvc.perform(post("/api/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(longText));
    }

    @Test
    void incrementsLikesAndReturnsTheNewCounter() throws Exception {
        long id = createPost("Likes", "Text", List.of());

        mockMvc.perform(post("/api/posts/{id}/likes", id))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        mockMvc.perform(post("/api/posts/{id}/likes", id))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));

        mockMvc.perform(post("/api/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(2));
    }

    @Test
    void uploadsReplacesAndDownloadsPostImage() throws Exception {
        long id = createPost("With image", "Text", List.of());
        byte[] firstImage = new byte[]{1, 2, 3, 4};
        byte[] secondImage = new byte[]{5, 6, 7};

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/posts/{id}/image", id)
                        .file(new MockMultipartFile("image", "picture.png", "image/png", firstImage)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{id}/image", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(firstImage));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/posts/{id}/image", id)
                        .file(new MockMultipartFile("image", "picture.jpg", "image/jpeg", secondImage)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{id}/image", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(secondImage));
    }

    @Test
    void supportsCommentLifecycle() throws Exception {
        long postId = createPost("Post with comments", "Text", List.of());

        MvcResult createResult = mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "text", "First comment",
                                "postId", postId
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("First comment"))
                .andExpect(jsonPath("$.postId").value((int) postId))
                .andReturn();

        long commentId = objectMapper.readTree(createResult.getResponse().getContentAsByteArray())
                .get("id").asLong();

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((int) commentId));

        mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("First comment"));

        mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", postId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", commentId,
                                "text", "Updated comment",
                                "postId", postId
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated comment"));

        mockMvc.perform(post("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentsCount").value(1));

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        long postId = createPost("Post", "Text", List.of());

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"text\":\"Text\",\"tags\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "1")
                        .param("pageSize", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/posts")
                        .param("search", "text")
                        .param("pageNumber", "not-a-number")
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/posts/{id}", postId + 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", postId,
                                "title", "Title",
                                "text", "Text",
                                "tags", List.of()
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "text", "Comment",
                                "postId", postId + 1
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/posts/{id}", 999_999))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/unknown-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void allowsCorsPreflightFromTheFrontend() throws Exception {
        mockMvc.perform(options("/api/posts")
                        .with(backendOnPort8080())
                        .header(HttpHeaders.ORIGIN, "http://localhost")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost"))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));

        mockMvc.perform(options("/api/posts")
                        .with(backendOnPort8080())
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5173"))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
    }

    private static RequestPostProcessor backendOnPort8080() {
        return request -> {
            request.setServerName("localhost");
            request.setServerPort(8080);
            return request;
        };
    }

    private long createPost(String title, String text, List<String> tags) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "text", text,
                "tags", tags
        ));

        MvcResult result = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .get("id").asLong();
    }
}
