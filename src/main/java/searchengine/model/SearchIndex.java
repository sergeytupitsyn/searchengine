package searchengine.model;

import lombok.Getter;
import lombok.Setter;


import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "'index'")
public class SearchIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @JoinColumn(name = "page_id", nullable = false)
    @ManyToOne(cascade = CascadeType.MERGE)
    private Page page;

    @JoinColumn(name = "lemma_id", nullable = false)
    @ManyToOne(cascade = CascadeType.MERGE)
    private Lemma lemma;

    @Column(name = "'rank'", nullable = false)
    private float rank;
}
