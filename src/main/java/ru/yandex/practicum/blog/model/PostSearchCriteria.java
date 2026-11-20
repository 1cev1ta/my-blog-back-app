package ru.yandex.practicum.blog.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record PostSearchCriteria(String titleSubstring, List<String> tags) {

    public PostSearchCriteria {
        tags = List.copyOf(tags);
    }

    public static PostSearchCriteria parse(String rawSearch) {
        if (rawSearch == null || rawSearch.isBlank()) {
            return new PostSearchCriteria("", List.of());
        }

        String title = "";
        Set<String> tags = new LinkedHashSet<>();
        List<String> titleWords = new ArrayList<>();

        for (String word : rawSearch.trim().split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (word.startsWith("#")) {
                String tag = word.substring(1).trim();
                if (!tag.isEmpty()) {
                    tags.add(tag.toLowerCase(Locale.ROOT));
                }
            } else {
                titleWords.add(word);
            }
        }

        if (!titleWords.isEmpty()) {
            title = String.join(" ", titleWords);
        }
        return new PostSearchCriteria(title, List.copyOf(tags));
    }

    public boolean isEmpty() {
        return titleSubstring.isBlank() && tags.isEmpty();
    }
}
