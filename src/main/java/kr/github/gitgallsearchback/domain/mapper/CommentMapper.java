package kr.github.gitgallsearchback.domain.mapper;

import kr.github.gitgallsearchback.domain.Comment;
import kr.github.gitgallsearchback.scraper.dto.DcComment;

public class CommentMapper {

    public static Comment fromDto(DcComment dcComment) {
        return Comment.builder()
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
