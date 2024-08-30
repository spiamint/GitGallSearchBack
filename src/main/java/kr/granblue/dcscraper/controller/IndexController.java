package kr.granblue.dcscraper.controller;

import kr.granblue.dcscraper.controller.form.FindPageForm;
import kr.granblue.dcscraper.controller.form.ScrapeStartForm;
import kr.granblue.dcscraper.driver.WebDriverUtil;
import kr.granblue.dcscraper.repository.CommandQueryExecutor;
import kr.granblue.dcscraper.repository.dto.SimilarityDto;
import kr.granblue.dcscraper.scraper.enums.ScrapingOption;
import kr.granblue.dcscraper.scraper.service.DcPageFinder;
import kr.granblue.dcscraper.service.DcBoardEmbeddingService;
import kr.granblue.dcscraper.service.DcBoardService;
import kr.granblue.dcscraper.service.ScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@Slf4j
@RequiredArgsConstructor
public class IndexController {

    private final DcPageFinder pageFinder;

    private final CommandQueryExecutor commandQueryExecutor;
    private final EmbeddingModel embeddingModel;

    private final DcBoardService boardService;
    private final DcBoardEmbeddingService boardEmbeddingService;
    private final ScrapingService scrapingService;

    private final DataSource dataSource;


    @RequestMapping("/command")
//    @Transactional
    public String command() {
        try {
            dataSource.getConnection().prepareStatement("vacuum full scrape_prod.dc_board").execute();
        } catch (Exception e) {
            log.error("{}", e);
        }
//        commandQueryExecutor.vacuum();
//        commandQueryExecutor.alterTimeout();
//        commandQueryExecutor.alterTimeout2();
        return "index";
    }

    @RequestMapping("/start")
    public String start(@ModelAttribute("scrapeStartForm") ScrapeStartForm scrapeStartForm) {
        Long startPage = scrapeStartForm.getStartPage();
        Long endPage = scrapeStartForm.getEndPage();
        Long interval = scrapeStartForm.getInterval();
        ScrapingOption scrapeOption = scrapeStartForm.getScrapeOption();
        log.info("startPage: {}, endPage: {}, interval: {}, scrapeOption: {}", startPage, endPage, interval, scrapeOption);

        if (scrapeOption == ScrapingOption.VIEWPAGE) {
            return "redirect:/";
        }
        scrapingService.scrape(startPage, endPage, interval, scrapeOption);

        return "redirect:/";
    }

    @RequestMapping("/start-single")
    public String startSingle(@ModelAttribute("scrapeStartForm") ScrapeStartForm scrapeStartForm) {
        Long startPage = scrapeStartForm.getStartPage();
        Long endPage = startPage;
        Long interval = 1L;
        ScrapingOption scrapeOption = scrapeStartForm.getScrapeOption();
        log.info("startPage: {}, endPage: {}, interval: {}, scrapeOption: {}", startPage, endPage, interval, scrapeOption);

        if (scrapeOption == ScrapingOption.VIEWPAGE) {
            return "redirect:/";
        }
        scrapingService.scrape(startPage, endPage, interval, scrapeOption);

        return "redirect:/";
    }

    @RequestMapping("/delete-duplicate")
    public String deleteDuplicate(@RequestParam(name="limitDcNum") long limitCount) {
        long deletedCount = boardService.deleteDuplicate(limitCount);
        log.info("deletedCount: {}", deletedCount);
        return "redirect:/";
    }

    @RequestMapping("/quit")
    public String quit() {
        scrapingService.quitDriver();
        return "redirect:/";
    }

    @RequestMapping("find-page")
    public String findPage(@ModelAttribute("findPageForm") FindPageForm form) {
        log.info("form: {}", form);
        String inputYear = "" + form.getYear();
        String inputMonth = form.getMonth() > 10 ? "" + form.getMonth() : "0" + form.getMonth();
        String inputDay = form.getDay() > 10 ? "" + form.getDay() : "0" + form.getDay();
        boolean isMinorGallery = "true".equals(form.getIsMinorGallery());

        String inputTimeString = inputYear + "-" + inputMonth + "-" + inputDay + " 00-00-00";
        LocalDateTime inputTime = LocalDateTime.from(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss").parse(inputTimeString));

        WebDriver chromeDriver = WebDriverUtil.getChromeDriver();
        try {
            pageFinder.findFirstPageByDate(inputTime, form.getGalleryId(), isMinorGallery, chromeDriver);
        } catch (Exception e) {
            log.error("[ERROR] scrapingService.findFirstPageByDate();=====================================\n" +
                    "{}", e);
            chromeDriver.quit(); // 예외 발생시 드라이버 종료
        }

        return "redirect:/";
    }


    @RequestMapping("/embed")
    public String embed() {
        boardEmbeddingService.embedNotEmbedded(1000, 90000, null);
        return "redirect:/";
    }

    @RequestMapping("/embed-partial")
    public String partialEmbed() {
        boardEmbeddingService.partialEmbed(1000, 70000);
        return "redirect:/";
    }

    @RequestMapping("/search")
    public String search(@RequestParam(name = "query") String query) {
        log.info("query: {}", query);
        EmbeddingResponse embeddingResponse = embeddingModel.call(new EmbeddingRequest(
                List.of(query),
                OpenAiEmbeddingOptions.builder().withDimensions(256).build()
        ));
        float[] embeddedQuery = embeddingResponse.getResult().getOutput();
        List<SimilarityDto> similarityDtos = boardService.getSimilarityTop10(embeddedQuery.toString());
        similarityDtos.forEach(similarityDto -> {
            log.info("similarityDto: {}", similarityDto);
        });
        return "index";
    }

}


