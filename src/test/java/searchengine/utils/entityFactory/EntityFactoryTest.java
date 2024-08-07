package searchengine.utils.entityFactory;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import searchengine.config.ConnectionSettings;
import searchengine.dto.indexing.Site;
import searchengine.model.*;
import searchengine.utils.scraper.WebScraper;

public class EntityFactoryTest {
  private EntityFactory factory;

  @BeforeEach
  public void setup() {
    ConnectionSettings connectionSettings =
        new ConnectionSettings("EmelianSearchBot", "https://www.google.com", 5000);
    WebScraper webScraper = new WebScraper(connectionSettings);
    factory = new EntityFactory(webScraper);
  }

  @Test
  public void testCreateSiteModel() {
    Site site = new Site("http://example.com", "Example Site");
    SiteModel expected =
        SiteModel.builder()
            .status(Status.INDEXING)
            .url(site.getUrl())
            .statusTime(new Date())
            .lastError("")
            .name(site.getName())
            .build();
    SiteModel actual = factory.createSiteModel(site);
    assertAll(
        () -> assertEquals(expected.getStatus(), actual.getStatus()),
        () -> assertEquals(expected.getUrl(), actual.getUrl()),
        () -> assertEquals(expected.getStatusTime(), actual.getStatusTime()),
        () -> assertEquals(expected.getLastError(), actual.getLastError()),
        () -> assertEquals(expected.getName(), actual.getName()));
  }

  @Test
  public void testCreatePageModel() {
    // Arrange
    SiteModel siteModel = new SiteModel();
    String path = "/index.html";
    PageModel expected =
        PageModel.builder().site(siteModel).path(path).code(404).content("").build();

    // Act
    PageModel actual = factory.createPageModel(siteModel, path);

    // Assert
    assertAll(
        () -> assertEquals(expected.getSite(), actual.getSite()),
        () -> assertEquals(expected.getPath(), actual.getPath()),
        () -> assertEquals(expected.getCode(), actual.getCode()),
        () -> assertEquals(expected.getVersion(), actual.getVersion()),
        () -> assertEquals(expected.getId(), actual.getId()),
        () -> assertEquals(expected.getContent(), actual.getContent()));
  }

  @Test
  public void testCreateIndexModel() {
    // Arrange
    PageModel pageModel = new PageModel();
    LemmaModel lemmaModel = new LemmaModel();
    float ranking = 0.5f;

    IndexModel expected =
        IndexModel.builder().page(pageModel).lemma(lemmaModel).rank(ranking).build();

    // Act
    IndexModel actual = factory.createIndexModel(pageModel, lemmaModel, ranking);

    // Assert
    assertAll(
        () -> assertEquals(expected.getPage(), actual.getPage()),
        () -> assertEquals(expected.getLemma(), actual.getLemma()),
        () -> assertEquals(expected.getRank(), actual.getRank()),
        () -> assertEquals(expected.getVersion(), actual.getVersion()),
        () -> assertEquals(expected.getId(), actual.getId()));
  }

  @Test
  public void testCreateLemmaModel() {
    // Arrange
    SiteModel siteModel = new SiteModel();
    String lemma = "example lemma";
    int frequency = 5;

    LemmaModel excepted =
        LemmaModel.builder().site(siteModel).lemma(lemma).frequency(frequency).build();

    // Act
    LemmaModel actual = factory.createLemmaModel(siteModel, lemma, frequency);

    // Assert
    assertAll(
        () -> assertEquals(excepted.getLemma(), actual.getLemma()),
        () -> assertEquals(excepted.getFrequency(), actual.getFrequency()),
        () -> assertEquals(excepted.getSite(), actual.getSite()),
        () -> assertEquals(excepted.getVersion(), actual.getVersion()),
        () -> assertEquals(excepted.getId(), actual.getId()));
  }
}
