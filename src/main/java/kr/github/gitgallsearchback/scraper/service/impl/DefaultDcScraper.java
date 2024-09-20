package kr.github.gitgallsearchback.scraper.service.impl;


import com.google.common.base.Stopwatch;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import kr.github.gitgallsearchback.scraper.dto.*;
import kr.github.gitgallsearchback.scraper.enums.ScrapingOption;
import kr.github.gitgallsearchback.scraper.exception.RetryExceededException;
import kr.github.gitgallsearchback.scraper.extractor.BoardExtractor;
import kr.github.gitgallsearchback.scraper.extractor.CommentExtractor;
import kr.github.gitgallsearchback.scraper.service.DcScraper;
import kr.github.gitgallsearchback.scraper.service.test.WebDriverWaits;
import kr.github.gitgallsearchback.scraper.util.ContentCleaner;
import kr.github.gitgallsearchback.scraper.util.WebDriverUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class DefaultDcScraper implements DcScraper {
    private final CommentExtractor commentExtractor;
    private final BoardExtractor boardExtractor;
    private Page browserPage;
    private Executor executor;

    private ScrapingOption scrapingOption;

    public DefaultDcScraper(BoardExtractor boardExtractor, CommentExtractor commentExtractor) {
        this.boardExtractor = boardExtractor;
        this.commentExtractor = commentExtractor;

        executor = ForkJoinPool.commonPool(); // default CompletableFuture executor

        scrapingOption = ScrapingOption.ALL;
        browserPage = null;
    }

    /* properties */
    // 스크래핑 할 url, 가변 uri
    private String baseUrl = "http://gall.dcinside.com";
    private String galleryListUri = "/board/lists/";
    private String minorGalleryListUri = "/mgallery/board/lists/";
    private String galleryViewUri = "/board/view/"; // 안씀
    private String galleryNameParameterPrefix = "?id="; // 반드시 첫번째로 사용하는 갤러리 이름 파라미터
    private String galleryNameParameter = ""; // 반드시 첫번째로 사용하는 갤러리 이름 파라미터
    private String pageParameter = "&page="; // 페이징 파라미터
    private long listNum = 100; // 한 페이지당 글 갯수 (30, 50, 100)
    private String listNumParameter = "&list_num=" + listNum; // 한 페이지당 글 갯수
    private String listUrlAlter = "https://gall.dcinside.com/board/lists/?id=granblue&list_num=100&sort_type=N&search_head=&page=";

    private String searchParameter = "&s_type=search_subject_memo&s_keyword="; // 검색 파라미터(제목+내용)
    private String boardListSelector = ".gall_list";
    private String boardListItemSelector = "tbody tr";
    private String boardHrefSelector = ".gall_tit>a";
    private String boardViewSelector = ".gallery_view";

    private String boardViewSelectorAlter = "#container";
    //    private String boardViewSelectorAlter = ".minor_view"; // minorgallery only?
    private String boardViewContentSelector = ".write_div"; // 글 내용 로드 기다릴때 사용
    private String commentListSelector = ".cmt_list";
    private String commentListItemSelector = ".cmt_list>li";
    private long maxRetryCount = 5; // 페이지 열기 및 파싱 실패시 최대 재시도 카운트

    public void setExecutor(Executor executor) {
        if (executor == null) {
            this.executor = ForkJoinPool.commonPool();
        } else {
            this.executor = executor;
        }
    }

    public void setScrapingOption(ScrapingOption scrapingOption) {
        this.scrapingOption = scrapingOption;
    }

    public DcBoardsAndComments start(ScrapeRequest scrapeRequest) {
        long startPage = scrapeRequest.getStartPage();
        long endPage = scrapeRequest.getEndPage();
        this.galleryNameParameter = scrapeRequest.getGalleryId();

        LocalDateTime startTime = LocalDateTime.now();
        ScrapeStatus scrapeStatus = new ScrapeStatus(startPage, endPage, startTime);

        List<ScrapeFailure> failures = new ArrayList<>();
        DcBoardsAndComments scrapedContents = null;

        try {
            // 스크래핑
            ScrapeResult scrapeResult = scrape(startPage, endPage, scrapingOption);

            //스크래핑 결과 처리
            scrapedContents = scrapeResult.getDcBoardsAndComments();
            failures = scrapeResult.getFailure();
            scrapeStatus.syncScrapeStatus(scrapeResult.getLoggingParams());

        } catch (Exception e) {
            log.error("\n[START ERROR] DcScraper.start() e = {}", e.getMessage());
        } finally {
            // 종료 로깅
            scrapeStatus.end();
            completeLogging(scrapeStatus, failures);
        }
        return scrapedContents;
    }

    public void startWithCallback(ScrapeRequest scrapeRequest, Consumer<DcBoardsAndComments> callback) {
        long startPage = scrapeRequest.getStartPage();
        long endPage = scrapeRequest.getEndPage();
        long interval = scrapeRequest.getInterval();
        this.galleryNameParameter = scrapeRequest.getGalleryId();

        // 로깅용
        LocalDateTime startTime = LocalDateTime.now();
        ScrapeStatus scrapeStatus = new ScrapeStatus(startPage, endPage, startTime);
        List<ScrapeFailure> failuresTotal = new ArrayList<>();

        try {
            long intervalStartPage = startPage; // 각 구간별 시작페이지
            long intervalEndPage = startPage + interval - 1; // 각 구간별 끝페이지
            List<CompletableFuture<Void>> callbackFutures = new ArrayList<>(); // 콜백 대기 리스트

            while (intervalStartPage <= endPage) {
                // 구간 끝페이지가 전체 스크래핑 끝 페이지보다 클경우 조정
                intervalEndPage = Math.min(intervalEndPage, endPage);

                // 스크래핑
                ScrapeResult scrapeResult = scrape(intervalStartPage, intervalEndPage, scrapingOption);

                // 결과
                DcBoardsAndComments scrapedContents = scrapeResult.getDcBoardsAndComments();
                LoggingParams loggingParams = scrapeResult.getLoggingParams();
                List<ScrapeFailure> failures = scrapeResult.getFailure();

                // 로깅용 카운터들 갱신
                scrapeStatus.syncScrapeStatus(loggingParams);

                // 로깅용 실패결과 모음
                failuresTotal.addAll(failures);

                // 콜백 실행
                if (interval > 0 && callback != null) {
                    log.info("\n[START WITH CALLBACK] scraping executed callback, start = {}, end = {} interval = {}", intervalStartPage, intervalEndPage, interval);
                    callbackFutures.add(CompletableFuture.runAsync(() -> callback.accept(scrapedContents), executor));
                }

                // 구간시작과 구간끝 증가
                intervalStartPage += interval;
                intervalEndPage += interval;
            } // while
            
            // 콜백 대기
            if (!callbackFutures.isEmpty()) {
                CompletableFuture.allOf(callbackFutures.toArray(new CompletableFuture[0])).join(); // 콜백 전체 대기
            }
            
        } catch (Exception e) {
            log.error("\n[START WITH CALLBACK ERROR] DcScraper.startWithCallback() e = {}", e.getMessage());
        } finally {
            // 종료 로깅
            scrapeStatus.end();
            completeLogging(scrapeStatus, failuresTotal);
        }
    }

    protected ScrapeResult scrape(long startPage, long endPage, ScrapingOption scrapingOption) {
        log.info("\n[SCRAPER] Start scraping from {} to {}", startPage, endPage);

        List<DcBoard> resultBoards = new ArrayList<>(); // 반환할 글 
        List<DcComment> resultComments = new ArrayList<>(); // 반환할 댓글
        int resultDeletedCommentCount = 0; // LoggingParam '삭제된 댓글' 갯수 합
        long resultCommentCntFromBoard = 0; // LoggingParam (댓글수) 갯수 합
        List<ScrapeFailure> failures = new ArrayList<>(); // 반환할 글 일부 누락된 페이지 정보

        // 스크래핑 시작, 끝 페이지
        Long pageNum = startPage;
        Long maxPageNum = endPage;

        // 재시도 카운터
        long retryCounter = 0; // 글 리스트, 글 상세 페이지 요소 로드 실패시 ++, maxRetryCounter 초과하면 RetryExceededException 발생
        int trElementsReloadCounter = 0; // 글 리스트 요소 누락시 ++, 3회까지 재시도 후 그대로 진행

        // 페이지 로깅용 카운터
        long addedBoardCount = 0; // resultBoards 에 추가된 글 수
        long addedBoardCommentCntTotal = 0; // resultBoards 에 board.getCommentCnt 총합
        long addedCommentCount = 0; // resultComments 에 추가된 댓글 수
        int addedDeletedCommentCount = 0; // resultComments 에 추가된 '삭제된 댓글' 갯수

        // 개발용 카운터
        long cutCounter = 1L; // 개발용 단건 카운트 컷 1부터 N 까지 N 회

        // 브라우저 기동
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setArgs(List.of(
                                    "--no-sandbox",
                                    "--disable-setuid-sandbox",
                                    "--disable-gl-drawing-for-tests",
                                    "--blink-settings=imagesEnabled=false"
                            )
                    ));
            browserPage = browser.newPage();
            browserPage.setDefaultTimeout(2000);

            while (pageNum <= maxPageNum) {
                // 글 리스트 페이지 URL 설정
                String executeUrl = baseUrl + minorGalleryListUri + galleryNameParameterPrefix + galleryNameParameter + pageParameter + pageNum + listNumParameter;
                // 글 리스트 페이지 접속 및 파싱
                Elements trElements = openListPageAndParse(executeUrl);

                // 글 리스트 페이지 로드 실패
                if (trElements == null) {
                    if (++retryCounter > maxRetryCount) {
                        throw new RetryExceededException("retryCounter exceeded, stop scraping, retryCounter = " + retryCounter +
                                " Exception pageNum = " + pageNum);
                    }
                    continue; // 현재 페이지 재시도
                }
                retryCounter = 0; // 성공시 초기화

                // 글 리스트 페이지 갯수 누락시 재시도
                if (trElements.size() < listNum && trElementsReloadCounter < 3) {
                    log.warn("\n[SCRAPER] listContinue trElements.size = {}, pageNum = {} ", trElements.size(), pageNum);
                    trElementsReloadCounter++;
                    continue; // 현재 페이지 재시도
                }

                // 글 리스트 에서 글 마다 내용 추출
                List<String> hrefs = new ArrayList<>(); // 글의 href 저장할 리스트
                List<DcBoard> extractedBoards = new ArrayList<>(); // 글 리스트에서 추출한 DcBoard 리스트
                for (Element trElement : trElements) {
                    DcBoard extractingBoard = boardExtractor.extractFromListPage(trElement);
                    if (extractingBoard.getDcNum() == -1) {
                        continue; // 공지, AD, 제휴글 등의 이유로 건너뛰어짐
                    } else {
                        // a 태그에서 글 href 추출 (/board/view/?id=granblue&no=4803525&page=1)
                        hrefs.add(trElement.select(boardHrefSelector).attr("href"));
                        extractedBoards.add(extractingBoard);
                    }
                }
                resultBoards.addAll(extractedBoards);
                addedBoardCount += extractedBoards.size();

                // 리스트 url 3회이상 실패후 진행시 누락진행으로 실패리스트에 추가
                if (trElementsReloadCounter >= 3) {
                    log.warn("\n[BOARD.PROPERLY] boardNotAdded properly. listnum = {} addedBoardCount = {}, executeUrl = {}", listNum, addedBoardCount, executeUrl);
                    failures.add(new ScrapeFailure(executeUrl, trElements.size(), extractedBoards.size(), pageNum));
                }

                // 글 리스트에서 글 하나하나 순회시작
                int boardIndex = 0; // 글 순회 인덱스.
                while (boardIndex < extractedBoards.size()) {
                    if (scrapingOption != ScrapingOption.ALL && scrapingOption != ScrapingOption.VIEWPAGE) break;

                    // 추출중인 글과 해당 글의 href
                    DcBoard extractingBoard = extractedBoards.get(boardIndex);
                    String href = hrefs.get(boardIndex);

                    // 글 상세 페이지 접속 및 파싱
                    executeUrl = baseUrl + href;
                    Element mainElement = openViewPageAndParse(executeUrl);

                    // 글 상세 페이지 로드 실패
                    if (mainElement == null) {
                        if (++retryCounter > maxRetryCount) {
                            throw new RetryExceededException("retryCounter exceeded, stop scraping, retryCounter = " + retryCounter +
                                    " Exception pageNum = " + pageNum);
                        }
                        continue; // 현재 글 재시도
                    }
                    retryCounter = 0; // 성공시 초기화

                    // 상세 페이지에서 내용 추출 후 DcBoard 객체에 저장하여 완성
                    String rawContent = boardExtractor.extractContentFromViewPage(mainElement);
                    extractingBoard.setContent(rawContent);

                    // 완성 후 로깅
                    String cleanContent = ContentCleaner.cleanContent(rawContent);
                    log.info("\n================================================================\n" +
                                    " title = \n" +
                                    "   {}\n" +
                                    " cleanContent = \n" +
                                    "   {}\n" +
                                    "================================================================\n",
                            extractingBoard.getTitle(),
                            cleanContent
                    );

                    // 다음 글로
                    boardIndex++;

                    // ScrapingOption.VIEWPAGE 종료지점 ============================================
                    if (scrapingOption != ScrapingOption.ALL) break;

                    // 상세 페이지에서 댓글 추출
                    List<DcComment> extractedComments = extractCommentsFromViewPage(extractingBoard.getDcNum(), mainElement);
                    // 완성된 List<DcComment> 객체 저장
                    resultComments.addAll(extractedComments);

                    // '삭제된 댓글' 갯수 ( regDate = null )
                    int deletedCommentCount = extractedComments.stream().filter(dcComment -> dcComment.getRegDate() == null).collect(Collectors.toList()).size();

                    addedCommentCount += extractedComments.size();
                    addedDeletedCommentCount += deletedCommentCount;
                    addedBoardCommentCntTotal += extractingBoard.getCommentCnt();

                    // 댓글 개수 체크 (댓글돌이는 extractingBoard.getCommentCnt() 숫자에서 제외됨, 삭제된 댓글 갯수는 빼줘야됨)
                    if (extractingBoard.getCommentCnt() != extractedComments.size() - deletedCommentCount) {
                        log.warn("\n[COMMENT.PROPERLY] comment not extracted properly extractingBoard.getCommentCnt() = {} extractedComments.size() = {}, deletedCommentCount = {}, executeUrl={}", extractingBoard.getCommentCnt(), extractedComments.size(), deletedCommentCount, executeUrl);
                        extractedComments.forEach(dcComment -> log.warn("dcComment = id = {} content = {}", dcComment.getId(), ContentCleaner.cleanContent(dcComment.getContent())));
                    }

                    // 글 하나만 하고 끝내기
//                break;

//                     컷 카운터로 컷
                if (cutCounter >= 5) {
                    resultBoards = resultBoards.subList(0, (int) cutCounter);
                    break;
                }
                cutCounter++;

                } // for trElements

                log.info("\n[DCSCRAPER - PAGE END] =================================================================\n" +
                                "pageNum = {}/{}]\n" +
                                "addedBoardCount = {} trElements.size = {} \n" +
                                "addedCommentCount = {} \n" +
                                "addedBoardCommentCntTotal = {} == calculated expect = {} \n" +
                                "===========================================================================================",
                        pageNum, maxPageNum,
                        addedBoardCount, trElements.size(),
                        addedCommentCount,
                        addedBoardCommentCntTotal, addedCommentCount - addedDeletedCommentCount);

                // 다음 페이지로
                pageNum++;

                // 카운터 초기화
                addedBoardCount = 0;
                addedCommentCount = 0;
                resultCommentCntFromBoard += addedBoardCommentCntTotal;
                addedBoardCommentCntTotal = 0;
                resultDeletedCommentCount += addedDeletedCommentCount;
                addedDeletedCommentCount = 0;

            }// for pageNum

        } catch (RetryExceededException e) {
            log.error("\n[SCRAPER] RetryExceededException e = {}", e.getMessage());
        } catch (Exception e) {
            log.error("\n[SCRAPER] Exception e = {}", e.getMessage());
        } // try(PlayWright) end

        return ScrapeResult.builder()
                .dcBoardsAndComments(new DcBoardsAndComments(resultBoards, resultComments))
                .failure(failures)
                .loggingParams(LoggingParams.builder()
                        .scrapedPageCnt(maxPageNum - startPage + 1)
                        .boardPerPage(listNum)
                        .scrapedBoardCount(resultBoards.size())
                        .scrapedCommentCnt(resultComments.size())
                        .scrapedDeletedCommentCnt(resultDeletedCommentCount)
                        .scrapedBoardCommentCntTotal(resultCommentCntFromBoard)
                        .build()
                ).build();
    }

    /**
     * WebDriver 를 통해 글 리스트 페이지를 열고, 파싱하여 글 리스트 Elements 를 반환함
     *
     * @param executeUrl
     * @return 글 리스트의 tr 요소로 구성된 Elements 객체, 실패시 size = 0 반환
     */
    protected Elements openListPageAndParse(String executeUrl) {
        Elements results = new Elements();
        try {
            log.info("\n[SCRAPER] listPage executeUrl = {}", executeUrl);
            browserPage.navigate(executeUrl,
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT));

            ElementHandle gallList = browserPage.waitForSelector(boardListSelector);
            String gallListOuterHtml = (String) gallList.evaluate("e => e.outerHTML");

            Element tableElement = Jsoup.parse(gallListOuterHtml);
            results = tableElement.select(boardListItemSelector);
        } catch (Exception e) {
            log.error("\n[ERROR] page.navigate(executeUrl);  executeUrl = {} e.name = {} ", executeUrl, e.getClass().getName());
        }
        return results;
    }

    /**
     * WebDriver 를 통해 글 상세 페이지를 열고, 파싱하여 본문 + 댓글 리스트를 포함하는 main 요소를 반환함
     *
     * @param executeUrl
     * @return 본문 + 댓글 리스트를 포함하는 main 요소 Element 객체, 실패시 null 반환
     */
    protected Element openViewPageAndParse(String executeUrl) {
        Stopwatch stopwatch; // 개발용 스톱워치
        Element result; // 상세페이지의 main 요소 담길 변수
        try {
            log.info("\n[SCRAPER] viewPage executeUrl = {}", executeUrl);
            stopwatch = Stopwatch.createStarted();
            browserPage.navigate(executeUrl,
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT));
            ElementHandle mainContainer = browserPage.waitForSelector(boardViewSelectorAlter,
                    new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED));
            String mainContainerHtml = (String) mainContainer.evaluate("e => e.outerHTML");

            // 파싱
            result = Jsoup.parse(mainContainerHtml);
            // 가끔 로드 느려서 .write_div = null 인경우 있어서 추가
            result = result.select(boardViewContentSelector).isEmpty() ? null : result;

            stopwatch.stop();
            log.info("\n[STOPWATCH] get mainElement from page : stopwatch.elapsed = {} : {}", stopwatch.elapsed().toSeconds(), stopwatch.elapsed().toMillisPart());
        } catch (Exception e) {
            log.error("\n[ERROR] page.navigate(executeUrl);  executeUrl = {} e.name = {}", executeUrl, e.getClass().getName());
            result = null;
        }
        return result;
    }

    /**
     * 본문내용 + 댓글 요소를 포함하는 mainElement 로부터 댓글 리스트를 추출하여 반환
     *
     * @param mainElement
     * @param dcNum
     * @return
     */
    public List<DcComment> extractCommentsFromViewPage(long dcNum, Element mainElement) {
        List<DcComment> results = new ArrayList<>();

        // 댓글 리스트 ul 요소 추출
        Element ulElementComment = mainElement.select(commentListSelector).first();
        // 댓글 있으면 내용 추출
        if (ulElementComment != null) {
            // 댓글 리스트 내부 li 요소 (댓글 + 답글)
            Elements liElementsComment = ulElementComment.select(commentListItemSelector);
            Element liElementPrev = null; // 답글의 target 설정을 위한 직전 반복 댓글
            for (Element liElement : liElementsComment) {
                List<DcComment> extractedComments = commentExtractor.extractCommentAndReply(dcNum, liElement, liElementPrev);
                results.addAll(extractedComments);
                liElementPrev = liElement;
            }
        }
        return results;
    }

    protected void completeLogging(ScrapeStatus scrapeStatus, List<ScrapeFailure> failures) {
        LocalDateTime startTime = scrapeStatus.getStartTime();
        LocalDateTime endTime = scrapeStatus.getEndTime();
        Duration duration = Duration.between(startTime, endTime);

        long endPage = scrapeStatus.getEndPage();
        long startPage = scrapeStatus.getStartPage();
        long boardPerSecond = scrapeStatus.getTotalBoardCnt() == 0 ? 0 : duration.getSeconds() / scrapeStatus.getTotalBoardCnt();
        long executePageCount = scrapeStatus.getExecutedPageCount();
        long secondsPerPage = executePageCount == 0 ? 0 : duration.getSeconds() / (executePageCount);

        log.info("\n [SCRAPE COMPLETE] ==================================================\n" +
                        "  elaspedTime = {}h : {}m : {}s : {}millis, \n" +
                        "  time per board = {}s / board, per Page = {}s / page \n" +
                        "  startedFrom = {}, endTime = {}\n" +
                        "  page = {} ~ {} pageCount = {}\n " +
                        "  expectedBoardCounter = {} expectedCommentCounter = {} \n" +
                        "  scrapedBoardCounter = {}, scrapedCommentCounter = {}\n" +
                        " ======================================================================",
                duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart(),
                boardPerSecond, secondsPerPage,
                startTime, endTime,
                startPage, endPage, executePageCount,
                (executePageCount) * 100,
                scrapeStatus.getTotalDeletedCommentCnt() + scrapeStatus.getTotalCommentCntFromBoard(),
                scrapeStatus.getTotalBoardCnt(), scrapeStatus.getTotalCommentCnt());

        if (!failures.isEmpty()) {
            log.warn("\n[SCRAPE COMPLETE - FAILURES] failures.size = {} =====================================================", failures.size());
            failures.forEach(failure -> log.warn("failure = {}", failure));
            log.warn("\n=====================================================================================================");
        }
    }
}



