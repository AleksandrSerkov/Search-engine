package searchengine.entity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(exclude = "id")
@Entity
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "siteid", nullable = false)
    private Site site;

    @Column(name = "lemma_text", nullable = false)
    private String lemmaText;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    public Lemma() {
    }

    public Lemma(Site site, String lemmaText) {
        this.site = site;
        this.lemmaText = lemmaText;
        this.frequency = 1;
    }

    // Геттер и сеттер для поля lemmaText
    public String getLemmaText() {
        return lemmaText;
    }

    public void setLemmaText(String lemmaText) {
        this.lemmaText = lemmaText;
    }

    // Метод для увеличения значения frequency
    public void incrementFrequency() {
        this.frequency++;
    }

}

