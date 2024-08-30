package kr.granblue.dcscraper.scraper.util;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.HashMap;
import java.util.Map;

public class WebDriverUtil {
    public static WebDriver getChromeDriver() {
        // webDriver 옵션 설정
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments(
                "--headless",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--lang=ko",
                "--disable-images",
                "--blink-settings=imagesEnabled=false" // 이미지 제거 옵션 alternative
        );

        // 이미지 제거 옵션 alternative
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.managed_default_content_settings.images", 2);
        chromeOptions.setExperimentalOption("prefs", prefs);

        // 페이지 로드 전략 NONE
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.NONE);

        ChromeDriverService defaultChromeService = ChromeDriverService.createDefaultService();
        WebDriver driver = new ChromeDriver(defaultChromeService, chromeOptions);

        return driver;
    }

}
