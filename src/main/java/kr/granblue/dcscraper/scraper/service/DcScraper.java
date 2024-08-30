package kr.granblue.dcscraper.scraper.service;

import kr.granblue.dcscraper.scraper.dto.DcBoardsAndComments;
import kr.granblue.dcscraper.scraper.dto.ScrapeRequest;
import kr.granblue.dcscraper.scraper.enums.ScrapingOption;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public interface DcScraper {

    /**
     * 기존 WebDriver 를 종료하고 사용자 정의 WebDriver 를 등록합니다.
     * 사용자 정의 WebDriver 를 등록하면, 기본 Explicit WebDriverWait 는 해제됩니다.
     * null 입력시 기본값 (util.WebDriverUtil.getChromeDriver()) 초기화 되며 Explicit WebDriverWait 도 기본값으로 같이 초기화 됩니다.
     * @param webDriver
     */
    void setWebDriver(WebDriver webDriver);

    /**
     * 사용자 정의 Explicit WebDriverWait 을 등록 합니다.
     * null 입력시 기본값 (WebDriver wait = new WebDriverWait(webDriver, Duration(2000))으로 초기화 됩니다.
     * @param explicitWait
     */
    void setWebDriverWait(WebDriverWait explicitWait);

    /**
     * Explicit WebDriverWait 를 해제합니다.
     * WebDriver 기본값의 PageLoadStrategy 가 NONE 으로 설정되어 있으므로 WebDriver.manage().timeouts 을 설정하지 않으면 제대로 동작하지 않습니다.
     */
    void clearWebDriverWait();

    /**
     * 사용자 정의 Executor 를 등록합니다. (callback 에 사용)
     * null 입력시 ForkJoinPool.commonPool() 로 초기화 됩니다.
     * @param executor
     */
    void setExecutor(Executor executor);

    /**
     * 스크래핑 옵션을 설정합니다.
     * @see kr.granblue.dcscraper.scraper.enums.ScrapingOption
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

    /**
     * WebDriver 를 종료합니다.
     * 종료 후 재실행을 위해서는 setWebDriver() 를 통해 다시 등록해야 합니다.
     */
    void quitDriver();

    /**
     * 스크래핑이 종료되면 webdriver 를 자동으로 종료 되도록 설정합니다.
     * 해당 설정이 true 면 스크래핑 종료시 자동으로 드라이버가 종료되며
     * 스크래핑 재시작시 드라이버를 다시 setDriver 로 설정해야합니다.
     * 기본값 false
     */
    void setAutoQuitWebDriver(boolean autoQuitWebDriver);

    WebDriver getWebDriver();
}
