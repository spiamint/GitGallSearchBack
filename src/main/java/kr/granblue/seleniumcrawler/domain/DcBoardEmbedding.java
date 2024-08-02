package kr.granblue.seleniumcrawler.domain;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
@Entity @Table(name = "dc_board_embedding", schema = "crawler")
@Getter @ToString @EqualsAndHashCode
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class DcBoardEmbedding { // delete on cascade by DB

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "board_id") @ToString.Exclude
    private DcBoard board;

    @JdbcTypeCode(SqlTypes.VECTOR) // @Array(length = 1536)
    private List<Double> titleContent = List.of(0.0); // title + content

    @CreationTimestamp
    private LocalDateTime createdAt;

}
