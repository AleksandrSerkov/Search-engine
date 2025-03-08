package searchengine.entity;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "page")
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "siteid", nullable = false)
    private Site site;

    @Column(name = "path", nullable = false, columnDefinition = "TEXT")
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "title")  // Добавлено поле title
    private String title;

    @ManyToMany
    @JoinTable(
        name = "idx", 
        joinColumns = @JoinColumn(name = "page_id"), 
        inverseJoinColumns = @JoinColumn(name = "lemma_id")
    )
    private Set<Lemma> lemmas;  // Связь с леммами

    // Constructors, getters, and setters

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public Site getSite() {
        return site;
    }
    
    public void setSite(Site site) {
        this.site = site;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;  // Метод для получения title
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Set<Lemma> getLemmas() {
        return lemmas;
    }

    public void setLemmas(Set<Lemma> lemmas) {
        this.lemmas = lemmas;
    }
}
