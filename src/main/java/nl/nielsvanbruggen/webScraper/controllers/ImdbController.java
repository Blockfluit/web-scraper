package nl.nielsvanbruggen.webScraper.controllers;

import lombok.RequiredArgsConstructor;
import nl.nielsvanbruggen.webScraper.models.Name;
import nl.nielsvanbruggen.webScraper.models.Title;
import nl.nielsvanbruggen.webScraper.scrapers.ImdbScraper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@RequestMapping("/imdb")
public class ImdbController {
    private final ImdbScraper imdbScraper;

    @GetMapping("/title/{id}")
    public ResponseEntity<Title> getTitle(@PathVariable String id) {
        Title title = imdbScraper.scrapeTitle(id).block();

        return ResponseEntity.ok(title);
    }

    @GetMapping("/name/{id}")
    public ResponseEntity<Name> getName(@PathVariable String id) {
        Name name = imdbScraper.scrapeName(id).block();

        return ResponseEntity.ok(name);
    }
}
