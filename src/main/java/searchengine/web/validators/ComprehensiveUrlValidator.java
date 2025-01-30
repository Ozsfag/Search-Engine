package searchengine.web.validators;

import org.springframework.beans.factory.annotation.Autowired;
import searchengine.config.MorphologySettings;
import searchengine.config.SitesList;
import searchengine.dto.indexing.Site;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public class ComprehensiveUrlValidator
    implements ConstraintValidator<searchengine.web.annotations.ComprehensiveUrlValidator, String> {

  @Autowired private SitesList sitesList;
  @Autowired private MorphologySettings morphologySettings;

  @Override
  public boolean isValid(String url, ConstraintValidatorContext context) {
    try {
      URI uri = new URI(url);
      String schemaAndHost = uri.getScheme() + "://" + uri.getHost() + "/";
      return isValidUrlBase(schemaAndHost)
          && isValidUrlEnding(url)
          && areUrlComponentsUnique(url)
          && isValidSchemas(url)
          && isExternalUrl(url);
    } catch (URISyntaxException e) {
      return false;
    }
  }

  private boolean isValidUrlBase(String url) {
    return sitesList.getSites().stream().map(Site::getUrl).anyMatch(url::startsWith);
  }

  private boolean isValidUrlEnding(String url) {
    return morphologySettings.getFormats().stream().noneMatch(url::contains);
  }

  private boolean isValidSchemas(String url) {
    return morphologySettings.getAllowedSchemas().stream().anyMatch(url::contains);
  }

  private boolean areUrlComponentsUnique(String url) {
    String[] urlSplit = url.split("/");
    return Arrays.stream(urlSplit).distinct().count() == urlSplit.length;
  }

  private boolean isExternalUrl(String url) {
    return sitesList.getSites().stream().map(Site::getUrl).noneMatch(url::equals);
  }
}
