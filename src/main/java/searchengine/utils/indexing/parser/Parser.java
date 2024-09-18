package searchengine.utils.indexing.parser;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import searchengine.model.SiteModel;
import searchengine.repositories.SiteRepository;
import searchengine.utils.indexing.IndexingStrategy;
import searchengine.utils.indexing.processor.taskFactory.TaskFactory;
import searchengine.utils.urlsHandler.UrlsChecker;

/**
 * Recursively indexes a page and its subpages.
 *
 * <p>This class uses a ForkJoin framework to parallelize the indexing process.
 * It checks URLs, processes indexing, updates site status, and creates subtasks for further indexing.
 * </p>
 *
 * @author Ozsfag
 */
@Component
@RequiredArgsConstructor
public class Parser extends RecursiveTask<Boolean> {
  private final UrlsChecker urlsChecker;
  private final IndexingStrategy indexingStrategy;
  private final SiteRepository siteRepository;
  private final TaskFactory taskFactory;

  private SiteModel siteModel;
  private String href;
  private Collection<String> urlsToParse;
  private Collection<ForkJoinTask<?>> subtasks;

  /**
   * Initializes the parser with the given site model and URL.
   *
   * @param siteModel the site model to be indexed
   * @param href the URL to start indexing from
   */
  public void init(SiteModel siteModel, String href) {
    this.siteModel = siteModel;
    this.href = href;
  }

  /**
   * Recursively computes the parsing of URLs and initiates subtasks for each URL to be parsed.
   *
   * @return true if the computation was successful, false otherwise
   */
  @Override
  protected Boolean compute() {
    setCheckingUrls();
    if (checkedUrlsIsNotEmpty()) {
      indexingUrls();
      updateSiteStatus(href);
      setSubtasks();
      invokeAll(subtasks);
    }
    return true;
  }

  private void setCheckingUrls() {
    urlsToParse = urlsChecker.getCheckedUrls(href, siteModel);
  }

  private boolean checkedUrlsIsNotEmpty() {
    return !urlsToParse.isEmpty();
  }

  private void indexingUrls() {
    indexingStrategy.processIndexing(urlsToParse, siteModel);
  }

  private void updateSiteStatus(String href) {
    siteRepository.updateStatusTimeByUrl(new Date(), href);
  }

  private void setSubtasks() {
    subtasks =
        urlsToParse.stream()
            .map(url -> taskFactory.initTask(siteModel, url))
            .collect(Collectors.toSet());
  }
}
