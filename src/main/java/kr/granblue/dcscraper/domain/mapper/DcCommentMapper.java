package kr.granblue.dcscraper.domain.mapper;

import kr.granblue.dcscraper.domain.DcComment;

public class DcCommentMapper {

    public static DcComment fromDto(kr.granblue.dcscraper.scraper.dto.DcComment dcComment) {
        return DcComment.builder()
                .commentNum(dcComment.getCommentNum())
                .boardNum(dcComment.getBoardNum())
                .writer(dcComment.getWriter())
                .content(dcComment.getContent())
                .regDate(dcComment.getRegDate())
                .reply(dcComment.isReply())
                .targetNum(dcComment.getTargetNum())
                .build();
    }
}
