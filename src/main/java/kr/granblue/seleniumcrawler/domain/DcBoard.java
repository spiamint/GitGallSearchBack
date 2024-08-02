package kr.granblue.seleniumcrawler.domain;

import jakarta.persistence.*;
import kr.granblue.seleniumcrawler.domain.enums.SourceType;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity @Table(name = "dc_board", schema = "crawler")
@Getter @ToString @EqualsAndHashCode(of = {"writer", "createdAt"})
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

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    private boolean recommended;

    public void setContent(String content) {
        this.content = content;
    }
    public void setCleanContent(String cleanContent) { this.cleanContent = cleanContent; }
    public void setRecommended(boolean recommended) { this.recommended = recommended; }
}

