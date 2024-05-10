package searchengine.config;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories; // Добавлен импорт

import jakarta.persistence.EntityManagerFactory;
import searchengine.logi.MyLogger;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@EntityScan(basePackages = "searchengine.entity")
@EnableJpaRepositories(basePackages = "searchengine.repository") // Добавляем @EnableJpaRepositories
public class DatabaseConfig {

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder,
                                                                       DataSourceProperties dataSourceProperties,
                                                                       JpaProperties jpaProperties) {
        HashMap<String, Object> properties = new HashMap<>();
        properties.putAll(jpaProperties.getProperties());

        return builder
                .dataSource(dataSourceProperties.initializeDataSourceBuilder().build())
                .packages("searchengine.entity")
                .properties(properties)
                .build();
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    public String message() {
        return "Your data here"; // Замени на свои данные
    }

}


