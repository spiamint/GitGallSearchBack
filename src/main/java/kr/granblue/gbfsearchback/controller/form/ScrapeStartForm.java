package kr.granblue.gbfsearchback.controller.form;

import kr.granblue.gbfsearchback.scraper.enums.ScrapingOption;
import lombok.Data;

@Data
public class ScrapeStartForm {
    private Long startPage;
    private Long endPage;
    private Long interval;
    private ScrapingOption scrapeOption;
}
