package searchengine.factory;

import java.util.Date;
import org.springframework.stereotype.Component;
import searchengine.dto.indexing.ConnectionResponse;
import searchengine.dto.indexing.Site;
import searchengine.model.*;
import searchengine.utils.webScraper.WebScraper;

@Component
public class EntityFactory {
  private final WebScraper webScraper;

  public EntityFactory(WebScraper webScraper) {
    this.webScraper = webScraper;
  }

  /**
   * Creates a new SiteModel object with the provided site information.
   *
   * @param site the Site object to create the SiteModel from
   * @return the newly created SiteModel object
   */
  public SiteModel createSiteModel(Site site) {
    return SiteModel.builder()
        .status(Status.INDEXING)
        .url(site.getUrl())
        .statusTime(new Date())
        .lastError("")
        .name(site.getName())
        .build();
  }

  /**
   * Creates a new PageModel object with the provided siteModel and path.
   *
   * @param siteModel the SiteModel for the PageModel
   * @param path the path of the page
   * @return the newly created PageModel object
   */
  public PageModel createPageModel(SiteModel siteModel, String path) {
    ConnectionResponse connectionResponse = webScraper.getConnectionResponse(path);
    return PageModel.builder()
        .site(siteModel)
        .path(path)
        .code(connectionResponse.getResponseCode())
        .content(connectionResponse.getContent())
        .build();
  }

  /**
   * Creates a new LemmaModel object with the provided siteModel, lemma, and frequency.
   *
   * @param siteModel the SiteModel for the LemmaModel
   * @param lemma the lemma for the LemmaModel
   * @param frequency the frequency for the LemmaModel
   * @return the newly created LemmaModel object
   */
  public LemmaModel createLemmaModel(SiteModel siteModel, String lemma, int frequency) {
    return LemmaModel.builder().site(siteModel).lemma(lemma).frequency(frequency).build();
  }

  /**
   * Creates an IndexModel object with the given PageModel, LemmaModel, and ranking.
   *
   * @param pageModel the PageModel to associate with the IndexModel
   * @param lemmaModel the LemmaModel to associate with the IndexModel
   * @param ranking the ranking value to associate with the IndexModel
   * @return the newly created IndexModel object
   */
  public IndexModel createIndexModel(PageModel pageModel, LemmaModel lemmaModel, Float ranking) {
    return IndexModel.builder().page(pageModel).lemma(lemmaModel).rank(ranking).build();
  }
}
