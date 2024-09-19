package kr.github.gitgallsearchback.scraper.service;

import kr.github.gitgallsearchback.scraper.dto.DcBoardsAndComments;
import kr.github.gitgallsearchback.scraper.dto.ScrapeRequest;
import kr.github.gitgallsearchback.scraper.enums.ScrapingOption;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public interface DcScraper {

    /**
     * 사용자 정의 Executor 를 등록합니다. (callback 에 사용)
     * null 입력시 ForkJoinPool.commonPool() 로 초기화 됩니다.
     * @param executor
     */
    void setExecutor(Executor executor);


    /**
     * 스크래핑 옵션을 설정합니다.
     * @see kr.github.gitgallsearchback.scraper.enums.ScrapingOption
     * @param scrapingOption
     */
    void setScrapingOption(ScrapingOption scrapingOption);

    /**
     * 스크래핑을 시작합니다.
     * @param scrapeRequest
     */
    DcBoardsAndComments start(ScrapeRequest scrapeRequest);

    /**
     * 주어진 콜백함수를 비동기로 등록하여 스크래핑을 시작합니다.
     * interval 이 2 일경우 두 페이지 마다 실행 (1-2-callback-3-4-callback...)
     *
     * @param scrapeRequest
     * @param callback      스크래핑 결과로 실행할 Consumer&lt;DcBoardsAndComments&gt; 콜백함수.
     */
    void startWithCallback(ScrapeRequest scrapeRequest, Consumer<DcBoardsAndComments> callback);


}
