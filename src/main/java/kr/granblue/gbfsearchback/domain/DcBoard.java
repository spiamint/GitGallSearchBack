package kr.granblue.gbfsearchback.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity @Table(name = "dc_board")
@Getter @ToString @EqualsAndHashCode
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class DcBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long dcNum;
    private String title;
    private String content;
    private String writer;
    private LocalDateTime regDate;
    @CreationTimestamp
    private LocalDateTime createdAt;

    private long viewCnt;             // Default = 0
    private long commentCnt;         // Default = 0
    private long recommendCnt;        // Default = 0

    @Transient
    private String cleanContent;

    private boolean recommended;

    public void setTitle(String title) {
        this.title = title;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public void setCleanContent(String cleanContent) { this.cleanContent = cleanContent; }
    public void setRecommended(boolean recommended) { this.recommended = recommended; }
}

