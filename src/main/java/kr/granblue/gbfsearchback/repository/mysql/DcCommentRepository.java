package kr.granblue.gbfsearchback.repository.mysql;

import kr.granblue.gbfsearchback.domain.DcComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DcCommentRepository extends JpaRepository<DcComment, Long> {
    long countByCreatedAtAfter(LocalDateTime localDateTime);

    @Query("select c from DcComment c where c.boardNum in :dcNum")
    List<DcComment> findCommentsByBoardNumInDcNum(List<Long> dcNum);
}
