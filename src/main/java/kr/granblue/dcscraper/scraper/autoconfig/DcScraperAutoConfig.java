package kr.granblue.dcscraper.scraper.autoconfig;

import kr.granblue.dcscraper.scraper.extractor.BoardExtractor;
import kr.granblue.dcscraper.scraper.extractor.CommentExtractor;
import kr.granblue.dcscraper.scraper.service.DcPageFinder;
import kr.granblue.dcscraper.scraper.service.DcScraper;
import kr.granblue.dcscraper.scraper.service.impl.DefaultDcPageFinder;
import kr.granblue.dcscraper.scraper.service.impl.DefaultDcScraper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DcScraperAutoConfig {

    @Bean
    public BoardExtractor boardExtractor() {
        return new BoardExtractor();
    }

    @Bean
    public CommentExtractor commentExtractor() {
        return new CommentExtractor();
    }

    @Bean
    @ConditionalOnMissingBean(DcScraper.class)
    public DcScraper dcScraper() {
        return new DefaultDcScraper(boardExtractor(), commentExtractor());
    }

    @Bean
    @ConditionalOnMissingBean(DcPageFinder.class)
    public DcPageFinder dcPageFinder() {
        return new DefaultDcPageFinder(boardExtractor());
    }

}
