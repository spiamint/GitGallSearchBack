package kr.github.gitgallsearchback.service;

import com.google.common.base.Stopwatch;
import kr.github.gitgallsearchback.domain.Board;
import kr.github.gitgallsearchback.domain.Embedding;
import kr.github.gitgallsearchback.repository.BulkInsertRepository;
import kr.github.gitgallsearchback.repository.DcBoardEmbeddingRepository;
import kr.github.gitgallsearchback.repository.DcBoardRepository;
import kr.github.gitgallsearchback.util.ContentCleaner;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     *
     * @param pageSize : DB 에서 조회할때 필요한 페이지 사이즈
     * @param maxSize  : 임베딩 maxSize
     * @param option   : "full", "from-last" 두개 (time 제한 추가예정)
     */
    public void embed(int pageSize, long maxSize, String option) {
        setTarget(pageSize, maxSize, option);
    }

    /**
     * 임베딩 대상 설정
     *
     * @param pageSize
     * @param maxSize
     * @param option   : "full", "from-last"
     */
    protected void setTarget(int pageSize, long maxSize, String option) {
        Params params; // DB 에서 조회할때 필요한 메서드 파라미터
        Function<Params, Page<Board>> getNotEmbeddedBoardsFunction; // DB 에서 조회할때 사용할 메서드
        switch (option) {
            case "full": // 전체 스캔후 임베딩 안된것 임베딩
                params = new Params(pageSize, maxSize, null);
                getNotEmbeddedBoardsFunction =
                        (p) -> boardRepository.findBoardsWithoutEmbeddingFull(PageRequest.of(p.getPageNum(), p.getPageSize()));
                break;

            case "from-last": // 마지막으로 임베딩 된 게시글 이후부터 임베딩
                // 마지막으로 임베딩 된 게시글
                Embedding lastEmbeddedBoard = embeddingRepository.findFirstByOrderByBoardIdDesc();
                log.info("lastEmbeddedBoard id = {} board.title = {} board.content = {}",
                        lastEmbeddedBoard.getId(), lastEmbeddedBoard.getBoard().getTitle(), lastEmbeddedBoard.getBoard().cleanContent());
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
     *
     * @param params
     * @param getNotEmbeddedBoardFunction
     * @return
     */
    protected void loadAndExecute(Params params, Function<Params, Page<Board>> getNotEmbeddedBoardFunction) {
        // 비동기 작업 리스트
        List<CompletableFuture<List<Embedding>>> futures = new ArrayList<>();
        // 시작시간
        LocalDateTime startTime = LocalDateTime.now();

        // 임베딩만 스톱워치
        Stopwatch stopWatch = Stopwatch.createStarted();
        // 페이지 순회
        for (int pageNum = 0; pageNum * params.getPageSize() < params.getMaxSize(); pageNum++) {
            // 시작 페이지 설정
            params.setPageNum(pageNum);
            // 주어진 함수로 임베딩 안된 게시글 페이지 가져오기
            Page<Board> notEmbeddedBoardsPage = getNotEmbeddedBoardFunction.apply(params);
            // 없으면 종료
            if (notEmbeddedBoardsPage == null || notEmbeddedBoardsPage.isEmpty()) break;

            // 내용에서 html 태그 제거
            List<Board> notEmbeddedBoards = notEmbeddedBoardsPage.getContent();
            notEmbeddedBoards.forEach(dcBoard -> {
                dcBoard.setCleanContent(ContentCleaner.cleanContent(dcBoard.getContent()));
            });

            // 비동기 임베딩 내부호출
            futures.add(this.asyncEmbedAndSave(notEmbeddedBoards));
        }

        // 비동기 작업 모두 종료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<List<Embedding>> embeddedResults = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());

        Duration embeddingTime = stopWatch.stop().elapsed();
        log.info("\n[EMBEDDING END]=======================================================\n" +
                        "embeddingTime = {}m {}s {}millis\n" +
                        "======================================================================",
                embeddingTime.toMinutesPart(), embeddingTime.toSecondsPart(), embeddingTime.toMillisPart());

        // 임베딩 결과를 DB 에 저장
        embeddedResults.forEach(embedded -> {
            log.info("saving DB, index = {} embedded.size = {}", embeddedResults.indexOf(embedded), embedded.size());
            bulkInsertRepository.bulkSaveEmbedding(embedded);
        });
        LocalDateTime endTime = LocalDateTime.now();

        // 로깅
        long countByCreatedAtAfter = embeddingRepository.countByCreatedAtAfter(startTime);
        log.info("\n[EMBEDDING INSERT COMPLETE]===============================================================\n" +
                        "startTime = {}, endTime = {}\n" +
                        "count by created after startTime = {}\n" +
                        "embeddingTime = {}m {}s {}millis\n" +
                        "====================================================================================",
                startTime, endTime,
                countByCreatedAtAfter,
                embeddingTime.toMinutesPart(), embeddingTime.toSecondsPart(), embeddingTime.toMillisPart());
    }

    @Getter
    class Params {
        int pageNum;
        int pageSize;
        long maxSize;
        Object param;

        /**
         * DB 조회용 메서드에서 사용할 파라미터, pageNum 은 조회시 할당
         *
         * @param pageSize : PageRequest.pageSize
         * @param maxSize  : 페이지 순회 최대값
         * @param param    : 추가 파라미터 Object
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
     *
     * @param boards
     * @return
     */
    @Async
    protected CompletableFuture<List<Embedding>> asyncEmbedAndSave(List<Board> boards) {
        return CompletableFuture.supplyAsync(() -> {
            List<Embedding> boardEmbeddings = new ArrayList<>();
            int reduceLength = 0; // token 초과시 prompt 자를 길이.
            for (int i = 0; i < boards.size(); i++) {
                Board board = boards.get(i);
                log.info("\n----------------------------------------------------------\n" +
                                "embedding board.id = {} / dcNum = {}\n" +
                                "title = {}\n" +
                                "cleanContent = {}\n",
                        board.getId(),
                        board.getDcNum(),
                        board.getTitle(),
                        board.getCleanContent());

                String input = board.getTitle() + " " + board.getCleanContent();

                if (reduceLength > 0) {
                    // token 갯수 초과하여 재시도중이면 prompt 자르기
                    input = input.substring(0, input.length() - reduceLength);
                }

                float[] output = new float[256];
                try {
                    EmbeddingResponse embeddingResponse = embeddingModel.call(
                            new EmbeddingRequest(
                                    List.of(input),
                                    OpenAiEmbeddingOptions.builder().withDimensions(256).build())
                    );
                    org.springframework.ai.embedding.Embedding embedding = embeddingResponse.getResult();
                    output = embedding.getOutput();
                    reduceLength = 0; // 성공시 reduceLength 초기화

                } catch (NonTransientAiException e) {
                    if (e.getMessage().indexOf("reduce your prompt") > 0) {
                        // 프롬프트 길이 초과 (8192) 메시지 : "This model's maximum context length is 8192 tokens, however you requested 11691 tokens (11691 in your prompt; 0 for the completion). Please reduce your prompt; or completion length.",
                        List<String> splittedMsg = Arrays.asList(e.getMessage().split(" "));
                        int tokenSize = Integer.parseInt(splittedMsg.get(splittedMsg.indexOf("requested") + 1));
                        reduceLength += (tokenSize - 8192) / 2 + 1; // 토큰 수 차이의 절반만큼 원 프롬프트 length 자를것 (+1 은 소숫점버림 보정)
                        i -= 1; // 재시도
                        continue;
                    }
                } catch (Exception e) {
                    log.error("\n[ERROR] embeddingModel.call();=====================================\n" +
                                    "{}\n" +
                                    "DcBoard.dcNum = {} DcBoard.title ={}\n" +
                                    "cleanContent = {}",
                            e, board.getDcNum(), board.getTitle(), board.getCleanContent());
                }

                // 임베딩 객체 생성 및 추가
                Embedding boardEmbedding = Embedding.builder()
                        .board(board)
                        .titleContent(output)
                        .build();
                boardEmbeddings.add(boardEmbedding);
            }
            return boardEmbeddings;
        }, embedExecutor);
    }

}