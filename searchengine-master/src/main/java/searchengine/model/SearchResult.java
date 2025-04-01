package searchengine.model;

public class SearchResult {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private double relevance;
    private String fileName; // Новое поле для имени файла

    // Конструктор с параметрами, включая fileName
    public SearchResult(String site, String siteName, String uri, String title, String snippet, double relevance, String fileName) {
        this.site = site;
        this.siteName = siteName;
        this.uri = uri;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
        this.fileName = fileName;
        System.out.println("SearchResult создан с fileName = " + this.fileName);
    }
    
    // Перегруженный конструктор без fileName (по умолчанию fileName = "")
    public SearchResult(String site, String siteName, String uri, String title, String snippet, double relevance) {
        this(site, siteName, uri, title, snippet, relevance, "");
    }

    // Геттеры и сеттеры
    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public double getRelevance() {
        return relevance;
    }

    public void setRelevance(double relevance) {
        this.relevance = relevance;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
        System.out.println("setFileName вызван, значение fileName = " + this.fileName);
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "site='" + site + '\'' +
                ", siteName='" + siteName + '\'' +
                ", uri='" + uri + '\'' +
                ", title='" + title + '\'' +
                ", snippet='" + snippet + '\'' +
                ", relevance=" + relevance +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}

