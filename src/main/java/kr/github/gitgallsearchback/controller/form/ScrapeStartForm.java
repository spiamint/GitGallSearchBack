package kr.github.gitgallsearchback.controller.form;

import kr.github.gitgallsearchback.scraper.enums.ScrapingOption;
import lombok.Data;

@Data
public class ScrapeStartForm {
    private Long startPage;
    private Long endPage;
    private Long interval;
    private ScrapingOption scrapeOption;
}
