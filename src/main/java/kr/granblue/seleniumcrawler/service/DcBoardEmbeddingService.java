package kr.granblue.seleniumcrawler.service;

import kr.granblue.seleniumcrawler.domain.DcBoard;
import kr.granblue.seleniumcrawler.domain.DcBoardEmbedding;
import kr.granblue.seleniumcrawler.repository.BulkInsertRepository;
import kr.granblue.seleniumcrawler.repository.DcBoardEmbeddingRepository;
import kr.granblue.seleniumcrawler.repository.DcBoardRepository;
import kr.granblue.seleniumcrawler.util.ContentCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/*
 * 병렬처리를 위해 별도분리
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DcBoardEmbeddingService {

    private final EmbeddingClient embeddingClient;
    private final DcBoardEmbeddingRepository embeddingRepository;
    private final DcBoardRepository boardRepository;
    private final BulkInsertRepository bulkInsertRepository;
    private final @Qualifier("embedExecutor") Executor embedExecutor;

    public long countByCreatedAtAfter(LocalDateTime localDateTime) {
        return embeddingRepository.countByCreatedAtAfter(localDateTime);
    }

    public void embedLoop() {
        long embeddedCount = 0;
        long allEmbeddedCount = 0;
        do {
            embeddedCount = embedNotEmbedded(); // 전체 임베딩 작업은 비동기 작업이 모두 종료되야 끝남
            allEmbeddedCount += embeddedCount;
        } while (embeddedCount > 0); // 더이상 임베딩할 게시글이 없을 때 종료
        log.info("\n=======================================================\n" +
                "모든 게시글 임베딩 완료\n " +
                "갯수: {}\n" +
                "=======================================================", allEmbeddedCount);
    }

    /**
     * 페이지 단위로 비동기 임베딩을 진행
     * ( 임베딩 + insert ) 가 세트로 비동기 진행됨
     * @return 임베딩 한 갯수 (마지막 임베딩 기준)
     */
    public long embedNotEmbedded() {
        // 마지막 임베딩된 게시글 과 id
        DcBoardEmbedding lastEmbeddedEmbedding = embeddingRepository.findTopByOrderByIdDesc();
        Long lastEmbeddedBoardId = lastEmbeddedEmbedding.getBoard().getId();
        // 임베딩 안된 게시글 총 수
        long notEmbeddedCount = boardRepository.countByIdGreaterThan(lastEmbeddedBoardId);
        if (notEmbeddedCount == 0) { log.info("모든 게시글이 임베딩 되었습니다."); return 0; }

        // 페이지 단위로 처리
        int pageNum = 0; int pageSize = 10;
        // 로깅용 처리된 수
        long embedAndSavedCount = 0;
        // 비동기 처리 리스트
        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();

        for (pageNum = 0; pageNum * pageSize < notEmbeddedCount; pageNum++) {
            // 임베딩 안된 게시글 가져오기
            List<DcBoard> notEmbeddedBoards = boardRepository.findPagedBoardByIdGreaterThan(PageRequest.of(pageNum, pageSize), lastEmbeddedBoardId);

            // cleanContent set
            notEmbeddedBoards.forEach(dcBoard -> {
                dcBoard.setCleanContent(ContentCleaner.cleanContent(dcBoard.getContent()));
                log.info("dcBoard = {} : {} : {}", dcBoard.getId(), dcBoard.getTitle(), dcBoard.getCleanContent());
            });

            // 페이지 단위 비동기 임베딩
            completableFutures.add(asyncEmbedAndSave(notEmbeddedBoards.toArray(new DcBoard[0])));
        }

        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();

        long countByCreatedAtAfter = embeddingRepository.countByCreatedAtAfter(lastEmbeddedEmbedding.getCreatedAt());
        log.info("countByCreadtedAtAfter lastEmbeddedBoard = {}", countByCreatedAtAfter);
        
        return countByCreatedAtAfter; // 반복을 위한 리턴
    }

    @Async
    public CompletableFuture<Void> asyncEmbedAndSave(DcBoard... dcBoards) {
        return CompletableFuture.runAsync(() -> {
            List<DcBoardEmbedding> boardEmbeddings = new ArrayList<>();
            for (DcBoard dcBoard : dcBoards) {
                String titleContent = dcBoard.getTitle() + " " + dcBoard.getCleanContent();

                log.info("\n===========================================\n" +
                                "embedding board.id = {} / dcNum = {}\n" +
                                "titleContent = {}\n" +
                                "===========================================",
                        dcBoard.getId(),
                        dcBoard.getDcNum(),
                        titleContent);

                List<Double> embeddedTitleContent = embeddingClient.embed(titleContent);
                DcBoardEmbedding boardEmbedding = DcBoardEmbedding.builder()
                        .board(dcBoard)
                        .titleContent(embeddedTitleContent)
                        .build();
                boardEmbeddings.add(boardEmbedding);
            }
            bulkInsertRepository.bulkSaveEmbedding(boardEmbeddings);
        }, embedExecutor);
    }

    /**
     * Embedding 작업 까지 비동기로 하는 코드 (비활성)
     */
    public void asyncEmbedNotEmbedded() {

        // 마지막 임베딩된 게시글 과 id
        DcBoard lastEmbeddedBoard = embeddingRepository.findTopByOrderByIdDesc().getBoard();
        Long lastEmbeddedBoardId = lastEmbeddedBoard.getId();
        // 임베딩 안된 게시글 총 수
        long notEmbeddedCount = boardRepository.countByIdGreaterThan(lastEmbeddedBoardId);
        // 페이지 단위로 처리
        int pageNum = 0; int pageSize = 10;
        // 로깅용 처리된 수
        long embedAndSavedCount = 0;

        for (pageNum = 0; pageNum * pageSize < notEmbeddedCount; pageNum++) {
            // 임베딩 안된 게시글 가져오기
            List<DcBoard> notEmbeddedBoards = boardRepository.findPagedBoardByIdGreaterThan(PageRequest.of(pageNum, pageSize), lastEmbeddedBoardId);

            notEmbeddedBoards.forEach(dcBoard -> {
                log.info("dcBoard = {} : {}", dcBoard.getId(), dcBoard.getTitle());
            });

            // 비동기 임베딩
            List<CompletableFuture<List<Double>>> completableFutures = new ArrayList<>();
            for (DcBoard dcBoard : notEmbeddedBoards) {
                completableFutures.add(asyncEmbed(dcBoard));
            }

            // 모든 Embedding 종료 후 embeddings 리스트에 저장
            List<List<Double>> embeddings = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        return completableFutures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList());
                    })
                    .join();

            // embeddings 와 notEmbeddedBoards 를 이용하여 DcBoardEmbedding 객체 생성
            List<DcBoardEmbedding> willSaveList = new ArrayList<>();
            for(int i = 0; i < notEmbeddedBoards.size(); i++) {
                DcBoardEmbedding boardEmbedding = DcBoardEmbedding.builder()
                        .board(notEmbeddedBoards.get(i))
                        .titleContent(embeddings.get(i))
                        .build();
                willSaveList.add(boardEmbedding);
            }

            // save to db
            bulkInsertRepository.bulkSaveEmbedding(willSaveList);
            embedAndSavedCount += notEmbeddedBoards.size();
        }

        log.info("embedAndSavedCount = {}", embedAndSavedCount);

        long countByCreatedAtAfter = embeddingRepository.countByCreatedAtAfter(lastEmbeddedBoard.getCreatedAt());
        log.info("countByCreadtedAtAfter lastEmbeddedBoard = {}", countByCreatedAtAfter);

    }

    @Async
    public CompletableFuture<List<Double>> asyncEmbed(DcBoard board) {
        String titleContent = board.getTitle() + " " + board.getCleanContent();
        return CompletableFuture.supplyAsync(() -> embeddingClient.embed(titleContent), embedExecutor);
    }
}
