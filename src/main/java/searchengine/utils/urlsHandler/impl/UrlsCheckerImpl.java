package searchengine.utils.urlsHandler.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.*;
import org.springframework.stereotype.Component;
import searchengine.dto.indexing.ConnectionResponse;
import searchengine.model.SiteModel;
import searchengine.repositories.PageRepository;
import searchengine.utils.urlsHandler.UrlsChecker;
import searchengine.utils.urlsHandler.urlsValidator.UrlValidator;
import searchengine.utils.webScraper.WebScraper;

@Component
@RequiredArgsConstructor
public class UrlsCheckerImpl implements UrlsChecker {
  private final WebScraper webScraper;
  private final PageRepository pageRepository;
  private final UrlValidator urlValidator;

  @Override
  public Collection<String> getCheckedUrls(String href, SiteModel siteModel) {

    Collection<String> urlsToCheck = fetchUrlsToCheck(href);

    if (urlsToCheck == null) {
      return Collections.EMPTY_LIST;
    }

    Stream<String> filteredUrls = filterOutAlreadyParsedUrls(urlsToCheck, siteModel);
    return filteredUrls
        .filter(url -> urlValidator.isValidUrl(url, siteModel.getUrl()))
        .collect(Collectors.toSet());
  }

  private Collection<String> fetchUrlsToCheck(String href) {
    ConnectionResponse connectionResponse = webScraper.getConnectionResponse(href);
    return connectionResponse.getUrls();
  }

  private Stream<String> filterOutAlreadyParsedUrls(
      Collection<String> scrappedUrls, SiteModel siteModel) {
    Collection<String> alreadyParsedUrls = findAlreadyParsedUrls(scrappedUrls, siteModel.getId());
    return scrappedUrls.stream().filter(url -> !alreadyParsedUrls.contains(url));
  }

  private Collection<String> findAlreadyParsedUrls(Collection<String> urls, int id) {
    return pageRepository.findAllPathsBySiteAndPathIn(id, new ArrayList<>(urls));
  }
}