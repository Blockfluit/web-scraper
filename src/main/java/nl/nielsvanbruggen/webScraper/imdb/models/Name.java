package nl.nielsvanbruggen.webScraper.imdb.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.nielsvanbruggen.webScraper.imdb.exceptions.ImdbScrapeException;
import nl.nielsvanbruggen.webScraper.imdb.utils.ImdbUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Name {
    @JsonIgnore
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private String imdbId;
    private String firstname;
    private String lastname;
    private String description;
    private List<String> roles;
    private LocalDate dateOfBirth;
    private LocalDate dateOfDeath;

    public Name(String imdbId, String firstname, String lastname) {
        this.imdbId = imdbId;
        this.firstname = firstname;
        this.lastname = lastname;
    }

    public Name(String imdbId, WebElement webElement) throws ImdbScrapeException {
        this.imdbId = imdbId;

        try {
            String[] fullName = ImdbUtils.parseName(webElement.findElement(By.cssSelector("span[data-testid='hero__primary-text']")).getText());
            this.firstname = fullName[0];
            this.lastname = fullName[1];

            try {
                this.roles = webElement
                        .findElement(By.cssSelector("section[data-testid='atf-wrapper-bg']"))
                        .findElements(By.xpath("//div[h1[@data-testid='hero__pageTitle']]/ul/li")).stream()
                        .map(WebElement::getText)
                        .toList();
            } catch (NoSuchElementException ignored) {}

            try {
                this.description = webElement
                        .findElement(By.cssSelector("section[data-testid='atf-wrapper-bg']"))
                        .findElement(By.className("ipc-html-content-inner-div"))
                        .getAttribute("innerHTML")
                        .replaceAll("<a[^>]*>(.*?)</a>", "$1");
            } catch (NoSuchElementException ignored) {}

            if(!webElement.findElements(By.cssSelector("div[data-testid='birth-and-death-birthdate']")).isEmpty()) {
                String birthText = webElement.findElement(By.xpath("//div[@data-testid='birth-and-death-birthdate'][1]"))
                        .findElement(By.xpath(".//span[2]"))
                        .getAttribute("innerHTML");
                try {
                    this.dateOfBirth = LocalDate.parse(birthText, formatter);
                } catch (DateTimeParseException ignored) {}
            }

            if(!webElement.findElements(By.cssSelector("div[data-testid='birth-and-death-deathdate']")).isEmpty()) {
                String deathText = webElement.findElement(By.xpath("//div[@data-testid='birth-and-death-deathdate'][1]"))
                        .findElement(By.xpath(".//span[2]"))
                        .getAttribute("innerHTML")
                        .replaceAll("<span.+/span>", "");
                try {
                    this.dateOfDeath = LocalDate.parse(deathText, formatter);
                } catch (DateTimeParseException ignored) {}
            }
        } catch (Exception e) {
            throw new ImdbScrapeException(e);
        }
    }
}
