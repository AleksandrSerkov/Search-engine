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

    @Column(name = "level", nullable = false)
    private Float rank;


// Конструкторы, геттеры и сеттеры
}
