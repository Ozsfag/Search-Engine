package searchengine.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "connection-settings")
@NoArgsConstructor(force = true)
@Setter
public final class JsoupConnectionSettings {
  private String userAgent;
  private String referrer;
  @Getter private Integer timeout;

  public String getUserAgent() {
    return String.copyValueOf(userAgent.toCharArray());
  }

  public String getReferrer() {
    return String.copyValueOf(referrer.toCharArray());
  }
}
