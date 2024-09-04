package kr.granblue.gbfsearchback.service;

import kr.granblue.gbfsearchback.domain.DcComment;
import kr.granblue.gbfsearchback.domain.DcBoard;
import lombok.Data;

import java.util.List;

@Data
public class DcBoardAndComments {
    DcBoard dcBoard;
    List<DcComment> comments; // not null
}
