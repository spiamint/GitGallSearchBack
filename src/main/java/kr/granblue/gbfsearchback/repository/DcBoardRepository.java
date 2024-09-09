package kr.granblue.gbfsearchback.repository;

import kr.granblue.gbfsearchback.domain.DcBoard;
import kr.granblue.gbfsearchback.repository.dto.DuplicateCountDto;
import kr.granblue.gbfsearchback.repository.dto.SimilarityDtoInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DcBoardRepository extends JpaRepository<DcBoard, Long> {
    long countByCreatedAtAfter(LocalDateTime localDateTime);

    @Query("select b from DcBoard b where b.id > :id order by b.id asc")
    Page<DcBoard> findPagedBoardByIdGreaterThan(Pageable pageable, @Param("id") Long id);

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

    // dc_board_embedding 테이블과 조인하여 embedding 값이 없는 게시글을 찾는 쿼리
    @Query(value = "SELECT b FROM DcBoard b" +
                    " LEFT JOIN DcBoardEmbedding be" +
                    " ON b.id = be.board.id" +
                    " WHERE be.id IS NULL",
            countQuery = "SELECT count(*) FROM DcBoard b" +
                    " LEFT JOIN DcBoardEmbedding be" +
                    " ON b.id = be.board.id" +
                    " WHERE be.id IS NULL")
    Page<DcBoard> findBoardsWithoutEmbeddingFull(Pageable pageable);

    @Query("select count(*) as totalCount, count(distinct b.dcNum) as distinctCount from DcBoard b")
    DuplicateCountDto findDuplicateCount();

    @Query("select d from DcBoard d where d.id Not in" +
            " (select t.id from (select min(b.id) as id from DcBoard b group by b.dcNum) as t )")
    List<DcBoard> findDuplicateBoard();

    @Transactional @Modifying
    @Query("delete from DcBoard d where d.id Not in (" +
            " select t.id from (select min(b.id) as id from DcBoard b group by b.dcNum) as t )")
    int deleteDuplicate();





}
