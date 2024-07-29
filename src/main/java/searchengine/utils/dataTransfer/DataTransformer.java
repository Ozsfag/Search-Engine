package searchengine.utils.dataTransfer;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import searchengine.config.SitesList;
import searchengine.dto.indexing.Site;
import searchengine.exceptions.OutOfSitesConfigurationException;

import java.util.Collection;
import java.util.Collections;

@Component
@Data
@RequiredArgsConstructor
public class DataTransformer {
    private final SitesList sitesList;
    public Collection<String> transformUrlToUrls(String url){
        return Collections.singletonList(url);
    }

    public Site transformUrlToSite(String url) throws OutOfSitesConfigurationException {
        return sitesList.getSites().stream()
                .filter(siteUrl -> siteUrl.getUrl().equals(url))
                .findFirst()
                .orElseThrow(() -> new OutOfSitesConfigurationException("Site is not in configuration"));
    }

    @SneakyThrows
    public Collection<Site> transformUrlToSites(String url) {
        return transformUrlToUrls(url).stream().map(href -> {
            try {
                return transformUrlToSite(href);
            } catch (OutOfSitesConfigurationException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }
}
