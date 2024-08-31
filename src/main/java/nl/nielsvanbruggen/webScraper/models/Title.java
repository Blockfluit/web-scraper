package nl.nielsvanbruggen.webScraper.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nl.nielsvanbruggen.webScraper.exceptions.ImdbScrapeException;
import nl.nielsvanbruggen.webScraper.scrapers.ImdbScraper;
import nl.nielsvanbruggen.webScraper.utils.ImdbUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Title {
    @JsonIgnore
    private final ImdbScraper imdbScraper;
    @JsonIgnore
    private final Map<String, Mono<Name>> nameLookup = new ConcurrentHashMap<>();

    private String imdbId;
    private String title;
    private Integer releaseYear;
    private List<String> genres;
    private String description;
    private List<Name> directors;
    private List<Name> writers;
    private List<Name> creators;
    private List<Name> stars;

    public Title(String imdbId, WebDriver driver, ImdbScraper scraper) throws ImdbScrapeException {
        this.imdbId = imdbId;
        this.imdbScraper = scraper;

        try {
            this.title = driver.findElement(By.cssSelector("span[data-testid='hero__primary-text']")).getText();
            this.releaseYear = Integer.parseInt(driver.findElement(By.cssSelector("a[href*='releaseinfo']")).getText().replaceAll("[^0-9*]", ""));
            this.genres = driver.findElements(By.cssSelector("div[data-testid='interests'] a")).stream()
                    .map(WebElement::getText)
                    .toList();
            this.description = driver.findElements(By.cssSelector("p[data-testid='plot'] span")).stream()
                    .map(WebElement::getText)
                    .filter(s -> !s.isBlank())
                    .findFirst()
                    .orElse("");

            Mono.zip(
                    getDirectors(driver).collectList(),
                    getWriters(driver).collectList(),
                    getCreators(driver).collectList(),
                    getStars(driver).collectList(),
                    scraper.scrapeFullCast(imdbId).collectList())
                    .doOnNext(data -> {
                        this.directors = data.getT1();
                        this.writers = data.getT2();
                        this.creators = data.getT3();
                        this.stars = data.getT4();
                    })
                    .block();
        } catch (Exception e) {
            throw new ImdbScrapeException(e);
        }
    }

    private Flux<Name> getDirectors(WebDriver driver) {
        return getNames(driver, "Director");
    }

    private Flux<Name> getWriters(WebDriver driver) {
        return getNames(driver, "Writer");
    }

    private Flux<Name> getCreators(WebDriver driver) {
        return getNames(driver, "Creator");
    }

    private Flux<Name> getStars(WebDriver driver) {
        return getNames(driver, "Star");
    }

    private Flux<Name> getNames(WebDriver driver, String field) {
        return Flux.fromIterable(driver.findElements(By.xpath(String.format("//li[.//a[contains(text(), '%s')]]//a", field))).stream()
                        .map(element -> element.getAttribute("href"))
                        .map(ImdbUtils::getIdFromUrl)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(id -> nameLookup.computeIfAbsent(id, imdbScraper::scrapeName))
                .sequential();
    }
}
