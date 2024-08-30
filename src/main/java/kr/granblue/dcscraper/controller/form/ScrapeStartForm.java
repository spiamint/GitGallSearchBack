package kr.granblue.dcscraper.controller.form;

import kr.granblue.dcscraper.scraper.enums.ScrapingOption;
import lombok.Data;

@Data
public class ScrapeStartForm {
    private Long startPage;
    private Long endPage;
    private Long interval;
    private ScrapingOption scrapeOption;
}
