package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HtmlCleaner {

    private static final Logger logger = LoggerFactory.getLogger(HtmlCleaner.class);

    public String cleanHtml(String html) {
        logger.info("Начало очистки HTML контента");
        Document doc = Jsoup.parse(html);
        doc.select("script, style, iframe, noscript").remove();
        String cleanedText = doc.text();
        logger.info("Очистка HTML завершена, получен текст длиной: {} символов", cleanedText.length());
        return cleanedText;
    }
}

