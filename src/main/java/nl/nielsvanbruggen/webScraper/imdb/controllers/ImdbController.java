package nl.nielsvanbruggen.webScraper.imdb.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.nielsvanbruggen.webScraper.imdb.models.Name;
import nl.nielsvanbruggen.webScraper.imdb.models.SearchTitleResult;
import nl.nielsvanbruggen.webScraper.imdb.models.Title;
import nl.nielsvanbruggen.webScraper.imdb.scrapers.ImdbScraper;
import org.openqa.selenium.TimeoutException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/imdb")
public class ImdbController {
    private final ImdbScraper imdbScraper;

    @GetMapping("/title/{id}")
    public ResponseEntity<Title> getTitle(@PathVariable String id) {
        try {
            Title title = imdbScraper.scrapeTitle(id).block();
            return ResponseEntity.ok(title);
        } catch (TimeoutException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/name/{id}")
    public ResponseEntity<Name> getName(@PathVariable String id) {
        try {
            Name name = imdbScraper.scrapeName(id).block();
            return ResponseEntity.ok(name);
        } catch (TimeoutException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/search/")
    public ResponseEntity<List<SearchTitleResult>> searchTitle(@RequestParam(name = "title") String title) {
        try {
            List<SearchTitleResult> suggestions = imdbScraper.scrapeSearchTitle(title)
                    .sort(Comparator.comparingInt(SearchTitleResult::getOrder))
                    .collectList()
                    .block();

            return ResponseEntity.ok(suggestions);
        } catch (TimeoutException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
