package kr.granblue.gbfsearchback.scraper.dto;

import lombok.*;

import java.time.LocalDateTime;

@Builder @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class DcComment {

    private Long id;
    private Long commentNum; // dc 댓글 번호
    private Long boardNum; // dc 게시글 번호
    private String writer;
    private String content;
    private LocalDateTime regDate;

    private boolean reply; // 대댓글여부

    @Builder.Default
    private Long targetNum = -1L; // 대댓글 타겟 디시 댓글 번호

    public void setTargetNum(Long commentNum) {
        this.targetNum = commentNum;
    }
}
