package searchengine.utils.scraper;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import searchengine.config.ConnectionSettings;
import searchengine.dto.indexing.ConnectionDto;
import searchengine.model.SiteModel;
import searchengine.repositories.PageRepository;
import searchengine.utils.validator.Validator;

/**
 * a util that parses a page
 *
 * @author Ozsfag
 */
@Component
@Data
@RequiredArgsConstructor
public class WebScraper {
  private final ConnectionSettings connectionSettings;
  @Autowired private PageRepository pageRepository;
  @Autowired private Validator validator;

  /**
   * Retrieves a Jsoup Document object for the given URL by establishing a connection with it.
   *
   * @param url the URL to connect to
   * @return a Jsoup Document object representing the HTML content of the URL, or null if an
   *     IOException occurs
   */
  public Document getDocument(String url) {
    try {
      return Jsoup.connect(url)
          .userAgent(connectionSettings.getUserAgent())
          .referrer(connectionSettings.getReferrer())
          .ignoreHttpErrors(true)
          .timeout(connectionSettings.getTimeout())
          .get();
    } catch (IOException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  /**
   * Retrieves the connection response for the specified URL.
   *
   * @param url the URL to establish a connection with
   * @return the ConnectionResponse containing URL, HTTP status, content, URLs, and an empty string
   */
  public ConnectionDto getConnectionDto(String url) {
    try {
      Document document = getDocument(url);
      String content = Optional.of(document.body().text()).orElseThrow();
      Set<String> urls =
          document.select("a[href]").stream()
              .map(element -> element.absUrl("href"))
              .collect(Collectors.toSet());
      String title = document.select("title").text();

      return new ConnectionDto(url, HttpStatus.OK.value(), content, urls, "", title);
    } catch (Exception e) {
      return new ConnectionDto(
          url,
          HttpStatus.NOT_FOUND.value(),
          "",
          new HashSet<>(),
          HttpStatus.NOT_FOUND.getReasonPhrase(),
          "");
    }
  }

  /**
   * Retrieves a set of URLs to parse based on the provided list of all URLs by site.
   *
   * @return a set of URLs to parse
   */
  public synchronized Collection<String> getUrlsToParse(SiteModel siteModel, String href) {
    Collection<String> urls = getConnectionDto(href).getUrls();
    Collection<String> alreadyParsed =
        pageRepository.findAllPathsBySiteAndPathIn(siteModel.getId(), urls);
    urls.removeAll(alreadyParsed);

    return urls.parallelStream()
        .filter(url -> validator.urlHasCorrectForm(url, siteModel.getUrl()))
        .collect(Collectors.toSet());
  }
}
