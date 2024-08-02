package kr.granblue.seleniumcrawler.crawler;

import kr.granblue.seleniumcrawler.domain.DcBoard;
import kr.granblue.seleniumcrawler.domain.enums.SourceType;
import kr.granblue.seleniumcrawler.driver.WebDriverUtil;
import kr.granblue.seleniumcrawler.service.DcBoardEmbeddingService;
import kr.granblue.seleniumcrawler.service.DcBoardService;
import kr.granblue.seleniumcrawler.util.ContentCleaner;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@Getter
@RequiredArgsConstructor
public class DcCrawler {

    private WebDriver webDriver; // chromeDriver from WebDriverUtil
    private final EmbeddingClient embeddingClient;

    private final DcBoardService dcBoardService;


    public void crawlStart(long startPage, long endPage) {
        log.info("=====[CRAWL START]=====");
        // 시간 확인
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime;
        // 크롤링할 시작, 끝 페이지
        Long pageNum = startPage;
        Long maxPageNum = endPage;
        // 크롤링할 url, 가변 uri
        String baseUrl = "http://gall.dcinside.com";
        String appendUri = "/board/lists/?id=granblue&page=";
        // 카운터
        Long devCounter = 1L; // 개발용 단건 카운트 컷 1부터 N 까지 N 회
        Long crawledBoardCounter = 0L;
        Long savedBoardCounter = 0L;
        Long savedEmbeddingCounter = 0L;
        // 저장할 디씨글 리스트와 비동기 작업 리스트
        List<DcBoard> dcBoardsForSave = new ArrayList<>();
        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();

        // 크롬 드라이버 획득
        webDriver = WebDriverUtil.getChromeDriver();

        while (pageNum <= maxPageNum) {
            // 글 리스트 페이지 접속
            String executeUrl = baseUrl + appendUri + pageNum;
            try {
                log.info("opening list executeUrl = {}", executeUrl);
                webDriver.get(executeUrl);
            } catch (TimeoutException e) {
                log.error("[ERROR] webDriver.get(executeUrl);  executeUrl = {} TIMEOUT", executeUrl);
                continue; // 실패 시 페이지 변화없이 재로드
            } catch (Exception e) {
                log.error("[ERROR] webDriver.get(executeUrl); Thread.sleep() executeUrl = {} e = {}", executeUrl, e);
                break;
            }

            // 글 리스트 획득
            String listPageSource = webDriver.getPageSource();
            Document dcBoardListPageHtml = Jsoup.parse(listPageSource);
            Elements dcBoardListHtml = dcBoardListPageHtml.select(".ub-content");
//            log.info("dcBoardListHtml = {}", dcBoardListHtml);

            // 글마다 루프
            for (Element dcBoardListItem : dcBoardListHtml) {

                // 설문, AD, 공지 거름
                String gallNum = dcBoardListItem.select(".gall_num").text();
                long parsedGallNum;
                try {
                    parsedGallNum = Long.parseLong(gallNum);
                } catch (Exception e) {
                    // gall_num 이 숫자가 아님 -> 설문, AD, 공지
//                     log.error("[ERROR] parseLong(gallNum) : gallNum = {} e = {}", gallNum, e);
                    continue;
                }

                // 글에서 필요한 부분 추출
                Element titleElement = dcBoardListItem.select(".gall_tit>a").first();
                Element writerElement = dcBoardListItem.select(".gall_writer>.nickname").first();
//                log.info("titleElement = {}", titleElement);
//                log.info("writerElement = {}", writerElement);
                String gallTitle = titleElement.text();
                Element replyNumElement = titleElement.nextElementSibling();
                String commentCnt = replyNumElement != null ? // 댓글없으면 null 임
                        replyNumElement.select(".reply_num").text().replaceAll("[\\[\\]]", "") : // text = [1]
                        "0";
                String gallWriter = writerElement.attr("title");
                String gallDate = dcBoardListItem.select(".gall_date").attr("title");
                String gallCount = dcBoardListItem.select(".gall_count").text();
                String gallRecommend = dcBoardListItem.select(".gall_recommend").text();

                // 날짜 변환 형식 : 2024-07-19 12:05:43
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime convertedGallDate = LocalDateTime.parse(gallDate, formatter);

                // 글 저장용 객체 생성(내용제외)
                DcBoard dcBoard = DcBoard.builder()
                        .dcNum(parsedGallNum)
                        .title(gallTitle)
                        .writer(gallWriter)
                        .regDate(convertedGallDate)
                        .viewCnt(Long.parseLong(gallCount))
                        .recommendCnt(Long.parseLong(gallRecommend))
                        .commentCnt(Long.parseLong(commentCnt))
                        .sourceType(SourceType.DC)
                        .build(); // content, recommend null

                // a 태그에서 글 href 추출
                String href = dcBoardListItem.select(".ub-content a").attr("href");
                executeUrl = baseUrl + href;

                // 글 접속
                try {
                    log.info("opening board executeUrl = {}", executeUrl);
                    webDriver.get(executeUrl);
                    crawledBoardCounter++;
                } catch (TimeoutException e) {
                    log.error("[ERROR] webDriver.get(executeUrl);  executeUrl = {} TIMEOUT", executeUrl);
                    continue; // 실패 시 페이지 변화없이 재로드
                } catch (Exception e) {
                    log.error("[ERROR] webDriver.get(executeUrl); Thread.sleep() executeUrl = {} e = {}", executeUrl, e);
                    continue;
                }

                String boardPageSource = webDriver.getPageSource();
                Document dcBoardHtml = Jsoup.parse(boardPageSource);

                // 내용 추출
                Elements writeElement = dcBoardHtml.select(".write_div");
                String rawContent = writeElement.html();

                // 념글 확인
                Elements recommendBoxElement = dcBoardHtml.select(".btn_recommend_box");
                boolean isRecommended = !recommendBoxElement.select(".btn_recom_up.on").isEmpty(); // 념글버튼의 클래스에 .on 붙으면 념글

                // DcBoard 객체에 내용 추가
                dcBoard.setRecommended(isRecommended);
                dcBoard.setContent(rawContent);
                dcBoard.setCleanContent(ContentCleaner.cleanContent(rawContent));

                log.info("\n================================================================\n" +
                                "  executeUrl = [{}]\n" +
                                "  title = \n" +
                                "{}\n" +
                                "  cleanContent = \n" +
                                "{}\n" +
                                "================================================================\n",
                        executeUrl,
                        dcBoard.getTitle(),
                        dcBoard.getCleanContent()
                );

                // DB 저장을 위한 리스트에 추가
                dcBoardsForSave.add(dcBoard);

                // 하나만 하고 끝내기 ===============
//                break;

//                 여러개 하고 끝내기
                if (devCounter >= 3) break;
                devCounter++;

            } // for Element

            log.info("\n===================================================[pageNum = {}/{} END]" +
                            "===================================================",
                    pageNum, maxPageNum);

            // DB 저장용 리스트를 이용해 DB 저장 (비동기), 비동기 작업 리스트에 추가 (페이지 단위)
            completableFutures.add(dcBoardService.asyncSaveBoards(dcBoardsForSave.toArray(new DcBoard[0])));
            dcBoardsForSave.clear();

            pageNum++;
            devCounter = 1L;
        } // for pageNum


        // 웹 크롤링 종료
        webDriver.quit();
        endTime = LocalDateTime.now();
        log.info("\n  [CRAWL END] =======================================================\n" +
                        "  elaspedTime = {}min, startedFrom = {}, endTime = {}\n" +
                        "  crawledCounter = {} ",
                Duration.between(startTime, endTime).toMinutes(), startTime, endTime,
                crawledBoardCounter);

//         save 끝날때 까지 대기
        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();

        savedBoardCounter = dcBoardService.countByCreatedAtAfter(startTime);

        // 크롤링 완전 종료
        endTime = LocalDateTime.now();
        log.info("\n  [CRAWL COMPLETE] ==================================================\n" +
                        "  elaspedTime = {}min, startedFrom = {}, endTime = {}\n" +
                        "  crawledCounter = {}, savedBoardCounter = {}",
                Duration.between(startTime, endTime).toMinutes(), startTime, endTime,
                crawledBoardCounter, savedBoardCounter);
    }

}

