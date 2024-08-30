package kr.granblue.dcscraper.repository;

import kr.granblue.dcscraper.domain.DcComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DcCommentRepository extends JpaRepository<DcComment, Long> {
    long countByCreatedAtAfter(LocalDateTime localDateTime);
}
