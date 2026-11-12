package ru.yandex.practicum.blog.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.blog.controller.dto.CommentResponse;
import ru.yandex.practicum.blog.controller.dto.CreateCommentRequest;
import ru.yandex.practicum.blog.controller.dto.UpdateCommentRequest;
import ru.yandex.practicum.blog.exception.BadRequestException;
import ru.yandex.practicum.blog.model.Comment;
import ru.yandex.practicum.blog.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> getComments(@PathVariable long postId) {
        return commentService.getComments(postId).stream()
                .map(CommentController::toResponse)
                .toList();
    }

    @GetMapping("/{commentId}")
    public CommentResponse getComment(@PathVariable long postId,
                                      @PathVariable long commentId) {
        return toResponse(commentService.getComment(postId, commentId));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@PathVariable long postId,
                                                         @RequestBody CreateCommentRequest request) {
        if (request.postId() == null || request.postId() != postId) {
            throw new BadRequestException("Field 'postId' must match the post id in the path");
        }
        Comment created = commentService.createComment(postId, request.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{commentId}")
    public CommentResponse updateComment(@PathVariable long postId,
                                         @PathVariable long commentId,
                                         @RequestBody UpdateCommentRequest request) {
        if (request.id() == null || request.id() != commentId) {
            throw new BadRequestException("Field 'id' must match the comment id in the path");
        }
        if (request.postId() == null || request.postId() != postId) {
            throw new BadRequestException("Field 'postId' must match the post id in the path");
        }
        return toResponse(commentService.updateComment(postId, commentId, request.text()));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable long postId,
                                              @PathVariable long commentId) {
        commentService.deleteComment(postId, commentId);
        return ResponseEntity.ok().build();
    }

    private static CommentResponse toResponse(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getText(), comment.getPostId());
    }
}
