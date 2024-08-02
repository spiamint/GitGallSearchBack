package kr.granblue.seleniumcrawler.service;

import kr.granblue.seleniumcrawler.domain.DcBoard;
import kr.granblue.seleniumcrawler.repository.BulkInsertRepository;
import kr.granblue.seleniumcrawler.repository.DcBoardRepository;
import kr.granblue.seleniumcrawler.repository.dto.SimilarityDto;
import kr.granblue.seleniumcrawler.repository.dto.SimilarityDtoInterface;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
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

    public long countByCreatedAtAfter(LocalDateTime localDateTime) {
        return boardRepository.countByCreatedAtAfter(localDateTime);
    }

    long countByIdGreaterThan(Long id) {
        return boardRepository.countByIdGreaterThan(id);
    }

    @Async
    public CompletableFuture<Void> asyncSaveBoards(DcBoard... boards) {
        return CompletableFuture.runAsync(() ->
                bulkInsertRepository.bulkSaveBoard(Arrays.stream(boards).toList()), executor);
    }

    List<SimilarityDto> getSimilarityTop10(String embedding) {
        return boardRepository.getSimilarityTop10(embedding)
                .stream().map(SimilarityDto::of)
                .collect(Collectors.toList());
    }

}
