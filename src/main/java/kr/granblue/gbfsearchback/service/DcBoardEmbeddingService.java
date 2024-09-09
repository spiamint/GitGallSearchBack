package kr.granblue.gbfsearchback.service;

import kr.granblue.gbfsearchback.domain.DcBoard;
import kr.granblue.gbfsearchback.domain.DcBoardEmbedding;
import kr.granblue.gbfsearchback.repository.BulkInsertRepository;
import kr.granblue.gbfsearchback.repository.DcBoardEmbeddingRepository;
import kr.granblue.gbfsearchback.repository.DcBoardRepository;
import kr.granblue.gbfsearchback.util.ContentCleaner;
import lombok.Getter;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

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

    /**
     * 페이지 단위 임베딩 실행
     * @param pageSize : DB 에서 조회할때 필요한 페이지 사이즈
     * @param maxSize : 임베딩 maxSize
     * @param option : "full", "from-last" 두개 (time 제한 추가예정)
     */
    public void embed(int pageSize, long maxSize, String option) {
        setTarget(pageSize, maxSize, option);
    }

    /**
     * 임베딩 대상 설정
     * @param pageSize
     * @param maxSize
     * @param option : "full", "from-last"
     */
    protected  void setTarget(int pageSize, long maxSize, String option) {
        Params params; // DB 에서 조회할때 필요한 메서드 파라미터
        Function<Params, Page<DcBoard>> getNotEmbeddedBoardsFunction; // DB 에서 조회할때 사용할 메서드
        switch (option) {
            case "full": // 전체 스캔후 임베딩 안된것 임베딩
                params = new Params(pageSize, maxSize, null);
                getNotEmbeddedBoardsFunction =
                        (p) -> boardRepository.findBoardsWithoutEmbeddingFull(PageRequest.of(p.getPageNum(), p.getPageSize()));
                break;

            case "from-last": // 마지막으로 임베딩 된 게시글 이후부터 임베딩
                // 마지막으로 임베딩 된 게시글
                DcBoardEmbedding lastEmbeddedBoard = embeddingRepository.findFirstByOrderByBoardIdDesc();
                log.info("lastEmbeddedBoard = {}", lastEmbeddedBoard);
                // 첫실행 시 lastEmbeddedBoard 가 없으면 fullscan
                Long lastEmbeddedBoardId = lastEmbeddedBoard == null ? 0L : lastEmbeddedBoard.getBoard().getId();

                params = new Params(pageSize, maxSize, lastEmbeddedBoardId);
                getNotEmbeddedBoardsFunction =
                        (p) -> boardRepository.findPagedBoardByIdGreaterThan(PageRequest.of(p.getPageNum(), p.getPageSize()), (long) p.getParam());
                break;
                
            default:
                throw new IllegalArgumentException("pageSize = " + pageSize + " / maxSize = " + maxSize + " / option = " + option);
        }

        // 임베딩 할 대상을 DB 에서 조회
        loadAndExecute(params, getNotEmbeddedBoardsFunction);
    }

    /**
     * 임베딩 할 대상을 DB 에서 조회후, 전처리 및 실제 임베딩 호출 실행
     * @param params
     * @param getNotEmbeddedBoardFunction
     * @return
     */
    protected void loadAndExecute(Params params, Function<Params, Page<DcBoard>> getNotEmbeddedBoardFunction) {
        // 비동기 작업 리스트
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        // 시작시간
        LocalDateTime startTime = LocalDateTime.now();
        
        // 페이지 순회
        for (int pageNum = 0; pageNum * params.getPageSize() < params.getMaxSize(); pageNum++) {
            // 시작 페이지 설정
            params.setPageNum(pageNum);
            // 주어진 함수로 임베딩 안된 게시글 페이지 가져오기
            Page<DcBoard> notEmbeddedBoardsPage = getNotEmbeddedBoardFunction.apply(params);
            // 없으면 종료
            if (notEmbeddedBoardsPage == null || notEmbeddedBoardsPage.isEmpty()) break;

            // 내용에서 html 태그 제거
            List<DcBoard> notEmbeddedBoards = notEmbeddedBoardsPage.getContent();
            notEmbeddedBoards.forEach(dcBoard -> {
                dcBoard.setCleanContent(ContentCleaner.cleanContent(dcBoard.getContent()));
            });

            // 비동기 임베딩 내부호출
            futures.add(this.asyncEmbedAndSave(notEmbeddedBoards));
        }

        // 비동기 작업 모두 종료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        LocalDateTime endTime = LocalDateTime.now();

        // 로깅
        long countByCreatedAtAfter = embeddingRepository.countByCreatedAtAfter(startTime);
        log.info("\n[EMBEDDING COMPLETE]===============================================================\n" +
                        "countByCreadtedAtAfter lastEmbeddedBoard = {}\n" +
                        "startTime = {}, endTime = {}\n" +
                        "====================================================================================",
                countByCreatedAtAfter,
                startTime, endTime);
    }

    @Getter
    class Params {
        int pageNum;
        int pageSize;
        long maxSize;
        Object param;

        /**
         * DB 조회용 메서드에서 사용할 파라미터, pageNum 은 조회시 할당
         * @param pageSize : PageRequest.pageSize
         * @param maxSize : 페이지 순회 최대값
         * @param param : 추가 파라미터 Object
         */
        public Params(int pageSize, long maxSize, Object param) {
            this.pageSize = pageSize;
            this.maxSize = maxSize;
            this.param = param;
        }

        public void setPageNum(int pageNum) {
            this.pageNum = pageNum;
        }
    }

    /**
     * 실제 임베딩 작업 비동기로 진행 및 INSERT
     * @param dcBoards
     * @return
     */
    @Async
    protected CompletableFuture<Void> asyncEmbedAndSave(List<DcBoard> dcBoards) {
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

                String input = dcBoard.getTitle() + " " + dcBoard.getCleanContent();
                float[] output;
                try {
                    EmbeddingResponse embeddingResponse = embeddingModel.call(
                            new EmbeddingRequest(
                                    List.of(input),
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
                    output = new float[256]; // 에러시 일단 기본값으로 처리
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