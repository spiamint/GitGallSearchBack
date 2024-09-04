package kr.granblue.gbfsearchback.scraper.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class DcBoard {

    private Long dcNum;
    private String title;
    private String content;
    private String writer;
    private LocalDateTime regDate;

    private long viewCnt;
    private long commentCnt;
    private long recommendCnt;
    private boolean recommended;

    public void setTitle(String title) {
        this.title = title;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public void setRecommended(boolean recommended) { this.recommended = recommended; }


    public Long getDcNum() {
        return dcNum;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getWriter() {
        return writer;
    }

    public LocalDateTime getRegDate() {
        return regDate;
    }

    public long getViewCnt() {
        return viewCnt;
    }

    public long getCommentCnt() {
        return commentCnt;
    }

    public long getRecommendCnt() {
        return recommendCnt;
    }

    public boolean isRecommended() {
        return recommended;
    }
}

