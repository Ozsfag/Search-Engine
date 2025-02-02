package searchengine.models;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Set;
import javax.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cascade;

@SuppressFBWarnings("EI_EXPOSE_REP")
@Entity
@Table(
    name = "pages",
    schema = "search_engine",
    indexes = {
      @Index(name = "path_index", columnList = "path", unique = true),
      @Index(name = "idx_site_id_path", columnList = "site_id, path", unique = true)
    })
@Getter
@Setter
@Builder
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class PageModel implements Serializable, EntityInterface {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "page_id", columnDefinition = "INT")
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "site_id", referencedColumnName = "site_id", nullable = false)
  @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
  @ToString.Exclude
  private SiteModel site;

  @Column(name = "path", nullable = false, columnDefinition = "TEXT", unique = true)
  private String path;

  @Column(nullable = false, columnDefinition = "INT")
  private Integer code;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<IndexModel> indexModels;
}
