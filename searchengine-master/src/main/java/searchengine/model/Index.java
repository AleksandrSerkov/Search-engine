package searchengine.model;
import jakarta.persistence.*;
import lombok.Data;
@Data
@Entity
@Table(name = "idx")
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Integer id;

    @Column(name = "page_id", nullable = false)
    private Integer pageId;

    @Column(name = "lemma_id", nullable = false)
    private Integer lemmaId;

    @Column(name = "lemma", nullable = false)
    private String lemma;

    @Column(name = "level", nullable = false)
    private Float rank;

    // Constructors, getters, and setters

    // Геттер и сеттер для поля lemma
    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    // Сеттер для pageId
    public void setPageId(Integer pageId) {
        this.pageId = pageId;
    }
}
