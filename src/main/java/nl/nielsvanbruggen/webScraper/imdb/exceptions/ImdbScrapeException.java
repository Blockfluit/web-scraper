package nl.nielsvanbruggen.webScraper.imdb.exceptions;

public class ImdbScrapeException extends RuntimeException {
    public ImdbScrapeException(Throwable throwable) {
        super(throwable);
    }
}
