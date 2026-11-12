package ru.yandex.practicum.blog.dao;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.blog.model.Post;
import ru.yandex.practicum.blog.model.PostSearchCriteria;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class JdbcPostQueryDao implements PostQueryDao {

    private static final String SELECT_POSTS = """
            SELECT p.id,
                   p.title,
                   p.text,
                   p.likes_count,
                   (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) AS comments_count
            FROM posts p
            """;

    private static final RowMapper<Post> POST_ROW_MAPPER = (rs, rowNum) -> {
        Post post = new Post(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("text"),
                rs.getLong("likes_count")
        );
        post.setCommentsCount(rs.getLong("comments_count"));
        return post;
    };

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPostQueryDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Post> findPage(PostSearchCriteria criteria, long offset, int limit) {
        FilterClause filter = buildFilter(criteria);
        filter.params()
                .addValue("limit", limit)
                .addValue("offset", offset);

        String sql = SELECT_POSTS + filter.sql()
                + " ORDER BY p.created_at DESC, p.id DESC"
                + " LIMIT :limit OFFSET :offset";
        return jdbc.query(sql, filter.params(), POST_ROW_MAPPER);
    }

    @Override
    public long count(PostSearchCriteria criteria) {
        FilterClause filter = buildFilter(criteria);
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM posts p" + filter.sql(),
                filter.params(),
                Long.class
        );
        return count == null ? 0L : count;
    }

    @Override
    public boolean incrementLikes(long postId) {
        int updated = jdbc.update(
                "UPDATE posts SET likes_count = likes_count + 1 WHERE id = :postId",
                Map.of("postId", postId)
        );
        return updated > 0;
    }

    @Override
    public Long findLikesCount(long postId) {
        List<Long> result = jdbc.queryForList(
                "SELECT likes_count FROM posts WHERE id = :postId",
                Map.of("postId", postId),
                Long.class
        );
        return result.isEmpty() ? null : result.get(0);
    }

    private FilterClause buildFilter(PostSearchCriteria criteria) {
        StringBuilder sql = new StringBuilder(" WHERE 1 = 1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (criteria != null && !criteria.titleSubstring().isBlank()) {
            sql.append(" AND LOWER(p.title) LIKE :titlePattern ESCAPE '\\'");
            String pattern = "%" + escapeLike(criteria.titleSubstring().toLowerCase(Locale.ROOT)) + "%";
            params.addValue("titlePattern", pattern);
        }

        if (criteria != null) {
            int index = 0;
            for (String tag : criteria.tags()) {
                String parameter = "tag" + index++;
                sql.append(" AND EXISTS (SELECT 1 FROM post_tags pt")
                        .append(" WHERE pt.post_id = p.id AND LOWER(pt.tag) = :")
                        .append(parameter)
                        .append(")");
                params.addValue(parameter, tag.toLowerCase(Locale.ROOT));
            }
        }

        return new FilterClause(sql.toString(), params);
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private record FilterClause(String sql, MapSqlParameterSource params) {
    }
}
