package kr.granblue.gbfsearchback.service;

import kr.granblue.gbfsearchback.domain.DcBoard;
import kr.granblue.gbfsearchback.repository.BulkInsertRepository;
import kr.granblue.gbfsearchback.repository.mysql.DcBoardRepository;
import kr.granblue.gbfsearchback.repository.dto.SimilarityDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DcBoardService {

    private final BulkInsertRepository bulkInsertRepository;
    private final DcBoardRepository boardRepository;
    private final @Qualifier("asyncExecutor") Executor executor;

    public void setTitle(List<DcBoard> boards) {
        bulkInsertRepository.bulkSetTitle(boards);
    }

    public long countByCreatedAtAfter(LocalDateTime localDateTime) {
        return boardRepository.countByCreatedAtAfter(localDateTime);
    }

    public void saveBoards(List<DcBoard> dcBoards) {
        bulkInsertRepository.bulkSaveBoard(dcBoards);
    }

    @Async
    public CompletableFuture<Void> asyncSaveBoards(List<DcBoard> dcBoards) {
        return CompletableFuture.runAsync(() ->
                bulkInsertRepository.bulkSaveBoard(dcBoards), executor);
    }

    public int deleteDuplicate(long limitCount) {
         return boardRepository.deleteDuplicate();
    }

    /**
     * content 가 없는 게시글에 content 업데이트
     * @param dcBoards
     */
    public void fillContent(List<DcBoard> dcBoards) {
        bulkInsertRepository.bulkUpdateBoard(dcBoards);
    }



}
