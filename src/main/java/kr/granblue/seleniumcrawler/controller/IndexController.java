package kr.granblue.seleniumcrawler.controller;

import kr.granblue.seleniumcrawler.crawler.DcCrawler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequiredArgsConstructor
public class IndexController {

    private final DcCrawler dcCrawler;

    @RequestMapping("/start")
    public String start() {

        try {
            dcCrawler.crawlStart();
        } catch (Exception e) {
            log.error("[ERROR] dcCrawler.crawlStart();=====================================");
            e.printStackTrace();
            dcCrawler.getWebDriver().quit(); // 예외 발생시 드라이버 종료
        }


        return "index";
    }

}


