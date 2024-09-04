package kr.granblue.gbfsearchback.domain;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity @Table(name = "dc_board_embedding")
@Getter @ToString @EqualsAndHashCode
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class DcBoardEmbedding { // delete on cascade by DB

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long boardId;

    @JdbcTypeCode(SqlTypes.VECTOR) // @Array(length = 1536)
    private float[] titleContent = new float[] {0.0F}; // title + content

    @CreationTimestamp
    private LocalDateTime createdAt;

    private boolean recommended;

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

}
