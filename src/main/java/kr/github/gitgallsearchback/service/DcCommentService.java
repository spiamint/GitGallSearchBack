package kr.github.gitgallsearchback.service;

import kr.github.gitgallsearchback.domain.Comment;
import kr.github.gitgallsearchback.repository.BulkInsertRepository;
import kr.github.gitgallsearchback.repository.DcCommentRepository;
import kr.github.gitgallsearchback.repository.dto.DuplicateCountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public DuplicateCountDto findDuplicateCount() {
        return commentRepository.findDuplicateCount();
    }

    public List<Comment> findDuplicate() {
        return commentRepository.selectDuplicate();
    }

    @Transactional
    public int deleteDuplicate() {
        return commentRepository.deleteDuplicate();
    }

    public void saveComments(List<Comment> comments) {
        bulkInsertRepository.bulkSaveComment(comments);
    }

    @Async
    public CompletableFuture<Void> asyncSaveComments(List<Comment> comments) {
        return CompletableFuture.runAsync(() ->
                bulkInsertRepository.bulkSaveComment(comments), executor);
    }

}
