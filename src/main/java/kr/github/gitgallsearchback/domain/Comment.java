package kr.github.gitgallsearchback.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Builder
@Entity @Table(name = "git_comment")
@Getter @ToString @EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

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

    public void setCleanContent(String content) {
        this.cleanContent = content;
    }
}
