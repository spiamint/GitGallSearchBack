package kr.github.gitgallsearchback.scraper.util;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverService;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.remote.http.jdk.JdkHttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class WebDriverUtil {
    public static WebDriver getChromeDriver() {
        // webDriver 옵션 설정
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments(
//                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--lang=ko",
                "--disable-images",
                "--blink-settings=imagesEnabled=false" // 이미지 제거 옵션 alternative
        );
        // 페이지 로드 전략 NONE
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.NONE);

//         jdk http client timeout 설정 (기본값 3분)
        ClientConfig clientConfig = ClientConfig.defaultConfig().
                readTimeout(Duration.ofSeconds(60)).
                connectionTimeout(Duration.ofSeconds(10))
                .withRetries();


//        ChromeDriverService defaultChromeService = ChromeDriverService.createDefaultService();

        ChromeDriver driver = new ChromeDriver(chromeOptions);
//        driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(5000));
//        driver.manage().timeouts().scriptTimeout(Duration.ofMillis(5000));
//        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(2000));


        return driver;
    }

    public static WebDriver getEagerChromeDriver() {
        // webDriver 옵션 설정
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments(
//                "--headless",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--lang=ko",
                "--disable-images",
                "--blink-settings=imagesEnabled=false" // 이미지 제거 옵션 alternative
        );
        // 페이지 로드 전략 NONE
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        return new ChromeDriver(chromeOptions);
    }

    public static WebDriver getImplicitWaitChromeDriver() {
        // webDriver 옵션 설정
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--lang=ko",
                "--disable-images",
                "--blink-settings=imagesEnabled=false", // 이미지 제거 옵션 alternative
                "--disable-search-engine-choice-screen",
                "--disable-features=OptimizationGuideModelDownloading,OptimizationHintsFetching,OptimizationTargetPrediction,OptimizationHints"
        );
        // 페이지 로드 전략 NONE
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.NONE);

        ClientConfig clientConfig = ClientConfig.defaultConfig().
                readTimeout(Duration.ofSeconds(60)).
                connectionTimeout(Duration.ofSeconds(10));
//
        ChromeDriver driver = new ChromeDriver(ChromeDriverService.createDefaultService(),
                chromeOptions, clientConfig);
//        ChromeDriver driver = new ChromeDriver(chromeOptions);
        // impilcitlyWait 설정
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(2000));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));

        return driver;
    }

    public static WebDriver getImplicitWaitEdgeDriver() {
        // webDriver 옵션 설정
        EdgeOptions options = new EdgeOptions();
        options.addArguments(
                "--headless",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--lang=ko",
                "--disable-images",
                "--blink-settings=imagesEnabled=false", // 이미지 제거 옵션 alternative
                "--disable-search-engine-choice-screen",
                "--disable-features=OptimizationGuideModelDownloading,OptimizationHintsFetching,OptimizationTargetPrediction,OptimizationHints"

        );


        // 이미지 제거 옵션 alternative
//        Map<String, Object> prefs = new HashMap<>();
//        prefs.put("profile.managed_default_content_settings.images", 2);
//        chromeOptions.setExperimentalOption("prefs", prefs);

        // 페이지 로드 전략 NONE
        options.setPageLoadStrategy(PageLoadStrategy.NONE);

        // clientConfig 를 통해 JDKHttpClient 에 readTimeout 을 설정하면,
        // java.co.concurrent.TimeoutException 발생 시 아무것도 못하고 그대로 Exception 연속으로 계속 터진뒤 종료됨..
//        ClientConfig clientConfig = ClientConfig.defaultConfig().
//                readTimeout(Duration.ofSeconds(5)).
//                connectionTimeout(Duration.ofSeconds(10));
//        EdgeDriver driver = new EdgeDriver(EdgeDriverService.createDefaultService(), options, clientConfig);


        EdgeDriver driver = new EdgeDriver(options);
        // impilcitlyWait 설정
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(2000));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(10000));
        return driver;
    }
}
