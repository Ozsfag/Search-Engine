package searchengine.utils.connectivity;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import searchengine.config.ConnectionSettings;
import searchengine.dto.indexing.responseImpl.ConnectionResponse;

import java.io.IOException;
import java.util.Optional;

/**
 * a util that parses a page
 * @author Ozsfag
 */
@Component
public class Connection {
    @Autowired
    ConnectionSettings connectionSettings;

    /**
     * get connection to url
     * @param url, link that needs to be parsed
     * @return ConnectionResponse
     */
    public ConnectionResponse getConnectionResponse(String url) {
        try {
            org.jsoup.Connection connection = Jsoup.connect(url)
                    .userAgent(connectionSettings.getUserAgent())
                    .referrer(connectionSettings.getReferrer());

            Document document = connection.get();
            int responseCode = connection.response().statusCode();
            String content = Optional.of(document.body().text()).orElseThrow();
            Elements urls = document.select("a[href]");

            return new ConnectionResponse(url, responseCode, content, urls, "");
        } catch (HttpStatusException e) {
            return getErrorConnectionResponse(url, e.getStatusCode(),e.getMessage());
        } catch (IOException e) {
            return getErrorConnectionResponse(url, HttpStatus.NOT_FOUND.value(), "Not found");
        }
    }

    /**
     *
     * @param url, link that needs to find title
     * @return title of page
     */
    public String getTitle(String url){
        org.jsoup.Connection connection = Jsoup.connect(url)
                .userAgent(connectionSettings.getUserAgent())
                .referrer(connectionSettings.getReferrer());

        Document document;
        try {
            document = connection.get();
        } catch (IOException e) {
            return "";
        }
        return document.select("title").text();
    }

    private ConnectionResponse getErrorConnectionResponse(String url, int statusCode, String errorMessage) {
        return new ConnectionResponse(url, statusCode,"", new Elements().empty(), errorMessage);
    }
}
