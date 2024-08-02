package kr.granblue.seleniumcrawler.driver;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class WebDriverUtil {

    public static WebDriver getChromeDriver() {
        // webDriver 옵션 설정
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless",
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--lang=ko",
                "--disable-images",
                "--blink-settings=imagesEnabled=false");
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.managed_default_content_settings.images", 2);
        chromeOptions.setExperimentalOption("prefs", prefs);

//        chromeOptions.setCapability("ignoreProtectedModeSettings", true);
        ChromeDriverService defaultChromeService = ChromeDriverService.createDefaultService();
        WebDriver driver = new ChromeDriver(defaultChromeService, chromeOptions);
        driver.get("https://www.google.com/");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5)); // 대기시간 설정
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(7)); // 대기시간 설정
        return driver;
    }

    public static void quit(WebDriver driver) {
        if (!ObjectUtils.isEmpty(driver)) {
            driver.quit();
        }
    }

    public static void close(WebDriver driver) {
        if (!ObjectUtils.isEmpty(driver)) {
            driver.close();
        }
    }


}
