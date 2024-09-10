package nl.nielsvanbruggen.webScraper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "imdb")
public class ImdbProperties {
    private String baseUrl;
    private int searchLimit;
}
