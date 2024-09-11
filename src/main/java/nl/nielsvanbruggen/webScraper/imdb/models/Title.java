package nl.nielsvanbruggen.webScraper.imdb.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.nielsvanbruggen.webScraper.imdb.exceptions.ImdbScrapeException;
import nl.nielsvanbruggen.webScraper.imdb.scrapers.ImdbScraper;
import nl.nielsvanbruggen.webScraper.imdb.utils.ImdbUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Title {
    @JsonIgnore
    private final ImdbScraper imdbScraper;
    @JsonIgnore
    private final Map<String, Mono<Name>> nameLookup = new ConcurrentHashMap<>();

    private String imdbId;
    private Double imdbRating;
    private Long imdbRatingsAmount;
    private String title;
    private Integer releaseYear;
    private List<String> genres;
    private String description;
    private List<Name> directors;
    private List<Name> writers;
    private List<Name> creators;
    private List<Name> stars;
    private List<Name> cast;

    public Title(String imdbId, WebElement webElement, ImdbScraper scraper) throws ImdbScrapeException {
        this.imdbId = imdbId;
        this.imdbScraper = scraper;

        try {
            this.imdbRating = webElement.findElements(By.cssSelector("div[data-testid='hero-rating-bar__aggregate-rating__score'] span")).stream()
                    .findFirst()
                    .map(WebElement::getText)
                    .map(Double::parseDouble)
                    .orElse(null);
            this.imdbRatingsAmount = webElement.findElements(By.xpath("//div[div[@data-testid='hero-rating-bar__aggregate-rating__score']]/div[last()]")).stream()
                    .findFirst()
                    .map(WebElement::getText)
                    .map(this::parseImdbRatingsAmount)
                    .orElse(null);
            this.title = webElement.findElement(By.cssSelector("span[data-testid='hero__primary-text']")).getText();
            this.releaseYear = Integer.parseInt(webElement.findElement(By.cssSelector("a[href*='releaseinfo']")).getText().replaceAll("[^0-9*]", ""));
            this.genres = webElement.findElements(By.cssSelector("div[data-testid='interests'] a")).stream()
                    .map(WebElement::getText)
                    .toList();
            this.description = webElement.findElements(By.cssSelector("p[data-testid='plot'] span")).stream()
                    .map(WebElement::getText)
                    .filter(s -> !s.isBlank())
                    .findFirst()
                    .orElse("");

            // Scrape name information.
            WebElement persons = webElement.findElement(By.xpath("//ul[contains(@class, 'title-pc-list')]"));
            Mono.zip(
                    getDirectors(persons).collectList(),
                    getWriters(persons).collectList(),
                    getCreators(persons).collectList(),
                    getStars(persons).collectList(),
                    scraper.scrapeFullCast(imdbId).collectList())
                    .doOnNext(data -> {
                        this.directors = data.getT1();
                        this.writers = data.getT2();
                        this.creators = data.getT3();
                        this.stars = data.getT4();
                        this.cast = data.getT5();
                    })
                    .block();
        } catch (Exception e) {
            throw new ImdbScrapeException(e);
        }
    }

    private Flux<Name> getDirectors(WebElement webElement) {
        return getNames(webElement, "Director");
    }

    private Flux<Name> getWriters(WebElement webElement) {
        return getNames(webElement, "Writer");
    }

    private Flux<Name> getCreators(WebElement webElement) {
        return getNames(webElement, "Creator");
    }

    private Flux<Name> getStars(WebElement webElement) {
        return getNames(webElement, "Star");
    }

    private Flux<Name> getNames(WebElement webElement, String field) {
        return Flux.fromIterable(webElement.findElements(By.xpath(String.format("//li[@data-testid='title-pc-principal-credit' and .//*[contains(text(), '%s')]]//a", field))).stream()
                        .map(element -> element.getAttribute("href"))
                        .map(ImdbUtils::getNameIdFromUrl)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(id -> nameLookup.computeIfAbsent(id, imdbScraper::scrapeName))
                .sequential();
    }

    private Long parseImdbRatingsAmount(String ratingsAmount) {
        String prefix = ratingsAmount.replaceAll("[^a-zA-Z]", "").toLowerCase();

        Long multiplier = switch (prefix) {
            case "m" -> 1_000_000L;
            case "k" -> 1_000L;
            default -> 1L;
        };

        return (long) (Double.parseDouble(ratingsAmount.replaceAll("[^0-9]", "")) * multiplier);
    }
}
