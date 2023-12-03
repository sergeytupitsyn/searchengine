package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "lemma")
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @JoinColumn(name = "site_id", nullable = false)
    @ManyToOne(cascade = CascadeType.MERGE)
    private Website website;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    public Lemma() {
    }

    public Lemma(Website website, String lemma, int frequency) {
        this.website = website;
        this.lemma = lemma;
        this.frequency = frequency;
    }
}
