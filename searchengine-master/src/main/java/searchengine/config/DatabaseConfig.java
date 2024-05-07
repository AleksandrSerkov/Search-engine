package searchengine.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.annotation.sql.DataSourceDefinition;
import javax.sql.DataSource;

@DataSourceDefinition(
        name = "java:global/jdbc/MyDataSource",
        className = "com.mysql.cj.jdbc.Driver",
        url = "${spring.datasource.url}",
        user = "${spring.datasource.username}",
        password = "${spring.datasource.password}"
)
@Configuration
public class DatabaseConfig {
    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.url}")
    private String url;

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    // Другие бины, методы конфигурации и компоненты...
}
