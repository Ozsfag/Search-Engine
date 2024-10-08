package searchengine.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true)
@Component
@ConfigurationProperties(prefix = "connection-settings")
public class JsoupConnectionSettings {
  public String userAgent;
  public String referrer;
  public int timeout;
}
