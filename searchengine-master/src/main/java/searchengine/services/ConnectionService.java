package searchengine.services;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);

    public Document connectToPage(String url) throws IOException {
        logger.info("Подключение к странице: {}", url);
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(30000) // увеличиваем таймаут до 30 секунд
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                               "AppleWebKit/537.36 (KHTML, like Gecko) " +
                               "Chrome/91.0.4472.124 Safari/537.36")
                    .get();
            logger.info("Успешное подключение к странице: {}", url);
            return doc;
        } catch (SocketTimeoutException e) {
            logger.error("Таймаут при подключении к странице {}: {}", url, e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.error("Ошибка подключения к странице {}: {}", url, e.getMessage());
            throw e;
        }
    }
}


