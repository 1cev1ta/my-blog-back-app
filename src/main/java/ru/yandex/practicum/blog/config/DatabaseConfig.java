package ru.yandex.practicum.blog.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean(destroyMethod = "close")
    public DataSource dataSource(Environment environment) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("blog-pool");
        config.setJdbcUrl(environment.getProperty("blog.db.url", "jdbc:postgresql://localhost:5432/blog"));
        config.setUsername(environment.getProperty("blog.db.username", "blog"));
        config.setPassword(environment.getProperty("blog.db.password", "blog"));
        config.setDriverClassName(environment.getProperty("blog.db.driver", "org.postgresql.Driver"));
        config.setMaximumPoolSize(environment.getProperty("blog.db.pool-size", Integer.class, 10));
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding("UTF-8");
        populator.setContinueOnError(false);
        populator.addScript(new ClassPathResource("schema.sql"));

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
