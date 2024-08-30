package kr.granblue.dcscraper.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Builder
@Entity @Table(name = "dc_comment")
@Getter @ToString @EqualsAndHashCode(of = {"writer", "createdAt"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class DcComment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long commentNum; // dc 댓글 번호
    private Long boardNum; // dc 게시글 번호
    private String writer;
    private String content;
    private LocalDateTime regDate;
    @CreationTimestamp
    private LocalDateTime createdAt;

    private boolean reply; // 대댓글여부

    @Builder.Default
    private Long targetNum = -1L; // 대댓글 타겟 디시 댓글 번호

    @Transient
    private String cleanContent;

    public void setTargetNum(Long commentNum) {
        this.targetNum = commentNum;
    }
}
