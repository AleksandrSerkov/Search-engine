package searchengine.dto.statistics;
public class SiteStatistics {
    private String url;
    private String name;
    private String status;
    private String statusTime;
    private int pages;
    private int lemmas;
    private String error;

    public SiteStatistics() {
    }

    public SiteStatistics(String url, String name, String status, String statusTime, int pages, int lemmas, String error) {
        this.url = url;
        this.name = name;
        this.status = status;
        this.statusTime = statusTime;
        this.pages = pages;
        this.lemmas = lemmas;
        this.error = error;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusTime() {
        return statusTime;
    }

    public void setStatusTime(String statusTime) {
        this.statusTime = statusTime;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public int getLemmas() {
        return lemmas;
    }

    public void setLemmas(int lemmas) {
        this.lemmas = lemmas;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}