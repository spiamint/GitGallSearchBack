package kr.granblue.seleniumcrawler;

import kr.granblue.seleniumcrawler.crawler.DcCrawler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;

@SpringBootApplication
@Slf4j
public class SeleniumCrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeleniumCrawlerApplication.class, args);
    }

}
