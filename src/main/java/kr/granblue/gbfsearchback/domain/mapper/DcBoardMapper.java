package kr.granblue.gbfsearchback.domain.mapper;

import kr.granblue.gbfsearchback.domain.DcBoard;
import kr.granblue.gbfsearchback.domain.enums.SourceType;

public class DcBoardMapper {

    public static DcBoard fromDto(kr.granblue.gbfsearchback.scraper.dto.DcBoard dcBoard) {
        return DcBoard.builder()
                .id(1L)
                .dcNum(dcBoard.getDcNum())
                .title(dcBoard.getTitle())
                .content(dcBoard.getContent())
                .writer(dcBoard.getWriter())
                .regDate(dcBoard.getRegDate())
                .commentCnt(dcBoard.getCommentCnt())
                .viewCnt(dcBoard.getViewCnt())
                .recommendCnt(dcBoard.getRecommendCnt())
                .sourceType(SourceType.DC)
                .recommended(dcBoard.isRecommended())
                .build();
    }

}
