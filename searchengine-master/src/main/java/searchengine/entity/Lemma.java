package searchengine.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(exclude = "id")
@Entity
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "siteid", nullable = true)
    private Site site;

    @Column(name = "lemma_text", nullable = false)
    private String lemmaText;

    @Column(name = "frequency")
    private Integer frequency;
    

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
    public void setId(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
    
    public void setSite(Site site) {
        this.site = site;
    }
    
    public Site getSite() {
        return site;
    }
    
  
    
    public void setFrequency(Integer frequency) {
        this.frequency = frequency;
    }
    
    public Integer getFrequency() {
        return frequency;
    }

}

