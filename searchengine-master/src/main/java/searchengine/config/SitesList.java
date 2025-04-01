package searchengine.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesList {
    private static final Logger logger = LoggerFactory.getLogger(SitesList.class);

    private List<Site> sites;

    public List<Site> getSites() {
        return sites;
    }

    public void setSites(List<Site> sites) {
        this.sites = sites;
    }

    @PostConstruct
    public void logLoadedSites() {
        if (sites == null || sites.isEmpty()) {
            System.out.println("⚠️ YAML-файл пуст или не загружен!");
            logger.warn("⚠️ YAML-файл пуст или не загружен!");
        } else {
            System.out.println("🔄 Загружены сайты из YAML:");
            logger.info("🔄 Загружены сайты из YAML:");
            for (Site site : sites) {
                System.out.println("🌍 Сайт: " + site.getName() + " (" + site.getUrl() + ")");
                logger.info("🌍 Сайт: {} ({})", site.getName(), site.getUrl());
            }
        }
    }
}

