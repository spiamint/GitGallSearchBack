package kr.granblue.seleniumcrawler.repository;

import kr.granblue.seleniumcrawler.domain.DcBoard;
import kr.granblue.seleniumcrawler.domain.DcBoardEmbedding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BulkInsertRepository {
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void bulkSaveBoard(List<DcBoard> boards) {
        batchBoardInsert(boards, boards.size());
    }

    public void batchBoardInsert(List<DcBoard> boards, int batchSize) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO crawler.dc_board (" +
                        "dc_num, title, writer, content," +
                        " reg_date," +
                        " view_cnt, recommend_cnt, comment_cnt," +
                        " source_type," +
                        " recommended)" +
                        " VALUES (?, ?, ?, ?, ?::timestamp, ?, ?, ?, ?, ?)",
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

    @Transactional
    public void bulkSaveEmbedding(List<DcBoardEmbedding> embeddings) {
        batchEmbeddingInsert(embeddings, embeddings.size());
    }

    public void batchEmbeddingInsert(List<DcBoardEmbedding> embeddings, int batchSize) {
        // 벡터값 너무 길어서 로그 별도로 작성
        StringBuilder sb = new StringBuilder();
        sb.append("batchEmbeddingInsert - ")
                .append("size = ").append(embeddings.size());
        for (int i = 0; i < embeddings.size(); i++) {
            sb.append("\n")
                    .append("index = ").append(i).append(" ")
                    .append("INSERT INTO crawler.dc_board_embedding")
                    .append("( ")
                    .append("board_id, title_content") // row name
                    .append(") ")
                    .append("VALUES")
                    .append("( ")
                    .append(embeddings.get(i).getBoard().getId()) // board_id
                    .append(", ")
                    .append(embeddings.get(i).getTitleContent().toString().substring(0, 10) + "...]") // title_content
                    .append(":vector ") // ::vector
                    .append(")");
        }
        log.info(sb.toString());

        jdbcTemplate.batchUpdate(
                "INSERT INTO crawler.dc_board_embedding (" +
                        "board_id, title_content)" +
                        " VALUES (?, ?::vector)",
                embeddings, batchSize,
                (ps, embedding) -> {
                    ps.setLong(1, embedding.getBoard().getId());
                    ps.setString(2, embedding.getTitleContent().toString());
                });
    }
}

