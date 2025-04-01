package searchengine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Сущность для таблицы индексов (idx).
 * Обратите внимание, что поле для ранга переименовано в "rank_value"
 * чтобы избежать использования зарезервированного слова "rank" в MySQL.
 */
@Entity
@Table(name = "idx")
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "page_id", nullable = false)
    private Integer pageId;

    @Column(name = "lemma_id", nullable = false)
    private Integer lemmaId;

    @Column(name = "lemma", nullable = false)
    private String lemma;

    @Column(name = "idx", nullable = false)
    private Integer idx;
    
    // Переименовано поле rank в rank_value для избежания конфликтов с зарезервированными словами MySQL.
    @Column(name = "rank_value")
    private float rank;

    // Геттеры и сеттеры

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPageId() {
        return pageId;
    }

    public void setPageId(Integer pageId) {
        this.pageId = pageId;
    }

    public Integer getLemmaId() {
        return lemmaId;
    }

    public void setLemmaId(Integer lemmaId) {
        this.lemmaId = lemmaId;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public Integer getIdx() {
        return idx;
    }

    public void setIdx(Integer idx) {
        this.idx = idx;
    }
    
    // Геттер и сеттер для rank (ранга)
    public float getRank() {
        return rank;
    }

    public void setRank(float rank) {
        this.rank = rank;
    }
}

