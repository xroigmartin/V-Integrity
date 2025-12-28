package xavierroigmartin.v_integrity.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for RestClient.
 * Ensures a RestClient.Builder bean is available for injection.
 */
@Configuration
public class RestClientConfig {

  @Bean
  public RestClient.Builder restClientBuilder() {
    return RestClient.builder();
  }
}
