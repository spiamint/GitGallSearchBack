package kr.granblue.seleniumcrawler.domain;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public class Reply {
    public Long id;
    public Long boardId;
    public String writer;
    public String content;
    public String regDate;
}
