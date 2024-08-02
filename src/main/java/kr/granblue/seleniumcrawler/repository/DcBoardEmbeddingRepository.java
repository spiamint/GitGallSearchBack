package kr.granblue.seleniumcrawler.repository;

import kr.granblue.seleniumcrawler.domain.DcBoardEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DcBoardEmbeddingRepository extends JpaRepository<DcBoardEmbedding, Long> {

    long countByCreatedAtAfter(LocalDateTime localDateTime);
    DcBoardEmbedding findTopByOrderByIdDesc();

    // command ============================================================================
    @Modifying
    @Query(value = "alter role authenticator set statement_timeout = '300s'", nativeQuery = true)
    void alterTimeout();

    @Modifying
    @Query(value = "set statement_timeout to 300000", nativeQuery = true)
    void alterTimeout2();

    @Modifying
    @Query(value = "ALTER TABLE crawler.dc_board_embedding DROP COLUMN title", nativeQuery = true)
    void deleteRow();

    @Modifying
    @Query(value = "ALTER TABLE crawler.dc_board_embedding DROP COLUMN content", nativeQuery = true)
    void deleteRow2();
}
