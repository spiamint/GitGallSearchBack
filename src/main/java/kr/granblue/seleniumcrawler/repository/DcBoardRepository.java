package kr.granblue.seleniumcrawler.repository;

import kr.granblue.seleniumcrawler.domain.DcBoard;
import kr.granblue.seleniumcrawler.repository.dto.SimilarityDtoInterface;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DcBoardRepository extends JpaRepository<DcBoard, Long> {

    @Query("select b from DcBoard b where b.id > :id order by b.id asc")
    List<DcBoard> findPagedBoardByIdGreaterThan(Pageable pageable, @Param("id") Long id);

    long countByCreatedAtAfter(LocalDateTime localDateTime);
    long countByIdGreaterThan(Long id);

    /**
     * 테스트) 유사도 상위 5개 SimilarityDtoInterface 로 받아오는 쿼리
     * @param embedding
     * @return List<SimilarityDtoInterface> limit 5
     */
    @Query(nativeQuery = true,
            value = "select " +
                    " b.id as boardId," +
                    " b.title," +
                    " b.dc_num," +
                    " 1 - (be.title <=> :embedding ::vector) as similarity," +
                    " be.title as embeddingValue" +
                    " FROM crawler.dc_board b" +
                    " JOIN crawler.dc_board_embedding be ON b.id = be.id" +
                    " ORDER BY be.title <=> CAST(:embedding AS vector) ASC" +
                    " LIMIT 10;")
    List<SimilarityDtoInterface> getSimilarityTop10(String embedding);

}
