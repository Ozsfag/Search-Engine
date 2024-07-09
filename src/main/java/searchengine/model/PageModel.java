package searchengine.model;

import lombok.*;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.BootstrapMode;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

@Entity
@Table( name = "pages", schema = "search_engine",
        indexes = {
        @Index(name = "path_index", columnList = "path", unique = true),
        @Index(name = "site_id_index", columnList = "site_id")})
@Getter
@Setter
@Builder
@AllArgsConstructor
@ToString
@NoArgsConstructor
@EqualsAndHashCode
public class PageModel implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "page_id", columnDefinition = "INT")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    @ToString.Exclude
    private SiteModel site;


    @Column(name = "path", nullable = false, columnDefinition = "TEXT", unique = true)
    private String path;


    @Column(nullable = false, columnDefinition = "INT")
    private int code;


    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<IndexModel> indexModels;
}
