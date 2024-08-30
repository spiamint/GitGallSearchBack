package kr.granblue.dcscraper.service;

import com.google.common.base.Stopwatch;
import kr.granblue.dcscraper.domain.mapper.DcBoardMapper;
import kr.granblue.dcscraper.domain.mapper.DcCommentMapper;
import kr.granblue.dcscraper.scraper.dto.*;
import kr.granblue.dcscraper.scraper.enums.ScrapingOption;
import kr.granblue.dcscraper.scraper.service.DcScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverInfo;
import org.openqa.selenium.chrome.ChromeDriverInfo;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapingService {

    private final DcBoardService boardService;
    private final DcCommentService commentService;
    private final DcScraper dcScraper;

    private LocalDateTime startTimeGlobal = null; // 흘리는거 확인하기 위한 페이지별 시작시간

    public void scrape(long startPage, long endPage, long interval, ScrapingOption scrapingOption) {

        // 시간 확인
        LocalDateTime startTime = LocalDateTime.now();
        startTimeGlobal = startTime;

        // 콜백 결정
        Consumer<DcBoardsAndComments> callback = null;
        if (scrapingOption == ScrapingOption.LISTPAGE) {
            // 리스트 페이지 콜백
            callback = (dcBoardsAndComments) -> saveResultFromListPage(dcBoardsAndComments);
        } else if (scrapingOption == ScrapingOption.ALL) {
            // 리스트 페이지 가 저장된 상태에서 ALL 재저장 콜백
            callback = (dcBoardsAndComments) -> saveResultFromAll(dcBoardsAndComments);
        }

        dcScraper.setScrapingOption(scrapingOption);
        dcScraper.setAutoQuitWebDriver(true);
        dcScraper.setWebDriverWait(new WebDriverWait(dcScraper.getWebDriver(), Duration.ofSeconds(3)));
        dcScraper.startWithCallback(ScrapeRequest.of(startPage, endPage, interval), callback);

        // 웹 스크래핑 종료
        long savedBoardCounter = boardService.countByCreatedAtAfter(startTime);
        long savedCommentCounter = commentService.countByCreatedAtAfter(startTime);

        log.info("[SCRAPE COMPLETE] from ScrapingService : savedBoardCounter = {}, savedCommmentCounter = {}", savedBoardCounter, savedCommentCounter);
    }

    @Transactional
    public void tmp(DcBoardsAndComments dcBoardsAndComments) {
        List<DcBoard> boards = dcBoardsAndComments.getBoards();
        if (boards.get(0).getDcNum() < 4709579) {
            return;
        }
        List<kr.granblue.dcscraper.domain.DcBoard> collect = boards.stream().map(board -> DcBoardMapper.fromDto(board)).collect(Collectors.toList());
        boardService.setTitle(collect);
    }

    // callback : 리스트페이지에서 추출한것만 저장
    public void saveResultFromListPage(DcBoardsAndComments dcBoardsAndComments) {
        List<DcBoard> boards = dcBoardsAndComments.getBoards();

        List<kr.granblue.dcscraper.domain.DcBoard> domainBoards = boards.stream().map(board -> DcBoardMapper.fromDto(board)).collect(Collectors.toList());

        boardService.saveBoards(domainBoards);

        long savedBoardCounter = boardService.countByCreatedAtAfter(startTimeGlobal);
        log.info("[SAVE RESULT] accumulated boards.size = {} savedBoardCounter = {}, ", boards.size(), savedBoardCounter);
    }

    // callback : ALL 로 추출한것 저장
    public void saveResultFromAll(DcBoardsAndComments dcBoardsAndComments) {
        List<DcBoard> boards = dcBoardsAndComments.getBoards();
        List<DcComment> comments = dcBoardsAndComments.getComments();

        List<kr.granblue.dcscraper.domain.DcBoard> domainBoards = boards.stream().map(board -> DcBoardMapper.fromDto(board)).collect(Collectors.toList());
        List<kr.granblue.dcscraper.domain.DcComment> domainComments = comments.stream().map(comment -> DcCommentMapper.fromDto(comment)).collect(Collectors.toList());

        boardService.saveBoards(domainBoards);
        commentService.saveComments(domainComments);

        long savedBoardCounter = boardService.countByCreatedAtAfter(startTimeGlobal);
        long savedCommentCounter = commentService.countByCreatedAtAfter(startTimeGlobal);
        log.info("[SAVE RESULT] accumulated boards.size = {}, comments.size = {}, savedBoardCounter = {}, savedCommentCounter = {}", boards.size(), comments.size(), savedBoardCounter, savedCommentCounter);

    }

    // callback : 리스트 페이지에서 추출했던것에 ALL 로 추출한것 추가로 저장 (채워넣기)
    public void saveResultFromAllToListPage(DcBoardsAndComments dcBoardsAndComments) {
        List<DcBoard> boards = dcBoardsAndComments.getBoards();
        List<DcComment> comments = dcBoardsAndComments.getComments();

        List<kr.granblue.dcscraper.domain.DcBoard> domainBoards = boards.stream().map(board -> DcBoardMapper.fromDto(board)).collect(Collectors.toList());
        List<kr.granblue.dcscraper.domain.DcComment> domainComments = comments.stream().map(comment -> DcCommentMapper.fromDto(comment)).collect(Collectors.toList());

        boardService.fillContent(domainBoards);
        commentService.saveComments(domainComments);

        long savedBoardCounter = boardService.countByCreatedAtAfter(startTimeGlobal);
        long savedCommentCounter = commentService.countByCreatedAtAfter(startTimeGlobal);
        log.info("[SAVE RESULT] accumulated boards.size = {}, comments.size = {}, savedBoardCounter = {}, savedCommentCounter = {}", boards.size(), comments.size(), savedBoardCounter, savedCommentCounter);

    }

    public void quitDriver() {
        dcScraper.quitDriver();
    }

}
