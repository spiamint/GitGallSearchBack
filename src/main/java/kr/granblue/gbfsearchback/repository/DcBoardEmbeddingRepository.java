package kr.granblue.gbfsearchback.repository;

import kr.granblue.gbfsearchback.domain.DcBoard;
import kr.granblue.gbfsearchback.domain.DcBoardEmbedding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface DcBoardEmbeddingRepository extends JpaRepository<DcBoardEmbedding, Long> {

    long countByCreatedAtAfter(LocalDateTime localDateTime);

    // 임베딩을 페이지단위 병렬로 삽입하기 때문에
    // board_id 가 커도(나중이어도) 임베딩 삽입이 먼저되는 경우가 있어 board_id 로 정렬하도록 함
    DcBoardEmbedding findFirstByOrderByBoardIdDesc();

    /**
     * 임베딩을 DcBoard.id 기준으로 삭제 (중복 제거)
     * @param boardIds
     * @return
     */
    @Transactional @Modifying
    @Query("delete from DcBoardEmbedding be where be.board.id in :boardIds")
    int deleteByBoardIdIn(List<Long> boardIds);

}
