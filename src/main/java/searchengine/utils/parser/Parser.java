package searchengine.utils.parser;

import java.util.*;
import java.util.concurrent.RecursiveTask;
import lombok.RequiredArgsConstructor;
import searchengine.model.SiteModel;
import searchengine.utils.entityHandler.EntityHandler;
import searchengine.utils.scraper.WebScraper;

/**
 * Recursively index page and it`s subpage.
 *
 * @author Ozsfag
 */
@RequiredArgsConstructor
public class Parser extends RecursiveTask<Boolean> {
  private final EntityHandler entityHandler;
  private final WebScraper webScraper;
  private final SiteModel siteModel;
  private final String href;

  /**
   * Recursively computes the parsing of URLs and initiates subtasks for each URL to be parsed.
   *
   * @return null
   */
  @Override
  protected Boolean compute() {
    Collection<String> urlsToParse = webScraper.getUrlsToParse(siteModel, href);
    if (!urlsToParse.isEmpty()) {
      entityHandler.processIndexing(urlsToParse, siteModel);
      Collection<Parser> subtasks =
          urlsToParse.parallelStream()
              .map(url -> new Parser(entityHandler, webScraper, siteModel, url))
              .toList();
      invokeAll(subtasks);
    }
    return true;
  }
}
