package searchengine.init;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;

import searchengine.config.SitesList;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final SitesList sitesList;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(SitesList sitesList, JdbcTemplate jdbcTemplate) {
        this.sitesList = sitesList;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        logger.info("🚀 DataInitializer запущен! Начинаем инициализацию данных...");

        // Проверяем количество записей в таблице site
        long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM site", Long.class);
        logger.info("📊 В таблице site найдено {} записей.", count);

        if (count > 0) {
            logger.info("✅ Таблица site уже содержит данные. Инициализация не требуется.");
            return;
        }

        // Проверяем список сайтов в YAML
        List<searchengine.config.Site> configSites = sitesList.getSites();
        if (configSites == null || configSites.isEmpty()) {
            logger.warn("⚠️ Список сайтов в конфигурации пуст. Завершаем инициализацию.");
            return;
        }

        logger.info("🔄 Загружаем сайты из конфигурации...");
        configSites.forEach(configSite ->
            logger.info("🌍 Сайт: Name='{}', URL='{}'", configSite.getName(), configSite.getUrl())
        );

        // Создаем SimpleJdbcInsert для таблицы site
        SimpleJdbcInsert jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("site")
                .usingGeneratedKeyColumns("id");

        // Сохраняем каждый сайт напрямую в БД
        for (searchengine.config.Site configSite : configSites) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("url", configSite.getUrl());
            parameters.put("name", configSite.getName());
            parameters.put("status", "INDEXING"); // Используйте строковое представление статуса
            parameters.put("status_time", new Timestamp(System.currentTimeMillis()));
            parameters.put("last_error", "");

            try {
                Number id = jdbcInsert.executeAndReturnKey(parameters);
                logger.info("✅ Сайт успешно сохранен: ID={}, Name='{}', URL='{}'",
                        id, configSite.getName(), configSite.getUrl());
            } catch (Exception e) {
                logger.error("❌ Ошибка при сохранении сайта {}: {}", configSite.getUrl(), e.getMessage(), e);
            }
        }

        logger.info("✅ Инициализация данных завершена.");
    }
}

