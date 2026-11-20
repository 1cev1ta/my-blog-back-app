package ru.yandex.practicum.blog.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.blog.model.PostImage;

import java.util.Optional;

@Repository
public class JdbcPostImageDao implements PostImageDao {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPostImageDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(PostImage image) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", image.postId())
                .addValue("filename", image.filename())
                .addValue("contentType", image.contentType())
                .addValue("data", image.data());

        int updated = jdbc.update(
                "UPDATE post_images"
                        + " SET filename = :filename, content_type = :contentType, data = :data"
                        + " WHERE post_id = :postId",
                params
        );

        if (updated == 0) {
            jdbc.update(
                    "INSERT INTO post_images (post_id, filename, content_type, data)"
                            + " VALUES (:postId, :filename, :contentType, :data)",
                    params
            );
        }
    }

    @Override
    public Optional<PostImage> findByPostId(long postId) {
        return jdbc.query(
                "SELECT post_id, filename, content_type, data"
                        + " FROM post_images WHERE post_id = :postId",
                new MapSqlParameterSource("postId", postId),
                rs -> {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new PostImage(
                            rs.getLong("post_id"),
                            rs.getString("filename"),
                            rs.getString("content_type"),
                            rs.getBytes("data")
                    ));
                }
        );
    }
}
