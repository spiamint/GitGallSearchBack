package kr.granblue.seleniumcrawler.domain;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter @ToString @EqualsAndHashCode
public class Board {
    private Long id;
    private String writer;
    private String title;
    private String content;
    private LocalDateTime regDate;
    private Long viewCnt;
    private Long recommend;

    public void setContent(String content) {
        this.content = content;
    }

    public void setEmbeddedContent(String content) {
        this.content = content;
    }

    public void setEmbeddedTitle(String title) {
        this.title = title;
    }
}
