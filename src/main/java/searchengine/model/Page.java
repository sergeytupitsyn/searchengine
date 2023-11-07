package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "page", indexes = {@Index(columnList = "path", name = "path_index")})
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @JoinColumn(name = "site_id", nullable = false)
    @ManyToOne(cascade = CascadeType.MERGE)
    private Website website;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    public Page() {
    }

    public Page(Website website, String path, int code, String content) {
        this.website = website;
        this.path = path;
        this.code = code;
        this.content = content;
    }
}
