package kr.github.gitgallsearchback.repository;

import kr.github.gitgallsearchback.domain.Comment;
import kr.github.gitgallsearchback.repository.dto.DuplicateCountDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DcCommentRepository extends JpaRepository<Comment, Long> {
    long countByCreatedAtAfter(LocalDateTime localDateTime);

    @Query("select count(*) as totalCount, count(distinct c.commentNum) as distinctCount from Comment c")
    DuplicateCountDto findDuplicateCount();

    @Query("select c FROM Comment c" +
            " WHERE c.id IN" +
            " (" +
            " SELECT t.id" +
            " FROM" +
            " (" +
            " SELECT c2.id as id," +
            " ROW_NUMBER() OVER (PARTITION BY c2.commentNum ORDER BY c2.id) AS row_num" +
            " FROM Comment c2" +
            " ) " +
            " as t" +
            " WHERE t.row_num > 1" +
            " )")
    List<Comment> selectDuplicate();

    @Modifying
    @Query("delete FROM Comment c" +
            " WHERE c.id IN" +
            " (" +
            " SELECT t.id" +
            " FROM" +
            " (" +
            " SELECT c2.id as id," +
            " ROW_NUMBER() OVER (PARTITION BY c2.commentNum ORDER BY c2.id) AS row_num" +
            " FROM Comment c2" +
            " ) " +
            " as t" +
            " WHERE t.row_num > 1" +
            " )")
    int deleteDuplicate();
}
