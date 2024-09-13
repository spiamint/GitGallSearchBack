package kr.github.gitgallsearchback.domain;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity @Table(name = "git_embedding")
@Getter @ToString @EqualsAndHashCode
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Embedding { // delete on cascade by DB

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne (fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @JdbcTypeCode(SqlTypes.VECTOR) // @Array(length = 1536)
    private float[] titleContent = new float[] {0.0F}; // title + content

    @CreationTimestamp
    private LocalDateTime createdAt;

}
