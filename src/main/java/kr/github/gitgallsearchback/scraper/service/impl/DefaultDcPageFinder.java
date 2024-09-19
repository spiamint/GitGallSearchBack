package kr.github.gitgallsearchback.scraper.service.impl;

import com.google.common.base.Stopwatch;
import com.microsoft.playwright.*;
import kr.github.gitgallsearchback.scraper.util.WebDriverUtil;
import kr.github.gitgallsearchback.scraper.dto.DcBoard;
import kr.github.gitgallsearchback.scraper.exception.RetryExceededException;
import kr.github.gitgallsearchback.scraper.extractor.BoardExtractor;
import kr.github.gitgallsearchback.scraper.service.DcPageFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URLEncoder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class DefaultDcPageFinder implements DcPageFinder {

    private final BoardExtractor boardExtractor;
    private Page browserPage;

    // 스크래핑 할 url, 가변 uri
    private String baseUrl = "http://gall.dcinside.com";
    private String galleryListUri = "/board/lists/";
    private String minorGalleryListUri = "/mgallery/board/lists/";
    private String galleryViewUri = "/board/view/"; // 안씀

    private String galleryNameParameterPrefix = "?id="; // 반드시 첫번째로 사용하는 갤러리 이름 파라미터
    private String pageParameter = "&page="; // 페이징 파라미터
    private String searchParameter = "&s_type=search_subject_memo&s_keyword="; // 검색 파라미터(제목+내용)

    private long maxRetryCount = 3;

    public void findFirstPageByDate(LocalDateTime inputDateTime, String galleryId, boolean isMinorGallery) {
        // 드라이버 켜기
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
            browserPage.setDefaultTimeout(3000);

            // 검색 페이지로 이동을 위한 url 설정
            String searchKeyword = "p"; // 글 내부에 p 요소 있으면 전부 검색됨 (div 등으로 확인)
            String encodedKeyword = URLEncoder.encode(searchKeyword);
            String galleryUri = isMinorGallery ? minorGalleryListUri : galleryListUri;
            String urlPreFix = baseUrl + galleryUri + galleryNameParameterPrefix + galleryId;

            String executeUrl = urlPreFix + searchParameter + encodedKeyword;

            // 검색할 날짜의 년, 월, 일을 각각 문자열로 변환, 0붙임
            String inputYear = "" + inputDateTime.getYear();
            String inputMonth = inputDateTime.getMonthValue() > 10 ? "" + inputDateTime.getMonthValue() : "0" + inputDateTime.getMonthValue();
            String inputDay = inputDateTime.getDayOfMonth() > 10 ? "" + inputDateTime.getDayOfMonth() : "0" + inputDateTime.getDayOfMonth();

            long retryCounter = 0;
            while (retryCounter < maxRetryCount) {

                Stopwatch stopwatch = Stopwatch.createStarted();

                try {

                    // 검색 페이지 접속
                    log.info("[DRIVER] findPage Initial executeUrl = {}", executeUrl);
                    browserPage.navigate(executeUrl);

                    // 빠른 이동 버튼 클릭
                    ElementHandle moveButton = browserPage.waitForSelector(".bottom_movebox>button");
                    moveButton.click();
                    log.info("빠른이동 버튼 클릭");

                    // 작성일 입력 클릭
                    ElementHandle calendarInput = browserPage.waitForSelector(".moveset #calendarInput");
                    calendarInput.click();
                    log.info("작성일 입력 클릭");

                    // 이전달 버튼의 onclick 메서드 수정후 클릭
                    ElementHandle prevMonthBtn = browserPage.waitForSelector(".btn_prev_month");
                    prevMonthBtn.evaluate("e => e.setAttribute('onclick', 'openCalendar(" + inputMonth + "," + inputYear + ")')");
                    prevMonthBtn.click();
                    log.info("이전 달 버튼 클릭");

                    // 이전달 버튼 클릭 후 잘 이동했는지 확인
//                String currentYear = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".calendar_wrap .tit_box #calendarYear"))).getText();
//                String currentMonth = webDriver.findElement(By.cssSelector(".calendar_wrap .tit_box #calendarMonth")).getText();
//                log.info("currentYear = {}, currentMonth = {}", currentYear, currentMonth);

                    // 날짜 박스 클릭
                    String inputDateTimeString = inputYear + "-" + inputMonth + "-" + inputDay;
                    ElementHandle dayBtn = browserPage.waitForSelector("#calendar_day td[data-day='" + inputDateTimeString + "']");
                    dayBtn.click();
                    log.info("달력의 날짜 버튼 클릭");

                    // 빠른이동 내부 확인버튼 클릭하여 페이지 이동
                    browserPage.waitForSelector(".fast_move_btn").click();
                    log.info("작성일 검색 페이지 이동, 현재페이지 = {}", browserPage.url());

                    // 디시에서 빠른이동 검색결과 없음 알림 뜬 경우 break
                    try {
                        ElementHandle noBoardInDayErrorElement = browserPage.waitForSelector(".hint_txt.font_red");
                        String errorMessageFromDc = noBoardInDayErrorElement.textContent();
                        log.info("해당 일에 작성된 글 없음. DC 알림내용 = {}", errorMessageFromDc);
                        break;
                    } catch (Exception e) {
                        log.info("해당 일에 작성된 글 있음. 정상진행");
                    }

                    // 타겟 날짜의 페이지 도달 함
                    ElementHandle targetPageGallList = browserPage.waitForSelector(".gall_list");
                    // 타겟 날짜의 페이지에서 리스트중 첫번째 글 파싱 및 dcNum 추출
                    DcBoard targetDcBoard = parseAndFindFirstDcBoard(targetPageGallList);
                    Long targetDcNum = targetDcBoard.getDcNum();
                    log.info("해당 일에 작성된 마지막 글 추출");

                    // 갤러리 첫 페이지로 이동 및 첫 표시 글 dcNum 파싱 및 추출
                    executeUrl = urlPreFix + pageParameter + 1;
                    browserPage.navigate(executeUrl);
                    ElementHandle firstPageGallList = browserPage.waitForSelector(".gall_list");
                    DcBoard firstDcBoard = parseAndFindFirstDcBoard(firstPageGallList);
                    Long firstPageDcNum = firstDcBoard.getDcNum();
                    log.info("갤러리 맨 첫페이지로 이동 및 최신 글 추출");

                    // 디시 아이디의 차를 구하여 표시 글 갯수(50개) 로 나눔
                    // 이 lastPage 는 삭제된 글이 0 이라고 가정한 수치이므로 이로부터 삭제된 글을 감안하여 페이지를 당겨가며 확인
                    Long dcNumDiff = firstPageDcNum - targetDcNum;
                    Long lastPage = dcNumDiff / 50;

                    // 1. 먼저 lastPage 부터 1000페이지씩 당겨 inputTime 보다 미래 글 이 포함된 페이지를 찾는다.
                    // 2. 해당 페이지를 searchStartPage 로 두고, 직적 페이지를 searchEndPage 로 둔다. 둘의 차는 1000페이지
                    // 3. inputTime 의 글은 두 페이지 사이에 있다.

                    log.info("탐색을 위한 시작 페이지 찾기 시작");
                    LocalDateTime lastPageFirstDcBoardTime;
                    long searchEndPage = lastPage; // 밑에 탐색에서 끝페이지로 사용할 페이지. (가장 과거)
                    int lastPageWhileIndex = 0;
                    do {
                        searchEndPage = lastPage;
                        lastPage -= 1000;
                        // 끝페이지의 첫글을 찾아 작성일자 대조 후 targetTime 보다 과거글이면 1000페이지씩 당김
                        executeUrl = urlPreFix + pageParameter + lastPage;
                        browserPage.navigate(executeUrl);
                        ElementHandle lastPageGallList = browserPage.waitForSelector(".gall_list");
                        DcBoard lastPageFirstDcBoard = parseAndFindFirstDcBoard(lastPageGallList);
                        lastPageFirstDcBoardTime = lastPageFirstDcBoard.getRegDate();
                        log.info("페이지 당기는 중, 현재페이지 = {}", lastPage);
                        if (lastPage < 0) {
                            lastPage = 1L;
                            break;
                        } // 페이지 당기는데 음수되면 1페이지로 조정 및 종료
                    } while (lastPageFirstDcBoardTime.isBefore(inputDateTime)); // 당겨온 페이지의 첫글이 inputDateTime 보다 미래 글이면 stop
                    long searchStartPage = lastPage; // 밑의 탐색에서 시작 페이지로 사용할 페이지. (가장 미래)
                    log.info("탐색을 위한 페이지 찾기 완료. 탐색 시작 페이지 = {}, 탐색 끝 페이지 = {}", searchStartPage, searchEndPage);

                    // 글은 lastPage 와 prevPage 사이에 있음
                    long middlePage = (searchStartPage + searchEndPage) / 2;
                    long prevMiddelPage = 0; // 중복 탐색을 방지하기 위한 직전 middlePage

                    String currentUrl;
                    int index = 0;
                    LocalDateTime middlePageFirstDcBoardTime = null;
                    log.info("작성일 페이지 탐색 시작");
                    // 이진탐색 (최대 10회)
                    while (index < 10) {
                        log.info("작성일 페이지 탐색중 {}/10", index + 1);
                        // 중간 페이지 계산
                        middlePage = (searchStartPage + searchEndPage) / 2;

                        if (middlePage == prevMiddelPage) { // 동일 페이지 재탐색 break
                            log.info("동일 페이지 탐색시도 에 따른 탐색 종료. 결과페이지 = {}", middlePage);
                            break;
                        }

                        // 중간 페이지 접속
                        executeUrl = urlPreFix + pageParameter + middlePage;
//                    log.info("binarySearching else executeUrl = {}", executeUrl);
                        browserPage.navigate(executeUrl);

                        // 중간 페이지 최상단 글 추출
                        ElementHandle middlePageGallList = browserPage.waitForSelector(".gall_list");
                        DcBoard middlePageDcBoard = parseAndFindFirstDcBoard(middlePageGallList);
                        middlePageFirstDcBoardTime = middlePageDcBoard.getRegDate();

                        if (Duration.between(middlePageFirstDcBoardTime, inputDateTime).abs().toMinutes() <= 1) {
                            //  inputTime 과의 차가 1분 이하 이면 break
                            break;
                        } else {
                            // 중간 페이지와 inputTime 의 차가 1분 이내가 아님
                            log.info("Searching else searchStartPage = {}, searchEndPage = {}, middlePage = {}, middlePageFirstDcBoardTime = {} executeUrl = {}",
                                    searchStartPage, searchEndPage, middlePage, middlePageFirstDcBoardTime, executeUrl);
                            prevMiddelPage = middlePage; // 중복 탐색 방지를 위한 이전 middlePage 저장

                            if (middlePageFirstDcBoardTime.isAfter(inputDateTime)) {
                                // 중간 페이지 첫글 이 inputTime 보다 미래
                                searchStartPage = middlePage;
                            } else {
                                // 중간 페이지 첫글 이 inputTime 보다 미래
                                searchEndPage = middlePage;
                            }
                        }
                        index++;
                    }

                    // 성공 종료 로깅
                    log.info("[SEARCH] End ==============================================");
                    log.info("find page number = {}", middlePage);
                    log.info(" -> check URL = {}", executeUrl);
//                log.info("index = {}\n" +
//                        "Duration minute = {}m middlePageFirstDcBoardTime = {}, inputDateTime = {}\n" +
//                        "searchStartPage = {} searchEndPage = {}\n" +
//                        "middlePage = {} confirmUrl = {}\n" +
//                        "[BINARYSEARCH] End =================================",
//                        index,
//                        Duration.between(middlePageFirstDcBoardTime, inputDateTime).abs().toMinutes(), middlePageFirstDcBoardTime, inputDateTime,
//                        searchStartPage, searchEndPage, middlePage,
//                        executeUrl);

                    Duration duration = stopwatch.stop().elapsed();
                    log.info("[STOPWATCH] findFirstPageByDate : stopwatch.elapsed = {} : {}", duration.toSeconds(), duration.toMillisPart());

                    // 성공시 바로 종료
                    break;

                } catch (ElementClickInterceptedException e) {
                    // 마이너 갤러리 알림 팝업 발생시 닫고 재시도
                    log.error("[ERROR] ElementClickInterceptedException : {}", e.getMessage());
                    closeMinorGalleryPopup(executeUrl);
                    retryCounter++;
                    // 재시도 횟수 초과 -> Exception 발생 및 스크래핑 종료
                    if (retryCounter > maxRetryCount) {
                        throw new RetryExceededException("retryCounter exceeded, stop scraping, retryCounter = {}" + retryCounter, e);
                    }
                } catch (Exception e) {
                    log.error("[ERROR] webDriver.get(executeUrl);  executeUrl = {} e.name = {}", executeUrl, e);
                    retryCounter++;
                    // 재시도 횟수 초과 -> Exception 발생 및 스크래핑 종료
                    if (retryCounter > maxRetryCount) {
                        throw new RetryExceededException("retryCounter exceeded, stop scraping, retryCounter = {}" + retryCounter, e);
                    }
                } finally {
                }
            }
            log.info("최종 종료;");
        }
    }


    public DcBoard parseAndFindFirstDcBoard(ElementHandle gallListElement) {
        Elements trElements = Jsoup.parse((String) gallListElement.evaluate("e => e.outerHTML")).select("tbody tr");
        DcBoard result = null;
        for (Element trElement : trElements) {
            DcBoard extractedBoard = boardExtractor.extractFromListPage(trElement);
            if (extractedBoard.getDcNum() != -1) {
                result = extractedBoard;
                break;

            }
        }
        return result;
    }

    public void closeMinorGalleryPopup(String executeUrl) {
        browserPage.navigate(executeUrl);
        browserPage.waitForSelector("#closure-popup .btn_bottom.fl").click();
    }


}
