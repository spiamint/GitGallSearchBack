package kr.granblue.seleniumcrawler.crawler;

import kr.granblue.seleniumcrawler.domain.Board;
import kr.granblue.seleniumcrawler.driver.WebDriverUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Getter
@RequiredArgsConstructor
public class DcCrawler {

    private WebDriver webDriver; // chromeDriver from WebDriverUtil
    private final EmbeddingClient embeddingClient;


    public void crawlStart() {
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.now().plusYears(1);
        log.info("crawl start");

        String baseUrl = "http://gall.dcinside.com";
        String appendUri = "/board/lists/?id=granblue&page=1";
        String executeUrl = baseUrl + appendUri;
        List<Board> boards = new ArrayList<>();

        webDriver = WebDriverUtil.getChromeDriver();
        Long pageNum = 1L;
        Long maxPageNum = 1L;

        while (pageNum <= maxPageNum) {
            // 글 리스트 페이지 접속
            try {
                webDriver.get(executeUrl);
                log.info("opened list executeUrl = {}", executeUrl);
                Thread.sleep(1500);
            } catch (Exception e) {
                log.error("[ERROR] webDriver.get(executeUrl); Thread.sleep() executeUrl = {} e = {}", executeUrl, e);
                continue; // 실패 시 페이지 변화없이 재로드
            }

            // 글 리스트 획득
            String listPageSource = webDriver.getPageSource();
            Document listDocument = Jsoup.parse(listPageSource);
            Elements listElements = listDocument.select(".ub-content");
//            log.info("listElements = {}", listElements);
            
            for (Element element : listElements) {
                
                // 설문, AD, 공지 거름
                String gallNum = element.select(".gall_num").text();
                long parsedGallNum;
                try {
                    parsedGallNum = Long.parseLong(gallNum);
                } catch (Exception e) {
                    // gall_num 이 숫자가 아님 -> 설문, AD, 공지
//                     log.error("[ERROR] parseLong(gallNum) : gallNum = {} e = {}", gallNum, e);
                    continue;
                }

                // 글에서 필요한 부분 추출
                Element titleElement = element.select(".gall_tit>a").first();
                Element writerElement = element.select(".gall_writer>.nickname.in").first();
//                log.info("titleElement = {}", titleElement);
//                log.info("writerElement = {}", writerElement);
                String gallTitle = titleElement.text();
                String gallWriter = writerElement.attr("title");
                String gallDate = element.select(".gall_date").attr("title");
                String gallCount = element.select(".gall_count").text();
                String gallRecommend = element.select(".gall_recommend").text();

                // 날짜 변환 형식 : 2024-07-19 12:05:43
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime convertedGallDate = LocalDateTime.parse(gallDate, formatter);

                Board board = Board.builder()
                        .id(parsedGallNum)
                        .title(gallTitle)
                        .writer(gallWriter)
                        .regDate(convertedGallDate)
                        .viewCnt(Long.parseLong(gallCount))
                        .recommend(Long.parseLong(gallRecommend))
                        .build();

                // a 태그에서 글 href 추출
                String href = element.select(".ub-content a").attr("href");
                executeUrl = baseUrl + href;
                
                // 글 접속
                try {
                    webDriver.get(executeUrl);
                    log.info("opened board executeUrl = {}", executeUrl);
                    Thread.sleep(3000);
                } catch (Exception e) {
                    log.error("[ERROR] webDriver.get(executeUrl); Thread.sleep() executeUrl = {} e = {}", executeUrl, e);
                    continue;
                }

                String boardPageSource = webDriver.getPageSource();
                Document boardDocument = Jsoup.parse(boardPageSource);

                // 내용 추출
                Elements writeElement = boardDocument.select(".write_div");
                log.info("writeElement .html() = {}, .text() = {}", writeElement.html(), writeElement.text());
                String content = writeElement.isEmpty() ? "" : writeElement.html();

                board.setContent(content);
                boards.add(board);

                // 통상
//                pageNum++;
                
                // 하나만 하고 끝내기 ===============
                break;

            } // for Element

            pageNum++;
            
            // 임베딩

            asyncEmbedding(boards);


        } // for pageNum

        // 크롤링 종료
        webDriver.quit();

        boards.forEach(board -> {
            log.info("board = {}", board);
        });
    }

    private void asyncEmbedding(List<Board> boards) {
        for (Board board : boards) {
            embeddingClient.embed(board.getTitle());

            embeddingClient.embed(board.getContent());
        }



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
