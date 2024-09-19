package kr.github.gitgallsearchback.scraper.autoconfig;

import kr.github.gitgallsearchback.scraper.extractor.BoardExtractor;
import kr.github.gitgallsearchback.scraper.extractor.CommentExtractor;
import kr.github.gitgallsearchback.scraper.service.DcPageFinder;
import kr.github.gitgallsearchback.scraper.service.DcScraper;
import kr.github.gitgallsearchback.scraper.service.impl.DefaultDcPageFinder;
import kr.github.gitgallsearchback.scraper.service.impl.DefaultDcScraper;
import kr.github.gitgallsearchback.scraper.service.test.TestDcScraper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

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
