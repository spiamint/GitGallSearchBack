package kr.github.gitgallsearchback.domain.mapper;

import kr.github.gitgallsearchback.domain.Board;
import kr.github.gitgallsearchback.scraper.dto.DcBoard;

public class BoardMapper {

    public static Board fromDto(DcBoard dcBoard) {
        return Board.builder()
                .id(1L)
                .dcNum(dcBoard.getDcNum())
                .title(dcBoard.getTitle())
                .content(dcBoard.getContent())
                .writer(dcBoard.getWriter())
                .regDate(dcBoard.getRegDate())
                .commentCnt(dcBoard.getCommentCnt())
                .viewCnt(dcBoard.getViewCnt())
                .recommendCnt(dcBoard.getRecommendCnt())
                .recommended(dcBoard.isRecommended())
                .build();
    }

}
