package kr.github.gitgallsearchback.repository;

import kr.github.gitgallsearchback.domain.Board;
import kr.github.gitgallsearchback.repository.dto.DuplicateCountDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DcBoardRepository extends JpaRepository<Board, Long> {
    long countByCreatedAtAfter(LocalDateTime localDateTime);

    @Query("select b from Board b where b.id > :id order by b.id asc")
    Page<Board> findPagedBoardByIdGreaterThan(Pageable pageable, @Param("id") Long id);

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
    Page<Board> findBoardsWithoutEmbedding(Pageable pageable, LocalDateTime time);

    // dc_board_embedding 테이블과 조인하여 embedding 값이 없는 게시글을 찾는 쿼리
    @Query(value = "SELECT b FROM Board b" +
                    " LEFT JOIN Embedding be" +
                    " ON b.id = be.board.id" +
                    " WHERE be.id IS NULL" +
                    " ORDER BY b.id",
            countQuery = "SELECT count(*) FROM Board b" +
                    " LEFT JOIN Embedding be" +
                    " ON b.id = be.board.id" +
                    " WHERE be.id IS NULL")
    Page<Board> findBoardsWithoutEmbeddingFull(Pageable pageable);

    @Query("select count(*) as totalCount, count(distinct b.dcNum) as distinctCount from Board b")
    DuplicateCountDto findDuplicateCount();

    @Query("select d from Board d where d.id Not in" +
            " (select t.id from (select min(b.id) as id from Board b group by b.dcNum) as t )")
    List<Board> findDuplicateBoard();

    @Modifying
    @Query("delete from Board d where d.id Not in (" +
            " select t.id from (select min(b.id) as id from Board b group by b.dcNum) as t )")
    int deleteDuplicate();





}
