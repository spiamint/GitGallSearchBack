package kr.granblue.gbfsearchback.repository;

import kr.granblue.gbfsearchback.domain.DcBoard;
import kr.granblue.gbfsearchback.domain.DcBoardEmbedding;
import kr.granblue.gbfsearchback.domain.DcComment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BulkInsertRepository {
    private final @Qualifier("mysqlDataSource") DataSource mysqlDataSource;
    private final @Qualifier("postgreDataSource") DataSource postgreDataSource;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Transactional
    public void bulkSaveBoard(List<DcBoard> boards) {
        batchBoardInsert(boards, boards.size());
    }

    protected void batchBoardInsert(List<DcBoard> boards, int batchSize) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(mysqlDataSource);
        jdbcTemplate.batchUpdate(
                "INSERT INTO " + schema + ".dc_board (" +
                        "dc_num, title, writer, content," +
                        " reg_date," +
                        " view_cnt, recommend_cnt, comment_cnt," +
                        " source_type," +
                        " recommended)" +
                        " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                boards, batchSize,
                (ps, board) -> {
                    ps.setLong(1, board.getDcNum());
                    ps.setString(2, board.getTitle());
                    ps.setString(3, board.getWriter());
                    ps.setString(4, board.getContent());
                    ps.setString(5, board.getRegDate().toString());
                    ps.setLong(6, board.getViewCnt());
                    ps.setLong(7, board.getRecommendCnt());
                    ps.setLong(8, board.getCommentCnt());
                    ps.setString(9, board.getSourceType().name());
                    ps.setBoolean(10, board.isRecommended());
                });
    }

    /**
     * content 가 없는 게시글에 content 업데이트
     * @param boards
     */
    @Transactional
    public void bulkUpdateBoard(List<DcBoard> boards) {
        batchBoardUpdate(boards, boards.size());
    }

    protected void batchBoardUpdate(List<DcBoard> boards, int batchSize) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(mysqlDataSource);
        // update scrape_prd.dc_board set content = ? where dc_num = ?
        jdbcTemplate.batchUpdate(
                "UPDATE " + schema + ".dc_board" +
                        " SET content = ? " +
                        " WHERE dc_num = ?",
                boards, batchSize,
                (ps, board) -> {
                    ps.setString(1, board.getContent());
                    ps.setLong(2, board.getDcNum());
                });
    }

    /**
     * content 가 없는 게시글에 content 업데이트
     * @param boards
     */
    @Transactional
    public void bulkSetTitle(List<DcBoard> boards) {
        batchsetTitle(boards, boards.size());
    }

    protected void batchsetTitle(List<DcBoard> boards, int batchSize) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(mysqlDataSource);
        // update scrape_prd.dc_board set title = ? where dc_num = ?
        jdbcTemplate.batchUpdate(
                "UPDATE " + schema + ".dc_board" +
                        " SET title = ? " +
                        " WHERE dc_num = ?",
                boards, batchSize,
                (ps, board) -> {
                    ps.setString(1, board.getTitle());
                    ps.setLong(2, board.getDcNum());
                });
    }

    @Transactional
    public void bulkSaveComment(List<DcComment> comments) {
        batchCommentInsert(comments, comments.size());
    }

    protected void batchCommentInsert(List<DcComment> comments, int batchSize) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(mysqlDataSource);
        jdbcTemplate.batchUpdate(
                "INSERT INTO " + schema + ".dc_comment (" +
                        "comment_num, board_num, writer, content," +
                        " reg_date, reply, target_num)" +
                        " VALUES (?, ?, ?, ?, ?, ?, ?)",
                comments, batchSize,
                (ps, comment) -> {
                    ps.setLong(1, comment.getCommentNum());
                    ps.setLong(2, comment.getBoardNum());
                    ps.setString(3, comment.getWriter());
                    ps.setString(4, comment.getContent());
                    ps.setString(5, comment.getRegDate() != null ?
                            comment.getRegDate().toString() :
                            LocalDateTime.of(1999,01,01,00,00).toString()); // 삭제된 댓글
                    ps.setBoolean(6, comment.isReply());
                    ps.setLong(7, comment.getTargetNum());
                });
    }

    @Transactional
    public void bulkSaveEmbedding(List<DcBoardEmbedding> embeddings) {
        batchEmbeddingInsert(embeddings, embeddings.size());
    }

    protected void batchEmbeddingInsert(List<DcBoardEmbedding> embeddings, int batchSize) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(postgreDataSource);
        jdbcTemplate.batchUpdate(
                "INSERT INTO " + schema + ".dc_board_embedding (" +
                        " board_id, title_content, recommended)" +
                        " VALUES (?, ?, ?)",
                embeddings, batchSize,
                (ps, embedding) -> {
                    ps.setLong(1, embedding.getBoardId());
                    ps.setObject(2, embedding.getTitleContent());
                    ps.setBoolean(3, embedding.isRecommended());
                });
    }

    @Transactional
    public void bulkUpdateEmbedding(List<DcBoardEmbedding> embeddings) {
        batchEmbeddingUpdate(embeddings, embeddings.size());
    }

    protected void batchEmbeddingUpdate(List<DcBoardEmbedding> embeddings, int batchSize) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(postgreDataSource);
        jdbcTemplate.batchUpdate(
                "UPDATE " + schema + ".dc_board_embedding" +
                        " SET recommended = ?" +
                        " WHERE board_id = ?",
                embeddings, batchSize,
                (ps, embedding) -> {
                    ps.setBoolean(1, embedding.isRecommended());
                    ps.setLong(2, embedding.getBoardId());
                });
    }
}

