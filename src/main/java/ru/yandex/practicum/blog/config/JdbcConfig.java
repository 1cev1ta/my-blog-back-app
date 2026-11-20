package ru.yandex.practicum.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import java.util.Collection;
import java.util.List;

@Configuration
public class JdbcConfig extends AbstractJdbcConfiguration {

    @Override
    protected Collection<String> getMappingBasePackages() {
        return List.of("ru.yandex.practicum.blog.model");
    }
}
