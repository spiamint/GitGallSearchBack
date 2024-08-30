package kr.granblue.dcscraper.service;

import kr.granblue.dcscraper.domain.DcBoard;
import kr.granblue.dcscraper.domain.DcBoardEmbedding;
import kr.granblue.dcscraper.repository.BulkInsertRepository;
import kr.granblue.dcscraper.repository.DcBoardEmbeddingRepository;
import kr.granblue.dcscraper.repository.DcBoardRepository;
import kr.granblue.dcscraper.util.ContentCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/*
 * 병렬처리를 위해 별도분리
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DcBoardEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final DcBoardEmbeddingRepository embeddingRepository;
    private final DcBoardRepository boardRepository;
    private final BulkInsertRepository bulkInsertRepository;
    private final @Qualifier("embedExecutor") Executor embedExecutor;

    public long countByCreatedAtAfter(LocalDateTime localDateTime) {
        return embeddingRepository.countByCreatedAtAfter(localDateTime);
    }

    /**
     * 마지막으로 임베딩된 게시글 이후의 게시글을 모두 임베딩
     * @param pageSize
     * @param time
     */
    public void embedNotEmbedded(int pageSize, long maxSize, LocalDateTime time) {
        // 비동기 처리 리스트
        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        // 시간이 null 로 들어오면 풀스캔 (1999.01.01)
        time = time == null ? LocalDateTime.of(1999, 1, 1, 0, 0) : time;

        LocalDateTime startTime = LocalDateTime.now();
        // 페이지 단위 순회
        for (int pageNum = 0; pageNum * pageSize < maxSize; pageNum++) {
            Page<DcBoard> boardsWithoutEmbedding = boardRepository.findBoardsWithoutEmbedding(PageRequest.of(pageNum, pageSize), time);
            if (boardsWithoutEmbedding.isEmpty()) break;
            List<DcBoard> notEmbeddedBoards = boardsWithoutEmbedding.getContent();

            notEmbeddedBoards.forEach(dcBoard -> {
                dcBoard.setCleanContent(ContentCleaner.cleanContent(dcBoard.getContent()));
            });

            // 비동기 임베딩 내부호출
            completableFutures.add(this.asyncEmbedAndSave(notEmbeddedBoards));
        }

        // 비동기 작업 모두 종료
        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();
        LocalDateTime endTime = LocalDateTime.now();

        long countByCreatedAtAfter = embeddingRepository.countByCreatedAtAfter(startTime);
        log.info("\n[EMBEDDING COMPLETE]===============================================================\n" +
                        "countByCreadtedAtAfter lastEmbeddedBoard = {}\n" +
                        "startTime = {}, endTime = {}\n" +
                        "====================================================================================",
                countByCreatedAtAfter,
                startTime, endTime);
    }

    /**
     * 특정 갯수 만큼 임베딩
     *
     * @param pageSize
     * @param maxSize
     */
    public void partialEmbed(int pageSize, long maxSize) {
        executeEmbed(pageSize, maxSize);
    }

    /**
     * 페이지 단위로 비동기 임베딩 및 insert을 진행
     *
     * @param pageSize : DB에서 가져올 페이지 사이즈 (limit)
     * @param maxSize  : 임베딩 해서 insert 할 최대 게시글 수 (count)
     */
    protected void executeEmbed(int pageSize, long maxSize) {
        // 비동기 처리 리스트
        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        // 마지막으로 임베딩 된 게시글
        DcBoardEmbedding lastEmbeddedBoard = embeddingRepository.findTopByOrderByIdDesc();
        // 게시글이 null 이면 stop
//        if (lastEmbeddedBoard == null) return;

        Long lastEmbeddedBoardId;
        if (lastEmbeddedBoard == null) {
            lastEmbeddedBoardId = 0L;
        } else {
            lastEmbeddedBoardId = lastEmbeddedBoard.getBoard().getId();
        }

        LocalDateTime startTime = LocalDateTime.now();
        // 페이지 단위 순회
        for (int pageNum = 0; pageNum * pageSize < maxSize; pageNum++) {
            // 임베딩 안된 게시글 페이지 가져오기
            List<DcBoard> notEmbeddedBoards = boardRepository.findPagedBoardByIdGreaterThan(
                    PageRequest.of(pageNum, pageSize), lastEmbeddedBoardId);

            notEmbeddedBoards.forEach(dcBoard -> {
                dcBoard.setCleanContent(ContentCleaner.cleanContent(dcBoard.getContent()));
            });

            // 비동기 임베딩 내부호출
            completableFutures.add(this.asyncEmbedAndSave(notEmbeddedBoards));
        }

        // 비동기 작업 모두 종료
        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();
        LocalDateTime endTime = LocalDateTime.now();

        long countByCreatedAtAfter = embeddingRepository.countByCreatedAtAfter(startTime);
        log.info("\n[EMBEDDING COMPLETE]===============================================================\n" +
                        "countByCreadtedAtAfter lastEmbeddedBoard = {}\n" +
                        "startTime = {}, endTime = {}\n" +
                        "====================================================================================",
                countByCreatedAtAfter,
                startTime, endTime);
    }

    /**
     * 비동기 작업 : 실제 임베딩 및 INSERT
     *
     * @param dcBoards
     * @return
     */
    @Async
    public CompletableFuture<Void> asyncEmbedAndSave(List<DcBoard> dcBoards) {
        return CompletableFuture.runAsync(() -> {
            List<DcBoardEmbedding> boardEmbeddings = new ArrayList<>();
            for (DcBoard dcBoard : dcBoards) {
                log.info("\n===========================================\n" +
                                "embedding board.id = {} / dcNum = {}\n" +
                                "title = {}\n" +
                                "cleanContent = {}\n" +
                                "===========================================",
                        dcBoard.getId(),
                        dcBoard.getDcNum(),
                        dcBoard.getTitle(),
                        dcBoard.getCleanContent());

                float[] output;
                try {
                    EmbeddingResponse embeddingResponse = embeddingModel.call(
                            new EmbeddingRequest(
                                    List.of(dcBoard.getTitle() + " " + dcBoard.getCleanContent()),
                                    OpenAiEmbeddingOptions.builder().withDimensions(256).build())
                    );
                    Embedding embedding = embeddingResponse.getResult();
                    output = embedding.getOutput();
                } catch (Exception e) {
                    log.error("\n[ERROR] embeddingModel.call();=====================================\n" +
                                    "{}\n" +
                                    "DcBoard.dcNum = {} DcBoard.title ={}\n" + 
                                    "cleanContent = {}",
                            e, dcBoard.getDcNum(), dcBoard.getTitle(), dcBoard.getCleanContent());
                    output = null; // 에러시 일단 0.0 으로 처리
                }


                // 임베딩 객체 생성 및 추가
                DcBoardEmbedding boardEmbedding = DcBoardEmbedding.builder()
                        .board(dcBoard)
                        .titleContent(output)
                        .build();
                boardEmbeddings.add(boardEmbedding);
            }

            // 임베딩 객체들을 DB에 저장
            bulkInsertRepository.bulkSaveEmbedding(boardEmbeddings);
        }, embedExecutor);
    }
}