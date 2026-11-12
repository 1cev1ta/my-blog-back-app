package ru.yandex.practicum.blog.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.blog.dao.CommentRepository;
import ru.yandex.practicum.blog.dao.PostRepository;
import ru.yandex.practicum.blog.exception.BadRequestException;
import ru.yandex.practicum.blog.exception.NotFoundException;
import ru.yandex.practicum.blog.model.Comment;
import ru.yandex.practicum.blog.service.CommentService;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private static final int MAX_TEXT_LENGTH = 1_000_000;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentServiceImpl(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    @Override
    public List<Comment> getComments(long postId) {
        ensurePostExists(postId);
        return commentRepository.findByPostIdOrderByIdAsc(postId);
    }

    @Override
    public Comment getComment(long postId, long commentId) {
        ensurePostExists(postId);
        return findComment(postId, commentId);
    }

    @Override
    @Transactional
    public Comment createComment(long postId, String text) {
        ensurePostExists(postId);
        String validText = validateText(text);
        return commentRepository.save(new Comment(null, postId, validText));
    }

    @Override
    @Transactional
    public Comment updateComment(long postId, long commentId, String text) {
        ensurePostExists(postId);
        Comment comment = findComment(postId, commentId);
        comment.setText(validateText(text));
        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteComment(long postId, long commentId) {
        ensurePostExists(postId);
        Comment comment = findComment(postId, commentId);
        commentRepository.delete(comment);
    }

    private void ensurePostExists(long postId) {
        if (!postRepository.existsById(postId)) {
            throw new NotFoundException("Post with id " + postId + " was not found");
        }
    }

    private Comment findComment(long postId, long commentId) {
        return commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new NotFoundException(
                        "Comment with id " + commentId + " for post " + postId + " was not found"
                ));
    }

    private String validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new BadRequestException("Field 'text' is required");
        }
        String trimmed = text.trim();
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new BadRequestException("Field 'text' must not exceed " + MAX_TEXT_LENGTH + " characters");
        }
        return trimmed;
    }
}
