//package kr.granblue.dcscraper.scraper.temp;
//
//import com.google.common.base.Stopwatch;
//import kr.granblue.dcscraper.domain.DcBoard;
//import kr.granblue.dcscraper.domain.DcComment;
//import kr.granblue.dcscraper.domain.enums.SourceType;
//import kr.granblue.dcscraper.driver.WebDriverUtil;
//import kr.granblue.dcscraper.service.DcBoardService;
//import kr.granblue.dcscraper.service.DcCommentService;
//import kr.granblue.dcscraper.util.ContentCleaner;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.jsoup.nodes.Document;
//import org.openqa.selenium.By;
//import org.openqa.selenium.TimeoutException;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.WebElement;
//import org.openqa.selenium.support.ui.ExpectedConditions;
//import org.openqa.selenium.support.ui.WebDriverWait;
//import org.springframework.ai.embedding.EmbeddingClient;
//import org.springframework.stereotype.Component;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
//@Component
//@Slf4j
//@Getter
//@RequiredArgsConstructor
//public class DcCrawlerSelenium {
//
//    private WebDriver webDriver; // chromeDriver from WebDriverUtil
//    private final EmbeddingClient embeddingClient;
//
//    private final DcBoardService dcBoardService;
//    private final DcCommentService dcCommentService;
//
//    private final CommentExtractorSelenium commentExtractor;
//
//
//    public void crawlStart(long startPage, long endPage) {
//        log.info("=====[CRAWL START]=====");
//        // 시간 확인
//        LocalDateTime startTime = LocalDateTime.now();
//        LocalDateTime endTime;
//
//        // 스크래핑할 시작, 끝 페이지
//        Long pageNum = startPage;
//        Long maxPageNum = endPage;
//
//        // 스크래핑 콜백 인터벌
//        Long callbackInterval = 0L;
//
//        // 스크래핑할 url, 가변 uri
//        String baseUrl = "http://gall.dcinside.com";
//        String appendUri = "/board/lists/?id=granblue&page=";
//
//        // 개발용 컷 카운터
//        Long devCounter = 1L; // 개발용 단건 카운트 컷 1부터 N 까지 N 회
//        // 스크래핑 카운터 (선 증가)
//        Long crawledBoardCounter = 0L;
//        Long crawledCommentCounter = 0L;
//
//        // 저장된 아이템 카운터
//        Long savedBoardCounter = 0L;
//        Long savedCommentCounter = 0L;
//
//        // 저장할 디씨글 리스트와 비동기 작업 리스트
//        List<DcBoard> dcBoardsForSave = new ArrayList<>();
//        List<CompletableFuture<Void>> boardFutures = new ArrayList<>();
//        // 저장할 디씨 댓글 리스트와 비동기 작업 리스트
//        List<DcComment> dcCommentsForSave = new ArrayList<>();
//        List<CompletableFuture<Void>> commentFutures = new ArrayList<>();
//
//        // 크롬 드라이버 획득
//        webDriver = WebDriverUtil.getChromeDriver();
//
//        while (pageNum <= maxPageNum) {
//            // 글 리스트 페이지 접속
//            String executeUrl = baseUrl + appendUri + pageNum;
//            try {
//                log.info("opening list executeUrl = {}", executeUrl);
//                webDriver.get(executeUrl);
//            } catch (TimeoutException e) {
//                log.error("[ERROR] webDriver.get(executeUrl);  executeUrl = {} TIMEOUT", executeUrl);
//                continue; // 실패 시 페이지 변화없이 재로드
//            } catch (Exception e) {
//                log.error("[ERROR] webDriver.get(executeUrl); executeUrl = {} e = {}", executeUrl, e);
//                break;
//            }
//
//            // 글 리스트 획득
//            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(3));
//            WebElement gallList = wait.until(ExpectedConditions.presenceOfElementLocated(By.className("gall_list")));
////            WebElement gallList = webDriver.findElement(By.className("gall_list"));
//            List<WebElement> trElementsBoard = gallList.findElements(By.cssSelector("tbody>tr"));
////            log.info("gallList = \n{}", gallList.getAttribute("innerHTML"));
//
//            // 글마다 루프
//            for (WebElement trElement : trElementsBoard) {
//
//                // 설문, AD, 공지 거름
//                String gallNum = trElement.findElement(By.className("gall_num")).getText();
//                long parsedGallNum;
//                try {
//                    parsedGallNum = Long.parseLong(gallNum);
//                } catch (Exception e) {
//                    // gall_num 이 숫자가 아님 -> 설문, AD, 공지
////                     log.error("[ERROR] parseLong(gallNum) : gallNum = {} e = {}", gallNum, e);
//                    continue;
//                }
//
//                // 글에서 필요한 부분 추출
//                WebElement titleElement = trElement.findElement(By.cssSelector(".gall_tit>a"));
//                WebElement writerElement = trElement.findElement(By.className("nickname"));
////                log.info("titleElement = {}", titleElement);
////                log.info("writerElement = {}", writerElement);
//                String gallTitle = titleElement.getText();
//                List<WebElement> replyNumElements = trElement.findElements(By.className("reply_num"));
//                String commentCnt = replyNumElements.size() > 0 ?
//                        replyNumElements.get(0).getText().replaceAll("[\\[\\]]", "") : // text = [1]
//                        "0";
//                String gallWriter = writerElement.getAttribute("title");
//                String gallDate = trElement.findElement(By.className("gall_date")).getAttribute("title");
//                String gallCount = trElement.findElement(By.className("gall_count")).getText();
//                String gallRecommend = trElement.findElement(By.className("gall_recommend")).getText();
//
//                // 날짜 변환 형식 : 2024-07-19 12:05:43
//                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//                LocalDateTime convertedGallDate = LocalDateTime.parse(gallDate, formatter);
//
//                // 글 저장용 객체 생성(내용제외)
//                DcBoard dcBoard = DcBoard.builder()
//                        .dcNum(parsedGallNum)
//                        .title(gallTitle)
//                        .writer(gallWriter)
//                        .regDate(convertedGallDate)
//                        .viewCnt(Long.parseLong(gallCount))
//                        .recommendCnt(Long.parseLong(gallRecommend))
//                        .commentCnt(Long.parseLong(commentCnt))
//                        .sourceType(SourceType.DC)
//                        .build(); // content, recommend null
//
//                // a 태그에서 글 href 추출
//                String href = titleElement.getAttribute("href");
//                executeUrl = href;
//
//                // 글 접속
//                Document dcBoardHtml;
//                Stopwatch stopwatch = Stopwatch.createStarted();
//                try {
//                    log.info("opening board executeUrl = {}", executeUrl);
//
//                    webDriver.get(executeUrl);
//
//                    crawledBoardCounter++;
//                } catch (TimeoutException e) {
//                    log.error("[ERROR] webDriver.get(executeUrl);  executeUrl = {} TIMEOUT", executeUrl);
//                    continue; // 실패 시 페이지 변화없이 재로드
//                } catch (Exception e) {
//                    log.error("[ERROR] webDriver.get(executeUrl); Thread.sleep() executeUrl = {} e = {}", executeUrl, e);
//                    continue;
//                }
//
////                log.info("dcBoardHtml = {}", dcBoardHtml);
//
//                // 내용 추출
//                WebDriverWait waitBoard = new WebDriverWait(webDriver, Duration.ofSeconds(3));
//                WebElement mainContainer = waitBoard.until(ExpectedConditions.presenceOfElementLocated(By.className("gallery_view")));
//
//                stopwatch.stop();
//                log.info("\nGET stopwatch.elapsed = {} : {}", stopwatch.elapsed().toSeconds(), stopwatch.elapsed().toMillisPart());
//                stopwatch.start();
//
////                WebElement mainContainer = webDriver.findElement(By.id("container"));
//                WebElement writeElement = mainContainer.findElement(By.className("write_div"));
//                String rawContent = writeElement.getAttribute("innerHTML");
//
//                // 념글 확인 : 념글버튼의 클래스에 .on 붙으면 념글
//                boolean isRecommended = mainContainer.findElements(By.cssSelector(".btn_recom_up.on")).size() != 0 ? true : false;
//
//                // DcBoard 객체에 내용 추가
//                dcBoard.setRecommended(isRecommended);
//                dcBoard.setContent(rawContent);
//                dcBoard.setCleanContent(ContentCleaner.cleanContent(rawContent));
//
//                log.info("\n================================================================\n" +
//                                "  executeUrl = [{}]\n" +
//                                "  title = \n" +
//                                "{}\n" +
//                                "  cleanContent = \n" +
//                                "{}\n" +
//                                "================================================================\n",
//                        executeUrl,
//                        dcBoard.getTitle(),
//                        dcBoard.getCleanContent()
//                );
//
//                // DB 저장을 위한 리스트에 추가
//                dcBoardsForSave.add(dcBoard);
//
//                // Board 스크래핑 끝 ----------------------------------------------------------
//
//                // 댓글 스크래핑 시작 ----------------------------------------------------------
//                List<WebElement> ulElementComment = mainContainer.findElements(By.className("cmt_list"));
//                List<DcComment> dcComments = commentExtractor.extractCommentAndReply(dcBoard.getDcNum(), ulElementComment);
//                dcCommentsForSave.addAll(dcComments);
//                // 댓글 스크래핑 끝 ----------------------------------------------------------
//
//                stopwatch.stop();
//                log.info("\nALL stopwatch.elapsed = {} : {}", stopwatch.elapsed().toSeconds(), stopwatch.elapsed().toMillisPart());
//
//
//                // 하나만 하고 끝내기 ===============
////                break;
//
////                 여러개 하고 끝내기
////                if (devCounter >= 3) break;
////                devCounter++;
//
//            } // for boardElement
//
//            log.info("\n===================================================[pageNum = {}/{} END]" +
//                            "===================================================",
//                    pageNum, maxPageNum);
//            callbackInterval++;
//            log.info("\n===================================================callbackInterval = {}" +
//                    "===================================================", callbackInterval);
//
//            if (callbackInterval >= 0) {
//                // forSave 리스트 초기화를 위해 카피
//                List dcBoards = List.copyOf(dcBoardsForSave);
//                List dcComments = List.copyOf(dcCommentsForSave);
//                // DB 저장용 리스트를 이용해 DB 저장 (비동기), 비동기 작업 리스트에 추가 (페이지 단위)
//                boardFutures.add(dcBoardService.asyncSaveBoards(dcBoards));
//                commentFutures.add(dcCommentService.asyncSaveComments(dcComments));
//                // forSave 리스트 초기화
//                dcBoardsForSave.clear();
//                dcCommentsForSave.clear();
//                callbackInterval = 0L;
//            }
//
//            pageNum++;
//            devCounter = 1L;
//        } // for pageNum
//
//
//        // 웹 스크래핑 종료
//        webDriver.quit();
//        endTime = LocalDateTime.now();
//        log.info("\n  [CRAWL END] =======================================================\n" +
//                        "  elaspedTime = {}min, startedFrom = {}, endTime = {}\n" +
//                        "  crawledBoardCounter = {}, crawledCommentCounter = {} ",
//                Duration.between(startTime, endTime).toMinutes(), startTime, endTime,
//                crawledBoardCounter, crawledCommentCounter);
//
////         save 끝날때 까지 대기
//        CompletableFuture.allOf(boardFutures.toArray(new CompletableFuture[0])).join();
//        CompletableFuture.allOf(commentFutures.toArray(new CompletableFuture[0])).join();
//
//        savedBoardCounter = dcBoardService.countByCreatedAtAfter(startTime);
//        savedCommentCounter = dcCommentService.countByCreatedAtAfter(startTime);
//
//
//        // 스크래핑 완전 종료
//        endTime = LocalDateTime.now();
//
//        Duration duration = Duration.between(startTime, endTime);
//        log.info("\n  [CRAWL COMPLETE] ==================================================\n" +
//                        "  elaspedTime = {} : {} : {} : {}, startedFrom = {}, endTime = {}\n" +
//                        "  crawledCounter = {}, savedBoardCounter = {}, savedCommentCounter = {}",
//                duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart(), startTime, endTime,
//                crawledBoardCounter, savedBoardCounter, savedCommentCounter);
//    }
//}
//
//
//
///*
//DC 스크래핑 후 글 형태
//select(.ub-content)
//    <tr class="ub-content" data-no="1" data-type="icon_notice">
//     <td class="gall_num">공지</td>
//     <td class="gall_tit ub-word"><a href="/board/view/?id=granblue&amp;no=1&amp;page=1" view-msg=""> <em class="icon_img icon_notice"></em><b><b>그랑블루 판타지 갤러리 이용 안내</b></b></a> <a class="reply_numbox" href="https://gall.dcinside.com/board/view/?id=granblue&amp;no=1&amp;t=cv&amp;page=1"><span class="reply_num">[124]</span></a></td>
//     <td class="gall_writer ub-writer" data-nick="운영자" data-uid="" data-ip="" data-loc="list"><b><b><b>운영자</b></b></b></td>
//     <td class="gall_date" title="2015-12-17 17:01:41">15.12.17</td>
//     <td class="gall_count">329904</td>
//     <td class="gall_recommend">34</td>
//    </tr>
//    <tr class="ub-content us-post" data-no="4782971" data-type="icon_pic">
//     <td class="gall_num">4782971</td>
//     <td class="gall_tit ub-word"><a href="/board/view/?id=granblue&amp;no=4782971&amp;page=1" view-msg=""> <em class="icon_img icon_pic"></em>레페 추가될 수영복은 누굴까</a></td>
//     <td class="gall_writer ub-writer" data-nick="KOSMOS" data-uid="301rs3xp0cfs" data-ip="" data-loc="list"><span class="nickname in" title="KOSMOS" style=""><em>KOSMOS</em></span><a class="writer_nikcon "><img src="https://nstatic.dcinside.com/dc/w/images/fix_nik.gif" border="0" title="301rs3xp0c** : 갤로그로 이동합니다." width="12" height="11" style="cursor:pointer;margin-left:2px;" onclick="window.open('//gallog.dcinside.com/301rs3xp0cfs');" alt="갤로그로 이동합니다."></a></td>
//     <td class="gall_date" title="2024-07-19 15:59:28">15:59</td>
//     <td class="gall_count">33</td>
//     <td class="gall_recommend">0</td>
//    </tr>
// */
//
///*
// 댓글 답글 스크래핑 예시
//
//<li id="comment_li_14233925" class="ub-content">
// <div class="cmt_info clear" data-no="14233925" data-rcnt="3" data-article-no="4798106">
//  <div class="cmt_nickbox">
//   <span class="gall_writer ub-writer" data-nick="신한C" data-uid="jpjp1129" data-ip=""><span class="nickname in" title="신한C" style=""><em>신한C</em></span> <a class="writer_nikcon "><img src="https://nstatic.dcinside.com/dc/w/images/fix_nik.gif" border="0" title="jpjp11** : 갤로그로 이동합니다." width="12" height="11" style="cursor:pointer;" onclick="window.open('//gallog.dcinside.com/jpjp1129');" alt="갤로그로 이동합니다."></a></span>
//  </div>
//  <div class="clear cmt_txtbox btn_reply_write_all">
//   <p class="usertxt ub-word">뒤져</p>
//  </div>
//  <div class="fr clear">
//   <span class="date_time">08.06 16:23:28</span>
//   <div class="cmt_mdf_del " data-type="cmt" re_no="14233925" data-my="N" data-article-no="4798106" data-pwd-pop="Y" data-uid="jpjp1129"></div>
//  </div>
// </div>
//</li>
//답글(reply)는 comment_li_... 과 동일 depth 에서 시작, 내부에 reply_list 를 가짐
//<li>
// <div class="reply show">
//  <div class="reply_box">
//   <ul class="reply_list" id="reply_list_14233925">
//    <li id="reply_li_14233928" class="ub-content">
//     <div class="reply_info clear" data-no="14233928">
//      <div class="cmt_nickbox">
//       <span class="gall_writer ub-writer" data-nick="정보소양" data-uid="shept123" data-ip=""><span class="nickname me in" title="정보소양" style=""><em>정보소양</em></span> <a class="writer_nikcon "><img src="https://nstatic.dcinside.com/dc/w/images/fix_nik.gif" border="0" title="shept1** : 갤로그로 이동합니다." width="12" height="11" style="cursor:pointer;" onclick="window.open('//gallog.dcinside.com/shept123');" alt="갤로그로 이동합니다."></a></span>
//      </div>
//      <div class="clear cmt_txtbox">
//       <p class="usertxt ub-word">나쁜말한번만더해봐라진짜</p>
//      </div>
//      <div class="fr clear">
//       <span class="date_time">08.06 16:24:06</span>
//      </div>
//     </div></li>
//    <li id="reply_li_14233930" class="ub-content">
//     <div class="reply_info clear" data-no="14233930">
//      <div class="cmt_nickbox">
//       <span class="gall_writer ub-writer" data-nick="신한C" data-uid="jpjp1129" data-ip=""><span class="nickname in" title="신한C" style=""><em>신한C</em></span> <a class="writer_nikcon "><img src="https://nstatic.dcinside.com/dc/w/images/fix_nik.gif" border="0" title="jpjp11** : 갤로그로 이동합니다." width="12" height="11" style="cursor:pointer;" onclick="window.open('//gallog.dcinside.com/jpjp1129');" alt="갤로그로 이동합니다."></a></span>
//      </div>
//      <div class="clear cmt_txtbox">
//       <p class="usertxt ub-word">뒤져(하트)</p>
//      </div>
//      <div class="fr clear">
//       <span class="date_time">08.06 16:24:39</span>
//      </div>
//     </div></li>
//    <li id="reply_li_14233931" class="ub-content">
//     <div class="reply_info clear" data-no="14233931">
//      <div class="cmt_nickbox">
//       <span class="gall_writer ub-writer" data-nick="정보소양" data-uid="shept123" data-ip=""><span class="nickname me in" title="정보소양" style=""><em>정보소양</em></span> <a class="writer_nikcon "><img src="https://nstatic.dcinside.com/dc/w/images/fix_nik.gif" border="0" title="shept1** : 갤로그로 이동합니다." width="12" height="11" style="cursor:pointer;" onclick="window.open('//gallog.dcinside.com/shept123');" alt="갤로그로 이동합니다."></a></span>
//      </div>
//      <div class="clear cmt_txtbox">
//       <div class="comment_dccon clear">
//        <div class="coment_dccon_img ">
//         <img class="written_dccon " src="https://dcimg5.dcinside.com/dccon.php?no=62b5df2be09d3ca567b1c5bc12d46b394aa3b1058c6e4d0ca41648b658ea2574147f57745397cd0897f50d707d51990e7d54f43e05dcf62cf58cabda7d77ef3bd062053d419be66ab40d1b448b" conalt="21" alt="21" title="21" data-dcconoverstatus="false">
//        </div>
//        <div class="coment_dccon_info clear dccon_over_box" onmouseover="dccon_btn_over(this);" onmouseout="dccon_btn_over(this);" style="display:none;">
//         <span class="over_alt"></span><button type="button" class="btn_dccon_infoview div_package" data-type="reply" onclick="dccon_btn_click();" reqpath="/dccon">디시콘 보기</button>
//        </div>
//       </div>
//      </div>
//      <div class="fr clear">
//       <span class="date_time">08.06 16:24:52</span>
//      </div>
//     </div></li>
//   </ul>
//  </div>
// </div>
//</li>
//
// */
