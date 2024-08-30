package kr.granblue.dcscraper.domain.mapper;

import kr.granblue.dcscraper.domain.DcBoard;
import kr.granblue.dcscraper.domain.enums.SourceType;

public class DcBoardMapper {

    public static DcBoard fromDto(kr.granblue.dcscraper.scraper.dto.DcBoard dcBoard) {
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
