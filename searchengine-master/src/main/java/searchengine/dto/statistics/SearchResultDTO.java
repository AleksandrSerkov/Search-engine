package searchengine.dto.statistics;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import searchengine.model.SearchResult;

/**
 * Data Transfer Object (DTO) для результатов поиска.
 * Адаптирован под JavaScript-код фронтенда.
 */
public class SearchResultDTO {

    @JsonProperty("site") // Базовый URL
    private String site;

    @JsonProperty("uri") // Относительный путь
    private String uri;

    @JsonProperty("title")
    private String title;

    @JsonProperty("snippet")
    private String snippet;

    @JsonProperty("relevance")
    private double relevance;

    @JsonProperty("siteName")
    private String siteName;

    @JsonProperty("fileName")
    private String fileName;

    // Конструктор без параметров (для JSON-сериализации)
    public SearchResultDTO() {
        initializeDefaults();
    }

    // Конструктор, принимающий SearchResult
    public SearchResultDTO(SearchResult searchResult) {
        if (searchResult != null) {
            this.site = extractBaseUrl(searchResult.getUri());
            this.uri = extractRelativeUrl(searchResult.getUri());
            this.title = Objects.requireNonNullElse(searchResult.getTitle(), "Без названия");
            this.snippet = Objects.requireNonNullElse(searchResult.getSnippet(), "");
            this.siteName = Objects.requireNonNullElse(searchResult.getSiteName(), "Неизвестный сайт");
            this.fileName = Objects.requireNonNullElse(searchResult.getFileName(), "");
            this.relevance = 0.0; // По умолчанию
        } else {
            initializeDefaults();
        }
    }

    public SearchResultDTO(String baseUrl, String relativeUri, String title, String snippet, double relevance, String siteName, String fileName) {
        this.site = baseUrl;
        this.uri = relativeUri;
        this.title = Objects.requireNonNullElse(title, "Без названия");
        this.snippet = Objects.requireNonNullElse(snippet, "");
        this.relevance = relevance;
        this.siteName = Objects.requireNonNullElse(siteName, "Неизвестный сайт");
        this.fileName = Objects.requireNonNullElse(fileName, "");
    }

    private void initializeDefaults() {
        this.site = "";
        this.uri = "";
        this.title = "Без названия";
        this.snippet = "";
        this.relevance = 0.0;
        this.siteName = "Неизвестный сайт";
        this.fileName = "";
    }

    // Метод для получения базового URL (например, https://www.lenta.ru)
    private String extractBaseUrl(String path) {
        try {
            java.net.URL url = new java.net.URL(path);
            return url.getProtocol() + "://" + url.getHost();
        } catch (Exception e) {
            return path; // Если ошибка, возвращаем оригинальный путь
        }
    }

    // Метод для получения относительного пути (например, /news/2024)
    private String extractRelativeUrl(String path) {
        try {
            java.net.URL url = new java.net.URL(path);
            return url.getPath() + (url.getQuery() != null ? "?" + url.getQuery() : "");
        } catch (Exception e) {
            return ""; // Если ошибка, оставляем пустым
        }
    }

    public String getSite() {
        return site;
    }

    public String getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public double getRelevance() {
        return relevance;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResultDTO that = (SearchResultDTO) o;
        return Double.compare(that.relevance, relevance) == 0 &&
               site.equals(that.site) &&
               uri.equals(that.uri) &&
               title.equals(that.title) &&
               snippet.equals(that.snippet) &&
               siteName.equals(that.siteName) &&
               fileName.equals(that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, uri, title, snippet, relevance, siteName, fileName);
    }

    @Override
    public String toString() {
        return "SearchResultDTO{" +
               "site='" + site + '\'' +
               ", uri='" + uri + '\'' +
               ", title='" + title + '\'' +
               ", snippet='" + snippet + '\'' +
               ", relevance=" + relevance +
               ", siteName='" + siteName + '\'' +
               ", fileName='" + fileName + '\'' +
               '}';
    }
}

