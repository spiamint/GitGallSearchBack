package kr.granblue.dcscraper.service;

import kr.granblue.dcscraper.domain.DcBoard;
import kr.granblue.dcscraper.repository.BulkInsertRepository;
import kr.granblue.dcscraper.repository.DcBoardRepository;
import kr.granblue.dcscraper.repository.dto.SimilarityDto;
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

    public long deleteDuplicate(long limitCount) {
        return boardRepository.deleteDuplicate(limitCount);
    }

    /**
     * content 가 없는 게시글에 content 업데이트
     * @param dcBoards
     */
    public void fillContent(List<DcBoard> dcBoards) {
        bulkInsertRepository.bulkUpdateBoard(dcBoards);
    }

    /**
     * 테스트용 유사도 상위 10개
     * @param embedding
     * @return
     */
    public List<SimilarityDto> getSimilarityTop10(String embedding) {
        return boardRepository.getSimilarityTop10(embedding)
                .stream().map(SimilarityDto::of)
                .collect(Collectors.toList());
    }

}
