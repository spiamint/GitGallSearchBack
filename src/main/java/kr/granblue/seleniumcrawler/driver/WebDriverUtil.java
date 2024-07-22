package kr.granblue.seleniumcrawler.driver;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.time.Duration;

@Component
@Slf4j
public class WebDriverUtil {

    public static WebDriver getChromeDriver() {
//        if (ObjectUtils.isEmpty(System.getProperty("webdriver.chrome.driver"))) {
//            System.setProperty("webdriver.chrome.driver", WEB_DRIVER_PATH);
//        }
        // webDriver 옵션 설정
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--lang=ko");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--disable-gpu");
//        chromeOptions.setCapability("ignoreProtectedModeSettings", true);
        WebDriver driver = new ChromeDriver(chromeOptions);
        driver.get("https://www.google.com/");
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(15)); // 대기시간 설정
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
