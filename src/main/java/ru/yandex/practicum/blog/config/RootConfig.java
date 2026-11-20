package ru.yandex.practicum.blog.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.ControllerAdvice;

@Configuration
@EnableTransactionManagement
@EnableJdbcRepositories(basePackages = "ru.yandex.practicum.blog.dao")
@ComponentScan(
        basePackages = "ru.yandex.practicum.blog",
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ANNOTATION,
                        classes = {Controller.class, ControllerAdvice.class}
                ),
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = WebConfig.class
                )
        }
)
@PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = true)
@Import({DatabaseConfig.class, JdbcConfig.class})
public class RootConfig {
}
