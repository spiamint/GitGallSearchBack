package kr.granblue.gbfsearchback.repository;

import kr.granblue.gbfsearchback.domain.DcComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DcCommentRepository extends JpaRepository<DcComment, Long> {
    long countByCreatedAtAfter(LocalDateTime localDateTime);
}
