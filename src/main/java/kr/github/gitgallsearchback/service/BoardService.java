package kr.github.gitgallsearchback.service;

import kr.github.gitgallsearchback.domain.Board;
import kr.github.gitgallsearchback.repository.BulkInsertRepository;
import kr.github.gitgallsearchback.repository.DcBoardRepository;
import kr.github.gitgallsearchback.repository.dto.DuplicateCountDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BulkInsertRepository bulkInsertRepository;
    private final DcBoardRepository boardRepository;
    private final @Qualifier("asyncExecutor") Executor executor;

    public void setTitle(List<Board> boards) {
        bulkInsertRepository.bulkSetTitle(boards);
    }

    public long countByCreatedAtAfter(LocalDateTime localDateTime) {
        return boardRepository.countByCreatedAtAfter(localDateTime);
    }

    public void saveBoards(List<Board> boards) {
        bulkInsertRepository.bulkSaveBoard(boards);
    }

    @Async
    public CompletableFuture<Void> asyncSaveBoards(List<Board> boards) {
        return CompletableFuture.runAsync(() ->
                bulkInsertRepository.bulkSaveBoard(boards)
                , executor
        );
    }

    public List<Board> findDuplicateBoard() {
        return boardRepository.findDuplicateBoard();
    }

    public DuplicateCountDto findDuplicateCount() {
        return boardRepository.findDuplicateCount();
    }

    @Transactional
    public int deleteDuplicate() {
        // delete Embedding cascade
         return boardRepository.deleteDuplicate();
    }

    /**
     * content 가 없는 게시글에 content 업데이트
     * @param boards
     */
    public void fillContent(List<Board> boards) {
        bulkInsertRepository.bulkUpdateBoard(boards);
    }



}
