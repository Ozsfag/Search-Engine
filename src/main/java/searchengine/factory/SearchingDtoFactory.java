package searchengine.factory;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import searchengine.dto.searching.responseImpl.DetailedSearchResponse;
import searchengine.model.IndexModel;
import searchengine.model.PageModel;
import searchengine.repositories.PageRepository;
import searchengine.utils.dataTransformer.DataTransformer;
import searchengine.utils.searching.snippetTransmitter.SnippetTransmitter;
import searchengine.utils.webScraper.WebScraper;

@Component
@Lazy
public class SearchingDtoFactory {
  private final ReentrantReadWriteLock lock;
  private final PageRepository pageRepository;
  private final DataTransformer dataTransformer;
  private final WebScraper webScraper;
  private final SnippetTransmitter snippetTransmitter;

  public SearchingDtoFactory(
      ReentrantReadWriteLock lock,
      PageRepository pageRepository,
      DataTransformer dataTransformer,
      WebScraper webScraper,
      SnippetTransmitter snippetTransmitter) {
    this.lock = lock;
    this.pageRepository = pageRepository;
    this.dataTransformer = dataTransformer;
    this.webScraper = webScraper;
    this.snippetTransmitter = snippetTransmitter;
  }

  public DetailedSearchResponse getDetailedSearchResponse(
      Map.Entry<Integer, Float> entry, Collection<IndexModel> uniqueSet) {
    PageModel pageModel = getPageModel(entry.getKey());
    String[] urlComponents = getUrlComponents(pageModel);
    String siteName = pageModel.getSite().getName();
    double relevance = entry.getValue();
    String title = webScraper.getConnectionResponse(pageModel.getPath()).getTitle();
    String snippet = snippetTransmitter.getSnippet(uniqueSet, pageModel);

    return DetailedSearchResponse.builder()
        .uri(urlComponents[1])
        .site(urlComponents[0])
        .title(title)
        .snippet(snippet)
        .siteName(siteName)
        .relevance(relevance)
        .build();
  }

  private PageModel getPageModel(Integer pageId) {
    try {
      lock.readLock().lock();
      return pageRepository.findById(pageId).orElseThrow();
    } finally {
      lock.readLock().unlock();
    }
  }

  private String[] getUrlComponents(PageModel pageModel) {
    try {
      return dataTransformer.getValidUrlComponents(pageModel.getPath());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e.getLocalizedMessage());
    }
  }
}