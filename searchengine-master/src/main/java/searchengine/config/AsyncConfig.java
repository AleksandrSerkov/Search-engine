package searchengine.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableAsync
@EnableTransactionManagement
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);  // Начальный размер пула
        executor.setMaxPoolSize(10);  // Максимальный размер пула
        executor.setQueueCapacity(100);  // Размер очереди
        executor.setThreadNamePrefix("AsyncThread-");  // Префикс для имен потоков
        executor.setWaitForTasksToCompleteOnShutdown(true);  // Ожидание завершения задач при завершении приложения
        executor.setKeepAliveSeconds(120);  // Время жизни бездействующих потоков
        executor.initialize();
        return executor;
    }
}

