package searchengine.utils.searching.searchingDtoFactory.impl;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import searchengine.dto.searching.responseImpl.DetailedSearchResponse;
import searchengine.model.IndexModel;
import searchengine.model.PageModel;
import searchengine.repositories.PageRepository;
import searchengine.utils.searching.searchingDtoFactory.SearchingDtoFactory;
import searchengine.utils.searching.snippetTransmitter.SnippetTransmitter;
import searchengine.utils.validator.Validator;
import searchengine.utils.webScraper.WebScraper;

@Component
@Lazy
@RequiredArgsConstructor
public class SearchingDtoFactoryImpl implements SearchingDtoFactory {
  private final ReentrantReadWriteLock lock;
  private final PageRepository pageRepository;
  private final Validator validator;
  private final WebScraper webScraper;
  private final SnippetTransmitter snippetTransmitter;

  @Override
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
      return validator.getValidUrlComponents(pageModel.getPath());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e.getLocalizedMessage());
    }
  }
}
