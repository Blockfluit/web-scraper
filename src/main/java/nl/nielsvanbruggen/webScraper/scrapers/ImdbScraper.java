package nl.nielsvanbruggen.webScraper.scrapers;

import lombok.extern.slf4j.Slf4j;
import nl.nielsvanbruggen.webScraper.config.ImdbProperties;
import nl.nielsvanbruggen.webScraper.models.Title;
import nl.nielsvanbruggen.webScraper.models.Name;
import nl.nielsvanbruggen.webScraper.exceptions.ImdbScrapeException;
import nl.nielsvanbruggen.webScraper.utils.ImdbUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static java.lang.String.format;

@Slf4j
@Service
public class ImdbScraper {
    private final ImdbProperties imdbProperties;
    private final ChromeOptions chromeOptions;

    public ImdbScraper(ImdbProperties imdbProperties, ChromeOptions chromeOptions) {
        this.imdbProperties = imdbProperties;
        this.chromeOptions = chromeOptions;
    }

    public Mono<Title> scrapeTitle(String id) throws ImdbScrapeException {
        String url = format("%stitle/%s/", imdbProperties.getBaseUrl(), id);

        WebDriver driver = new ChromeDriver(chromeOptions);

        return Mono.create(sink -> {
            log.info("Scraping title: {}", url);

            try {
                driver.get(url);

                // Wait for title to visible.
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("h1[data-testid='hero__pageTitle']")));

                sink.success(new Title(id, driver, this));
                log.debug("Finished scraping title: {}", url);
            } catch(ImdbScrapeException e) {
                log.error(e.getMessage(), e);
                sink.error(e);
            } catch (Exception e) {
                sink.error(e);
            } finally {
                driver.quit();
            }
        });
    }

    public Mono<Name> scrapeName(String id) {
        String url = format("%sname/%s/", imdbProperties.getBaseUrl(), id);

        WebDriver driver = new ChromeDriver(chromeOptions);

        return Mono.create(sink -> {
            log.info("Scraping name: {}", url);

            try {
                driver.get(url);

                // Wait for title to visible.
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("h1[data-testid='hero__pageTitle']")));

                sink.success(new Name(id, driver));
                log.debug("Finished scraping name: {}", url);
            } catch(ImdbScrapeException e) {
                log.error(e.getMessage(), e);
                sink.error(e);
            } catch (Exception e) {
                sink.error(e);
            } finally {
                driver.quit();
            }
        });
    }

    public Flux<Name> scrapeFullCast(String id) {
        String url = format("%stitle/%s/fullcredits", imdbProperties.getBaseUrl(), id);

        WebDriver driver = new ChromeDriver(chromeOptions);

        return Flux.create(sink -> {
            log.info("Scraping full cast: {}", url);

            try {
                driver.get(url);

                // Wait for title to visible.
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id='fullcredits_content']")));

                driver.findElement(By.className("cast_list"))
                        .findElements(By.xpath("//td[@class='primary_photo']/a")).stream()
                        .map(element -> {
                            String castId = ImdbUtils.getIdFromUrl(element.getAttribute("href"));
                            String[] fullName = ImdbUtils.parseName(element.findElement(By.tagName("img")).getAttribute("alt"));

                            return new Name(castId, fullName[0], fullName[1]);
                        })
                        .forEach(sink::next);

                sink.complete();
                log.debug("Finished scraping full cast: {}", url);
            } catch (Exception e) {
                sink.error(new ImdbScrapeException(e));
            } finally {
                driver.quit();
            }
        });
    }
}
