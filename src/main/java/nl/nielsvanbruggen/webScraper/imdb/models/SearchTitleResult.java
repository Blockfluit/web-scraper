package nl.nielsvanbruggen.webScraper.imdb.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.nielsvanbruggen.webScraper.imdb.utils.ImdbUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

@Slf4j
@Data
public class SearchTitleResult {
    private String imdbId;
    private String title;
    private Integer releaseYear;
    private String thumbnail;
    private String type;
    private String description;
    @JsonIgnore
    private int order;

    public SearchTitleResult(WebElement element) {
        String fullTitle = element.findElement(By.xpath(".//h3[@class='ipc-title__text']")).getText();
        this.title = fullTitle.replaceAll("^[0-9]+\\.", "").trim();
        this.order = Integer.parseInt(fullTitle.split("\\.")[0]);
        this.imdbId = ImdbUtils.getIdFromUrl(element.findElement(By.xpath(".//a[contains(@href, 'title')]")).getAttribute("href"));

        String releaseYearString = element.findElement(By.xpath(".//span[contains(@class, 'dli-title-metadata-item')]")).getText().replaceAll("[^0-9*]", "");
        if(releaseYearString.length() >= 4) this.releaseYear = Integer.parseInt(releaseYearString.substring(0, 4));

        try {
            this.description = element.findElement(By.xpath(".//div[contains(@class, 'dli-plot-container')]/div")).getText();
        } catch (NoSuchElementException ignored) {}
        try {
            this.type = element.findElement(By.xpath(".//span[contains(@class, 'dli-title-type-data')]")).getText();
        } catch (NoSuchElementException ignored) {}
        try {
            this.thumbnail = element.findElement(By.xpath(".//img[contains(@class, 'ipc-image')]")).getAttribute("src");
        } catch (NoSuchElementException ignored) {}
    }
}
