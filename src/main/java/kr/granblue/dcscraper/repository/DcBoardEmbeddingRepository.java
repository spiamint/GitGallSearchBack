package kr.granblue.dcscraper.repository;

import kr.granblue.dcscraper.domain.DcBoardEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DcBoardEmbeddingRepository extends JpaRepository<DcBoardEmbedding, Long> {

    long countByCreatedAtAfter(LocalDateTime localDateTime);
    DcBoardEmbedding findTopByOrderByIdDesc();

}
