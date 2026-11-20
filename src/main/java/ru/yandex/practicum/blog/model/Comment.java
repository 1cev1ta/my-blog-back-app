package ru.yandex.practicum.blog.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

@Table("comments")
public class Comment {

    @Id
    private Long id;

    @Column("post_id")
    private Long postId;

    private String text;

    public Comment() {
    }

    public Comment(Long id, Long postId, String text) {
        this.id = id;
        this.postId = postId;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Comment comment)) {
            return false;
        }
        return id != null && Objects.equals(id, comment.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }
}
