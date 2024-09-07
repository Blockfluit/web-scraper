package nl.nielsvanbruggen.webScraper.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.nielsvanbruggen.webScraper.exceptions.ImdbScrapeException;
import nl.nielsvanbruggen.webScraper.utils.ImdbUtils;
import org.openqa.selenium.By;
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

    public Name(String imdbId, WebDriver driver) throws ImdbScrapeException {
        this.imdbId = imdbId;

        try {
            String[] fullName = ImdbUtils.parseName(driver.findElement(By.cssSelector("span[data-testid='hero__primary-text']")).getText());
            this.firstname = fullName[0];
            this.lastname = fullName[1];
            this.description = driver
                    .findElement(By.cssSelector("section[data-testid='atf-wrapper-bg']"))
                    .findElement(By.className("ipc-html-content-inner-div"))
                    .getAttribute("innerHTML")
                    .replaceAll("<a[^>]*>(.*?)</a>", "$1");
            this.roles = driver
                    .findElement(By.cssSelector("section[data-testid='atf-wrapper-bg']"))
                    .findElements(By.xpath("//div[h1[@data-testid='hero__pageTitle']]/ul/li")).stream()
                    .map(WebElement::getText)
                    .toList();

            if(!driver.findElements(By.cssSelector("div[data-testid='birth-and-death-birthdate']")).isEmpty()) {
                String birthText = driver.findElement(By.xpath("//div[@data-testid='birth-and-death-birthdate'][1]"))
                        .findElement(By.xpath(".//span[2]"))
                        .getAttribute("innerHTML");
                try {
                    this.dateOfBirth = LocalDate.parse(birthText, formatter);
                } catch (DateTimeParseException e) {
                    log.warn("Could not parse date of birth of imdb name: {}", imdbId);
                }
            }

            if(!driver.findElements(By.cssSelector("div[data-testid='birth-and-death-deathdate']")).isEmpty()) {
                String deathText = driver.findElement(By.xpath("//div[@data-testid='birth-and-death-deathdate'][1]"))
                        .findElement(By.xpath(".//span[2]"))
                        .getAttribute("innerHTML")
                        .replaceAll("<span.+/span>", "");
                try {
                    this.dateOfDeath = LocalDate.parse(deathText, formatter);
                } catch (DateTimeParseException e) {
                    log.warn("Could not parse date of death of imdb name: {}", imdbId);
                }
            }
        } catch (Exception e) {
            throw new ImdbScrapeException(e);
        }
    }
}
