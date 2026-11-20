package ru.yandex.practicum.blog.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.blog.dao.PostImageDao;
import ru.yandex.practicum.blog.dao.PostRepository;
import ru.yandex.practicum.blog.exception.BadRequestException;
import ru.yandex.practicum.blog.exception.NotFoundException;
import ru.yandex.practicum.blog.model.PostImage;
import ru.yandex.practicum.blog.service.PostImageService;

@Service
@Transactional(readOnly = true)
public class PostImageServiceImpl implements PostImageService {

    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_CONTENT_TYPE_LENGTH = 255;
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final PostImageDao postImageDao;
    private final PostRepository postRepository;

    public PostImageServiceImpl(PostImageDao postImageDao, PostRepository postRepository) {
        this.postImageDao = postImageDao;
        this.postRepository = postRepository;
    }

    @Override
    @Transactional
    public void saveImage(long postId, String filename, String contentType, byte[] data) {
        ensurePostExists(postId);
        if (data == null || data.length == 0) {
            throw new BadRequestException("Image must not be empty");
        }

        postImageDao.save(new PostImage(
                postId,
                normalizeFilename(filename),
                normalizeContentType(contentType),
                data
        ));
    }

    @Override
    public PostImage getImage(long postId) {
        ensurePostExists(postId);
        return postImageDao.findByPostId(postId)
                .orElseThrow(() -> new NotFoundException(
                        "Image for post " + postId + " was not found"
                ));
    }

    private void ensurePostExists(long postId) {
        if (!postRepository.existsById(postId)) {
            throw new NotFoundException("Post with id " + postId + " was not found");
        }
    }

    private String normalizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new BadRequestException("Field 'image' must be a file with a filename");
        }
        String trimmed = filename.trim();
        return trimmed.length() > MAX_FILENAME_LENGTH
                ? trimmed.substring(0, MAX_FILENAME_LENGTH)
                : trimmed;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_CONTENT_TYPE;
        }
        String trimmed = contentType.trim();
        return trimmed.length() > MAX_CONTENT_TYPE_LENGTH
                ? trimmed.substring(0, MAX_CONTENT_TYPE_LENGTH)
                : trimmed;
    }
}
