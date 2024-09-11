package kr.granblue.gbfsearchback.controller;

import kr.granblue.gbfsearchback.controller.form.FindPageForm;
import kr.granblue.gbfsearchback.controller.form.ScrapeStartForm;
import kr.granblue.gbfsearchback.domain.DcBoard;
import kr.granblue.gbfsearchback.domain.DcComment;
import kr.granblue.gbfsearchback.repository.CommandQueryExecutor;
import kr.granblue.gbfsearchback.repository.dto.DuplicateCountDto;
import kr.granblue.gbfsearchback.scraper.enums.ScrapingOption;
import kr.granblue.gbfsearchback.scraper.service.DcPageFinder;
import kr.granblue.gbfsearchback.service.DcBoardEmbeddingService;
import kr.granblue.gbfsearchback.service.DcBoardService;
import kr.granblue.gbfsearchback.service.DcCommentService;
import kr.granblue.gbfsearchback.service.ScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
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

    private final EmbeddingModel embeddingModel;

    private final DcBoardService boardService;
    private final DcBoardEmbeddingService boardEmbeddingService;
    private final DcCommentService commentService;
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
        List<DcBoard> duplicateBoard = boardService.findDuplicateBoard();
        DuplicateCountDto duplicateCount = boardService.findDuplicateCount();
        duplicateBoard.forEach(board  -> log.info("{}", board));
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
        List<DcComment> duplicateComments = commentService.findDuplicate();
        DuplicateCountDto duplicateCount = commentService.findDuplicateCount();
        duplicateComments.forEach(board  -> log.info("{}", board));
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


