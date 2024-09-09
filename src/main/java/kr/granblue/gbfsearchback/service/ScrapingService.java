package kr.granblue.gbfsearchback.service;

import kr.granblue.gbfsearchback.domain.mapper.DcBoardMapper;
import kr.granblue.gbfsearchback.domain.mapper.DcCommentMapper;
import kr.granblue.gbfsearchback.scraper.dto.*;
import kr.granblue.gbfsearchback.scraper.enums.ScrapingOption;
import kr.granblue.gbfsearchback.scraper.service.DcScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    /**
     * 그랑블루 갤러리 스크래핑
     * @param startPage
     * @param endPage
     * @param interval
     * @param scrapingOption
     */
    public void scrape(long startPage, long endPage, long interval, ScrapingOption scrapingOption) {

        // 시간 확인
        LocalDateTime startTime = LocalDateTime.now();

        // 콜백 결정
        Consumer<DcBoardsAndComments> callback = null;
        if (scrapingOption == ScrapingOption.ALL) {
            callback = this::saveResultFromAll;
        } else if(scrapingOption == ScrapingOption.LISTPAGE) {
            callback = this::saveResultFromListPage;
        } else if(false) {
            // LISTPAGE -> ALL 채우기
        }

        dcScraper.setScrapingOption(scrapingOption);
        dcScraper.setAutoQuitWebDriver(true);

        dcScraper.startWithCallback(ScrapeRequest.of("granblue", startPage, endPage, interval), callback);

        // 웹 스크래핑 종료
        long savedBoardCounter = boardService.countByCreatedAtAfter(startTime);
        long savedCommentCounter = commentService.countByCreatedAtAfter(startTime);

        log.info("[SCRAPE COMPLETE] from ScrapingService : savedBoardCounter = {}, savedCommmentCounter = {}", savedBoardCounter, savedCommentCounter);
    }

    /**
     * ScrapingOption.ALL로 추출한것을 저장하는 콜백함수
     * @param dcBoardsAndComments
     */
    public void saveResultFromAll(DcBoardsAndComments dcBoardsAndComments) {
        LocalDateTime startTime = LocalDateTime.now();
        List<DcBoard> boards = dcBoardsAndComments.getBoards();
        List<DcComment> comments = dcBoardsAndComments.getComments();

        List<kr.granblue.gbfsearchback.domain.DcBoard> domainBoards = boards.stream().map(DcBoardMapper::fromDto).collect(Collectors.toList());
        List<kr.granblue.gbfsearchback.domain.DcComment> domainComments = comments.stream().map(DcCommentMapper::fromDto).collect(Collectors.toList());

        boardService.saveBoards(domainBoards);
        commentService.saveComments(domainComments);

        long savedBoardCounter = boardService.countByCreatedAtAfter(startTime);
        long savedCommentCounter = commentService.countByCreatedAtAfter(startTime);
        log.info("[SAVE RESULT] boards.size = {}, comments.size = {}, savedBoardCounter = {}, savedCommentCounter = {}", boards.size(), comments.size(), savedBoardCounter, savedCommentCounter);
    }
    
    /**
     * ScrapingOption.List 로 추출한것 저장하는 콜백함수
     * @param dcBoardsAndComments
     */
    public void saveResultFromListPage(DcBoardsAndComments dcBoardsAndComments) {
        LocalDateTime startTime = LocalDateTime.now();
        List<DcBoard> boards = dcBoardsAndComments.getBoards();

        List<kr.granblue.gbfsearchback.domain.DcBoard> domainBoards = boards.stream().map(board -> DcBoardMapper.fromDto(board)).collect(Collectors.toList());

        boardService.saveBoards(domainBoards);

        long savedBoardCounter = boardService.countByCreatedAtAfter(startTime);
        log.info("[SAVE RESULT] boards.size = {} savedBoardCounter = {}, ", boards.size(), savedBoardCounter);
    }

    /**
     * ScrapingOption.LISTPAGE 로 추출한것에 다시 ScrapingOption.ALL 로 추출한것 저장하는 콜백함수
     * @param dcBoardsAndComments
     */
    public void saveResultFromAllToListPage(DcBoardsAndComments dcBoardsAndComments) {
        LocalDateTime startTime = LocalDateTime.now();
        List<DcBoard> boards = dcBoardsAndComments.getBoards();
        List<DcComment> comments = dcBoardsAndComments.getComments();

        List<kr.granblue.gbfsearchback.domain.DcBoard> domainBoards = boards.stream().map(board -> DcBoardMapper.fromDto(board)).collect(Collectors.toList());
        List<kr.granblue.gbfsearchback.domain.DcComment> domainComments = comments.stream().map(comment -> DcCommentMapper.fromDto(comment)).collect(Collectors.toList());

        boardService.fillContent(domainBoards);
        commentService.saveComments(domainComments);

        long savedBoardCounter = boardService.countByCreatedAtAfter(startTime);
        long savedCommentCounter = commentService.countByCreatedAtAfter(startTime);
        log.info("[SAVE RESULT] boards.size = {}, comments.size = {}, savedBoardCounter = {}, savedCommentCounter = {}", boards.size(), comments.size(), savedBoardCounter, savedCommentCounter);

    }

}
