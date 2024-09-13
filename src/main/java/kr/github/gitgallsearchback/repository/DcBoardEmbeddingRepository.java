package kr.github.gitgallsearchback.repository;

import kr.github.gitgallsearchback.domain.Embedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface DcBoardEmbeddingRepository extends JpaRepository<Embedding, Long> {

    long countByCreatedAtAfter(LocalDateTime localDateTime);

    // 임베딩을 페이지단위 병렬로 삽입하기 때문에
    // board_id 가 커도(나중이어도) 임베딩 삽입이 먼저되는 경우가 있어 board_id 로 정렬하도록 함
    Embedding findFirstByOrderByBoardIdDesc();

    /**
     * 임베딩을 DcBoard.id 기준으로 삭제 (중복 제거)
     * @param boardIds
     * @return
     */
    @Modifying
    @Query("delete from Embedding be where be.board.id in :boardIds")
    int deleteByBoardIdIn(List<Long> boardIds);

}
