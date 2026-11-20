package ru.yandex.practicum.blog.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JdbcPostTagDao implements PostTagDao {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPostTagDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<Long, List<String>> findByPostIds(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<String>> tagsByPostId = new LinkedHashMap<>();
        jdbc.query(
                "SELECT post_id, tag FROM post_tags"
                        + " WHERE post_id IN (:postIds)"
                        + " ORDER BY post_id, tag",
                Map.of("postIds", postIds),
                rs -> {
                    long postId = rs.getLong("post_id");
                    tagsByPostId.computeIfAbsent(postId, key -> new ArrayList<>())
                            .add(rs.getString("tag"));
                }
        );
        return tagsByPostId;
    }

    @Override
    public List<String> findByPostId(long postId) {
        return findByPostIds(List.of(postId)).getOrDefault(postId, List.of());
    }

    @Override
    public void replaceTags(long postId, List<String> tags) {
        jdbc.update("DELETE FROM post_tags WHERE post_id = :postId", Map.of("postId", postId));

        if (tags == null || tags.isEmpty()) {
            return;
        }

        SqlParameterSource[] batch = tags.stream()
                .map(tag -> new MapSqlParameterSource()
                        .addValue("postId", postId)
                        .addValue("tag", tag))
                .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate(
                "INSERT INTO post_tags (post_id, tag) VALUES (:postId, :tag)",
                batch
        );
    }
}
