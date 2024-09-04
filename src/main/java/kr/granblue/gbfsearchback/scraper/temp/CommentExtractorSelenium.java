package kr.granblue.gbfsearchback.scraper.temp;

import kr.granblue.gbfsearchback.domain.DcComment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentExtractorSelenium {

    protected boolean isReply(WebElement liElement) {
        return liElement.findElements(By.className("reply_list")).size() > 0;
    }

    protected boolean isDeleted(WebElement liElement) {
        return liElement.findElements(By.className("del_reply")).size() > 0;
    }

    protected boolean isDory(WebElement liElement) {
        return liElement.findElements(By.className("dory")).size() > 0;
    }

    /**
     * 댓글 ul 요소 (.cmt_list) 에서 댓글과 답글을 추출
     * @param dcBoardNum 게시글 번호
     * @param ulElements 댓글 ul 요소
     * @return 댓글과 답글 리스트 (없으면 size = 0)
     */
    public List<DcComment> extractCommentAndReply(long dcBoardNum, List<WebElement> ulElements) {
        if (ulElements.size() == 0) { return new ArrayList<>(); }
        WebElement ulElement = ulElements.get(0);
        List<WebElement> liElements = ulElement.findElements(By.cssSelector(".cmt_list>li"));// 댓글 답글 모두 li 태그로 가져올수 잇음
        DcComment result = null;
        List<DcComment> results = new ArrayList<>();

        DcComment prevComment = null; // 답글 저장시 이전 댓글을 찾기 위한 변수
        for (WebElement liElement : liElements) {
            if (isDory(liElement)) { continue; } // 댓글돌이 제외
            
            if (!isReply(liElement)) { // 답글 여부 확인
                // 댓글
                if (isDeleted(liElement)) { // 삭제여부 확인
                    // 삭제된 댓글
                    result = extractDeletedComment(dcBoardNum, liElement);
                } else {
                    // 일반 댓글
                    result = extractComment(dcBoardNum, liElement);
                }
                prevComment = result;
            } else {
                // 답글
                List<WebElement> liElementsReply = liElement.findElements(By.cssSelector(".reply_list>li"));
                // 답글리스트 루프
                for (WebElement liElementReply : liElementsReply) {
                    result = extractReply(dcBoardNum, liElementReply, prevComment.getCommentNum());

                }
            }
            results.add(result);
//            log.info("CommentExtractor result: {}", result);
        } // liElements.forEach

        return results;
    }

    protected DcComment extractComment(long dcBoardNum, WebElement liElement) {
        String commentNum = liElement.findElement(By.className("cmt_info")).getAttribute("data-no");
        String replyWriter = liElement.findElement(By.className("gall_writer")).getAttribute("data-nick");
        String replyContent = liElement.findElement(By.className("cmt_txtbox")).getAttribute("innerHTML");
        String replyRegDate = liElement.findElement(By.className("date_time")).getText(); // 대댓글 날짜 가져오는거 방지

        LocalDateTime parsedTime = parseTime(replyRegDate);

        return DcComment.builder()
                .boardNum(dcBoardNum)
                .commentNum(Long.parseLong(commentNum))
                .writer(replyWriter)
                .content(replyContent)
                .regDate(parsedTime)
                .reply(false)
                .build();
    }

    public DcComment extractReply(long dcBoardNum, WebElement liElement, long targetNum) {
        String commentNum = liElement.findElement(By.className("reply_info")).getAttribute("data-no");
        String replyWriter = liElement.findElement(By.className("gall_writer")).getAttribute("data-nick");
        String replyContent = liElement.findElement(By.className("cmt_txtbox")).getAttribute("innerHTML");
        String replyRegDate = liElement.findElement(By.className("date_time")).getText();
        LocalDateTime parsedTime = parseTime(replyRegDate);

        return DcComment.builder()
                .boardNum(dcBoardNum)
                .commentNum(Long.parseLong(commentNum))
                .writer(replyWriter)
                .content(replyContent)
                .regDate(parsedTime)
                .reply(true)
                .targetNum(targetNum)
                .build();
    }

    public DcComment extractDeletedComment(long dcBoardNum, WebElement liElement) {
        String commentNum = liElement.findElement(By.className("cmt_info")).getAttribute("data-no");
        String replyContent = liElement.findElement(By.className("del_reply")).getText();

        return DcComment.builder()
                .boardNum(dcBoardNum)
                .commentNum(Long.parseLong(commentNum))
                .content(replyContent)
                .reply(false)
                .build();
    }

    protected LocalDateTime parseTime(String regDate) {
        // 날짜 변환 형식 2024.07.19 12:05:43 or 07-19 12:05:43
        regDate = regDate.length() > 14 ? regDate : LocalDateTime.now().getYear() + "." + regDate;
        DateTimeFormatter replyFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
        return LocalDateTime.parse(regDate, replyFormatter);
    }


}
