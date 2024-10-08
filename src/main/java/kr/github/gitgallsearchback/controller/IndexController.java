package kr.github.gitgallsearchback.controller;

import kr.github.gitgallsearchback.controller.form.FindPageForm;
import kr.github.gitgallsearchback.controller.form.ScrapeStartForm;
import kr.github.gitgallsearchback.domain.Board;
import kr.github.gitgallsearchback.domain.Comment;
import kr.github.gitgallsearchback.mail.EmailDTO;
import kr.github.gitgallsearchback.mail.EmailSender;
import kr.github.gitgallsearchback.repository.CommandQueryExecutor;
import kr.github.gitgallsearchback.repository.dto.DuplicateCountDto;
import kr.github.gitgallsearchback.scraper.enums.ScrapingOption;
import kr.github.gitgallsearchback.scraper.service.DcPageFinder;
import kr.github.gitgallsearchback.scraper.util.WebDriverUtil;
import kr.github.gitgallsearchback.service.EmbeddingService;
import kr.github.gitgallsearchback.service.BoardService;
import kr.github.gitgallsearchback.service.CommentService;
import kr.github.gitgallsearchback.service.ScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Controller
@Slf4j
@RequiredArgsConstructor
public class IndexController {

    private final DcPageFinder pageFinder;

    private final EmbeddingModel embeddingModel;

    private final BoardService boardService;
    private final EmbeddingService boardEmbeddingService;
    private final CommentService commentService;
    private final ScrapingService scrapingService;

    // 스크래핑 ===========================================================

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

    // 임베딩 ===========================================================

    @RequestMapping("/embed-full")
    public String embed(@RequestParam(name = "pageSize") int pageSize,
                        @RequestParam(name = "maxSize") int maxSize) {
        boardEmbeddingService.embed(pageSize, maxSize, "full");
        return "redirect:/";
    }

    @RequestMapping("/embed-from-last")
    public String partialEmbed(@RequestParam(name = "pageSize") int pageSize,
                               @RequestParam(name = "maxSize") int maxSize) {
        boardEmbeddingService.embed(pageSize, maxSize, "from-last");
        return "redirect:/";
    }

    // 페이지 찾기 ===========================================================

    @RequestMapping("find-page")
    public String findPage(@ModelAttribute("findPageForm") FindPageForm form ) {
        log.info("form: {}", form);
        String inputYear = "" + form.getYear();
        String inputMonth = form.getMonth() > 10 ? "" + form.getMonth() : "0" + form.getMonth();
        String inputDay = form.getDay() > 10 ? "" + form.getDay() : "0" + form.getDay();
        boolean isMinorGallery = "true".equals(form.getIsMinorGallery());

        String inputTimeString = inputYear + "-" + inputMonth + "-" + inputDay + " 00-00-00";
        LocalDateTime inputTime = LocalDateTime.from(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss").parse(inputTimeString));

        try {
            pageFinder.findFirstPageByDate(inputTime, form.getGalleryId(), isMinorGallery);
        } catch (Exception e) {
            log.error("[ERROR] scrapingService.findFirstPageByDate();=====================================\n" +
                    "{}", e.getMessage());
        }

        return "redirect:/";
    }

    // 중복제거 ===========================================================

    @RequestMapping("/show-duplicate")
    public String showDuplicateBoard(Model model) {
        List<Board> duplicateBoard = boardService.findDuplicateBoard();
        DuplicateCountDto duplicateCount = boardService.findDuplicateCount();
//        duplicateBoard.forEach(board  -> log.info("{}", board));
        log.info("\n================================================================\n" +
                "duplicateBoard.size() = {}\n" +
                "totalCount = {}, distinctCount = {}\n",
                duplicateBoard.size(),
                duplicateCount.getTotalCount(), duplicateCount.getDistinctCount());

        model.addAttribute("duplicateBoard", duplicateBoard);
        model.addAttribute("totalCount", duplicateCount.getTotalCount());
        model.addAttribute("distinctCount", duplicateCount.getDistinctCount());
        model.addAttribute("expectedCount", duplicateCount.getTotalCount() - duplicateCount.getDistinctCount());
        return "index";
    }

    @RequestMapping("/delete-duplicate")
    public String deleteDuplicateBoard(Model model) {
        int deletedCount = boardService.deleteDuplicate();
        log.info("deletedCount: {}", deletedCount);
        model.addAttribute("deletedBoardCount", deletedCount);
        return "index";
    }

    @RequestMapping("/show-duplicate-comment")
    public String showDuplicateComment(Model model) {
        List<Comment> duplicateComments = commentService.findDuplicate();
        DuplicateCountDto duplicateCount = commentService.findDuplicateCount();
//        duplicateComments.forEach(board  -> log.info("{}", board));
        log.info("\n================================================================\n" +
                        "duplicateComments.size() = {}\n" +
                        "totalCount = {}, distinctCount = {}\n",
                duplicateComments.size(),
                duplicateCount.getTotalCount(), duplicateCount.getDistinctCount());

        model.addAttribute("duplicateComments", duplicateComments);
        model.addAttribute("totalCount", duplicateCount.getTotalCount());
        model.addAttribute("distinctCount", duplicateCount.getDistinctCount());
        model.addAttribute("expectedCount", duplicateCount.getTotalCount() - duplicateCount.getDistinctCount());
        return "index";
    }

    @RequestMapping("/delete-duplicate-comment")
    public String deleteDuplicateComment(Model model) {
        int deletedCount = commentService.deleteDuplicate();
        log.info("deletedCount: {}", deletedCount);
        model.addAttribute("deletedCommentCount", deletedCount);
        return "index";
    }



    // 기타 ===========================================================

    private final CommandQueryExecutor commandQueryExecutor;
    private final DataSource dataSource;
    @RequestMapping("/command")
    @Transactional
    public String command() {
//        try {
//            dataSource.getConnection().prepareStatement("vacuum full scrape_prod.dc_board_embedding").execute();
//        } catch (Exception e) {
//            log.error("{}", e);
//        }
//        commandQueryExecutor.vacuum();
//        commandQueryExecutor.alterTimeout();
//        commandQueryExecutor.alterTimeout2();
        return "index";
    }

}


