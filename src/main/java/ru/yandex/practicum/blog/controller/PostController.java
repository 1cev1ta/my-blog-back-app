package ru.yandex.practicum.blog.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.yandex.practicum.blog.controller.dto.CreatePostRequest;
import ru.yandex.practicum.blog.controller.dto.PostPageResponse;
import ru.yandex.practicum.blog.controller.dto.PostResponse;
import ru.yandex.practicum.blog.controller.dto.PostSummaryResponse;
import ru.yandex.practicum.blog.controller.dto.UpdatePostRequest;
import ru.yandex.practicum.blog.exception.BadRequestException;
import ru.yandex.practicum.blog.model.PageResult;
import ru.yandex.practicum.blog.model.PostData;
import ru.yandex.practicum.blog.model.PostDetails;
import ru.yandex.practicum.blog.model.PostImage;
import ru.yandex.practicum.blog.model.PostSummary;
import ru.yandex.practicum.blog.service.PostImageService;
import ru.yandex.practicum.blog.service.PostService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final PostImageService postImageService;

    public PostController(PostService postService, PostImageService postImageService) {
        this.postService = postService;
        this.postImageService = postImageService;
    }

    @GetMapping
    public PostPageResponse getPosts(@RequestParam(defaultValue = "") String search,
                                     @RequestParam(defaultValue = "1") int pageNumber,
                                     @RequestParam(defaultValue = "5") int pageSize) {
        PageResult<PostSummary> page = postService.getPosts(search, pageNumber, pageSize);
        List<PostSummaryResponse> posts = page.items().stream()
                .map(PostController::toSummaryResponse)
                .toList();
        return new PostPageResponse(posts, page.hasPrev(), page.hasNext(), page.lastPage());
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestBody CreatePostRequest request) {
        PostDetails created = postService.createPost(new PostData(
                request.title(),
                request.text(),
                request.tags()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PostMapping("/{id}")
    public PostResponse getPost(@PathVariable long id) {
        return toResponse(postService.getPost(id));
    }

    @PutMapping("/{id}")
    public PostResponse updatePost(@PathVariable long id,
                                   @RequestBody UpdatePostRequest request) {
        if (request.id() == null || request.id() != id) {
            throw new BadRequestException("Field 'id' must match the post id in the path");
        }
        PostDetails updated = postService.updatePost(id, new PostData(
                request.title(),
                request.text(),
                request.tags()
        ));
        return toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable long id) {
        postService.deletePost(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/likes")
    public long addLike(@PathVariable long id) {
        return postService.addLike(id);
    }

    @PutMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadImage(@PathVariable long id,
                                            @RequestPart("image") MultipartFile image) {
        try {
            postImageService.saveImage(
                    id,
                    image.getOriginalFilename(),
                    image.getContentType(),
                    image.getBytes()
            );
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read the uploaded image");
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable long id) {
        PostImage image = postImageService.getImage(id);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(image.contentType()))
                .contentLength(image.data().length)
                .body(image.data());
    }

    private static MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static PostSummaryResponse toSummaryResponse(PostSummary post) {
        return new PostSummaryResponse(
                post.id(),
                post.title(),
                post.text(),
                post.tags(),
                post.likesCount(),
                post.commentsCount()
        );
    }

    private static PostResponse toResponse(PostDetails post) {
        return new PostResponse(
                post.id(),
                post.title(),
                post.text(),
                post.tags(),
                post.likesCount(),
                post.commentsCount()
        );
    }
}
