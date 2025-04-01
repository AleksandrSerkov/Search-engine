package searchengine;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync; // Добавляем аннотацию для асинхронных операций
import org.springframework.transaction.annotation.EnableTransactionManagement;

@ConfigurationPropertiesScan
@PropertySource({"application.yml"})
@SpringBootApplication
@EntityScan(basePackages = "searchengine.entity")
@ComponentScan(basePackages = {
    "searchengine.services",
    "searchengine.controllers",
    "searchengine.config",
    "searchengine.init",
    "searchengine.repository" // Добавляем репозитории
})@EnableTransactionManagement
@EnableAsync // Включаем поддержку асинхронных методов
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