/*
DC 크롤링 후 글 형태
select(.ub-content)
    <tr class="ub-content" data-no="1" data-type="icon_notice">
     <td class="gall_num">공지</td>
     <td class="gall_tit ub-word"><a href="/board/view/?id=granblue&amp;no=1&amp;page=1" view-msg=""> <em class="icon_img icon_notice"></em><b><b>그랑블루 판타지 갤러리 이용 안내</b></b></a> <a class="reply_numbox" href="https://gall.dcinside.com/board/view/?id=granblue&amp;no=1&amp;t=cv&amp;page=1"><span class="reply_num">[124]</span></a></td>
     <td class="gall_writer ub-writer" data-nick="운영자" data-uid="" data-ip="" data-loc="list"><b><b><b>운영자</b></b></b></td>
     <td class="gall_date" title="2015-12-17 17:01:41">15.12.17</td>
     <td class="gall_count">329904</td>
     <td class="gall_recommend">34</td>
    </tr>
    <tr class="ub-content us-post" data-no="4782971" data-type="icon_pic">
     <td class="gall_num">4782971</td>
     <td class="gall_tit ub-word"><a href="/board/view/?id=granblue&amp;no=4782971&amp;page=1" view-msg=""> <em class="icon_img icon_pic"></em>레페 추가될 수영복은 누굴까</a></td>
     <td class="gall_writer ub-writer" data-nick="KOSMOS" data-uid="301rs3xp0cfs" data-ip="" data-loc="list"><span class="nickname in" title="KOSMOS" style=""><em>KOSMOS</em></span><a class="writer_nikcon "><img src="https://nstatic.dcinside.com/dc/w/images/fix_nik.gif" border="0" title="301rs3xp0c** : 갤로그로 이동합니다." width="12" height="11" style="cursor:pointer;margin-left:2px;" onclick="window.open('//gallog.dcinside.com/301rs3xp0cfs');" alt="갤로그로 이동합니다."></a></td>
     <td class="gall_date" title="2024-07-19 15:59:28">15:59</td>
     <td class="gall_count">33</td>
     <td class="gall_recommend">0</td>
    </tr>
 */
