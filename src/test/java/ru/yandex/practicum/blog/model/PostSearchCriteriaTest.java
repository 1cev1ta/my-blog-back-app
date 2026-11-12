package ru.yandex.practicum.blog.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostSearchCriteriaTest {

    @Test
    void returnsEmptyCriteriaForNullOrBlankSearch() {
        assertTrue(PostSearchCriteria.parse(null).isEmpty());
        assertTrue(PostSearchCriteria.parse("   ").isEmpty());
    }

    @Test
    void joinsTitleWordsWithSingleSpaces() {
        PostSearchCriteria criteria = PostSearchCriteria.parse("  spring   framework  ");

        assertEquals("spring framework", criteria.titleSubstring());
        assertEquals(List.of(), criteria.tags());
    }

    @Test
    void extractsTagsAndLowercasesThem() {
        PostSearchCriteria criteria = PostSearchCriteria.parse("Spring #Java #Backend");

        assertEquals("Spring", criteria.titleSubstring());
        assertEquals(List.of("java", "backend"), criteria.tags());
    }

    @Test
    void removesDuplicateTagsAndEmptyTags() {
        PostSearchCriteria criteria = PostSearchCriteria.parse("#java #JAVA # #spring");

        assertEquals(List.of("java", "spring"), criteria.tags());
        assertTrue(criteria.titleSubstring().isEmpty());
    }

    @Test
    void keepsOnlyTagsWhenThereIsNoTitlePart() {
        PostSearchCriteria criteria = PostSearchCriteria.parse("#java");

        assertEquals("", criteria.titleSubstring());
        assertEquals(List.of("java"), criteria.tags());
    }
}
