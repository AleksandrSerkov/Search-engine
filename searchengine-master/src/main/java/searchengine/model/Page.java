package searchengine.model;
import jakarta.persistence.*;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
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
    @Size(max = 255) // добавь эту аннотацию, если нужно ограничить максимальную длину
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    // Constructors, getters, and setters
}

