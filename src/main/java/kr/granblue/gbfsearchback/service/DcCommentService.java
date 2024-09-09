package kr.granblue.gbfsearchback.service;

import kr.granblue.gbfsearchback.domain.DcComment;
import kr.granblue.gbfsearchback.repository.BulkInsertRepository;
import kr.granblue.gbfsearchback.repository.DcCommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
public class DcCommentService {

    private final BulkInsertRepository bulkInsertRepository;
    private final DcCommentRepository commentRepository;
    private final @Qualifier("asyncExecutor") Executor executor;

    public long countByCreatedAtAfter(LocalDateTime localDateTime) {
        return commentRepository.countByCreatedAtAfter(localDateTime);
    }

    public void saveComments(List<DcComment> comments) {
        bulkInsertRepository.bulkSaveComment(comments);
    }

    @Async
    public CompletableFuture<Void> asyncSaveComments(List<DcComment> comments) {
        return CompletableFuture.runAsync(() ->
                bulkInsertRepository.bulkSaveComment(comments), executor);
    }

}
