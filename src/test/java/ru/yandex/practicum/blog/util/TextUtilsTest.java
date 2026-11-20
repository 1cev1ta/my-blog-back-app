package ru.yandex.practicum.blog.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextUtilsTest {

    @Test
    void returnsTextWithoutChangesWhenItIsShortEnough() {
        String text = "short text";

        assertEquals(text, TextUtils.truncateForFeed(text));
    }

    @Test
    void returnsTextWithoutChangesWhenItIsExactly128CharactersLong() {
        String text = "a".repeat(128);

        assertEquals(text, TextUtils.truncateForFeed(text));
    }

    @Test
    void truncatesLongTextTo128CharactersAndAddsEllipsis() {
        String text = "a".repeat(200);

        String truncated = TextUtils.truncateForFeed(text);

        assertEquals(129, truncated.codePointCount(0, truncated.length()));
        assertEquals("a".repeat(128) + "…", truncated);
    }

    @Test
    void doesNotSplitSurrogatePairs() {
        String text = "😀".repeat(200);

        String truncated = TextUtils.truncateForFeed(text);

        assertEquals(129, truncated.codePointCount(0, truncated.length()));
    }

    @Test
    void handlesNull() {
        assertNull(TextUtils.truncateForFeed(null));
    }
}
