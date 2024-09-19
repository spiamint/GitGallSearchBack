package kr.github.gitgallsearchback.service;

import kr.github.gitgallsearchback.domain.Board;
import kr.github.gitgallsearchback.domain.Comment;
import kr.github.gitgallsearchback.domain.mapper.BoardMapper;
import kr.github.gitgallsearchback.domain.mapper.CommentMapper;
import kr.github.gitgallsearchback.scraper.dto.*;
import kr.github.gitgallsearchback.scraper.enums.ScrapingOption;
import kr.github.gitgallsearchback.scraper.service.DcScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapingService {

    private final BoardService boardService;
    private final CommentService commentService;
    private final DcScraper dcScraper;

    /**
     * 갤러리 스크래핑
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

        dcScraper.setScrapingOption(ScrapingOption.ALL);
        dcScraper.startWithCallback(ScrapeRequest.of("github", startPage, endPage, interval), callback);

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

        List<Board> domainBoards = boards.stream().map(BoardMapper::fromDto).collect(Collectors.toList());
        List<Comment> domainComments = comments.stream().map(CommentMapper::fromDto).collect(Collectors.toList());

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

        List<Board> domainBoards = boards.stream().map(BoardMapper::fromDto).collect(Collectors.toList());

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

        List<Board> domainBoards = boards.stream().map(BoardMapper::fromDto).collect(Collectors.toList());
        List<Comment> domainComments = comments.stream().map(CommentMapper::fromDto).collect(Collectors.toList());

        boardService.fillContent(domainBoards);
        commentService.saveComments(domainComments);

        long savedBoardCounter = boardService.countByCreatedAtAfter(startTime);
        long savedCommentCounter = commentService.countByCreatedAtAfter(startTime);
        log.info("[SAVE RESULT] boards.size = {}, comments.size = {}, savedBoardCounter = {}, savedCommentCounter = {}", boards.size(), comments.size(), savedBoardCounter, savedCommentCounter);

    }

}
