package ru.yandex.practicum.blog.util;

public final class TextUtils {

    public static final int FEED_TEXT_LIMIT = 128;

    private static final String ELLIPSIS = "…";

    private TextUtils() {
    }

    public static String truncateForFeed(String text) {
        return truncate(text, FEED_TEXT_LIMIT);
    }

    public static String truncate(String text, int maxCodePoints) {
        if (text == null) {
            return null;
        }
        if (maxCodePoints < 0) {
            throw new IllegalArgumentException("maxCodePoints must not be negative");
        }
        if (text.codePointCount(0, text.length()) <= maxCodePoints) {
            return text;
        }
        int endIndex = text.offsetByCodePoints(0, maxCodePoints);
        return text.substring(0, endIndex) + ELLIPSIS;
    }
}
