package kr.granblue.dcscraper.repository;

import kr.granblue.dcscraper.domain.DcBoard;
import kr.granblue.dcscraper.repository.dto.SimilarityDtoInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DcBoardRepository extends JpaRepository<DcBoard, Long> {

    @Query("select b from DcBoard b where b.id > :id order by b.id asc")
    List<DcBoard> findPagedBoardByIdGreaterThan(Pageable pageable, @Param("id") Long id);

    // dc_board_embedding 테이블과 조인하여 embedding 값이 없는 게시글을 찾는 쿼리
    @Query(nativeQuery = true,
            value = "SELECT b.* FROM dc_board b" +
                    " LEFT JOIN dc_board_embedding be" +
                    " ON b.id = be.board_id" +
                    " WHERE be.id IS NULL" +
                    " AND b.created_at > :time",
            countQuery = "SELECT count(*) FROM dc_board b" +
                    " LEFT JOIN dc_board_embedding be" +
                    " ON b.id = be.board_id" +
                    " WHERE be.id IS NULL" +
                    " AND b.created_at > :time")
    Page<DcBoard> findBoardsWithoutEmbedding(Pageable pageable, LocalDateTime time);

    List<DcBoard> findByTitleIsNull();

    DcBoard findByDcNum(long dcNum);

    long countByCreatedAtAfter(LocalDateTime localDateTime);

    long countByIdGreaterThan(Long id);

    /**
     * 테스트) 유사도 상위 10개 SimilarityDtoInterface 로 받아오는 쿼리
     *
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

    /**
     * 중복 자료 제거용
     *
     * @param intervalStart
     * @param intervalEnd
     * @return
     */
    @Query(nativeQuery = true,
            value = "WITH deleted_rows AS " +
                    "( " +
                    "DELETE FROM scrape_prod.dc_board " +
                    "WHERE id between :intervalStart and :intervalEnd " +
                    "and " +
                    "id NOT IN (SELECT MIN(id) " +
                    "FROM scrape_prod.dc_board " +
                    "WHERE id between :intervalStart and :intervalEnd " +
                    "GROUP BY dc_num) " +
                    "returning id " +
                    ") " +
                    "SELECT COUNT(*) AS deleted_rows_count FROM deleted_rows;"
    )
    long deleteDuplicates(long intervalStart, long intervalEnd);

    /**
     * 중복 자료 제거용 (최근 저장된 row 부터 dc_num 동일한것 group by 후 limitCount 로 잘라서 삭제)
     * @param limitCount group by 에서 자를 갯수 (10000 이하 권장)
     * @return
     */
    @Query(nativeQuery = true,
            value = "WITH deleted_rows AS " +
                    " (" +
                    " DELETE FROM dc_board " +
                    " WHERE dc_num > :limitDcNum" +
                    " AND" +
                    " id NOT IN " +
                    " (" +
                    " SELECT MIN(id) FROM dc_board " +
                    " GROUP BY dc_num " +
                    " HAVING dc_num > :limitDcNum " +
                    " )" +
                    " RETURNING id" +
                    " )" +
                    " SELECT COUNT(*) AS deleted_rows_count FROM deleted_rows;"
    )
    long deleteDuplicate(long limitDcNum);

}
