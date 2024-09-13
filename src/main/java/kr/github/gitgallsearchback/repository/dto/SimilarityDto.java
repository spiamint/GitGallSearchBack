package kr.github.gitgallsearchback.repository.dto;

import lombok.Data;
import lombok.ToString;

@Data
public class SimilarityDto {

    private Long boardId;
    private Long dcNum;
    private String title;
    private String similarity;
    @ToString.Exclude
    private Object embeddingValue;
    private String url;
    private String baseUrl = "https://gall.dcinside.com/board/view/?id=granblue&no=";

    /**
     * SimilarityDtoInterface 를 SimilarityDto 로 변환 (nativeQuery 반환으로 interface 사용에 따라)
     * @param similarityDtoInterface
     * @return
     */
    public static SimilarityDto of(SimilarityDtoInterface similarityDtoInterface) {
        SimilarityDto similarityDto = new SimilarityDto();
        similarityDto.setBoardId(similarityDtoInterface.getBoardId());
        similarityDto.setTitle(similarityDtoInterface.getTitle());
        similarityDto.setSimilarity(similarityDtoInterface.getSimilarity());
        similarityDto.setEmbeddingValue(similarityDtoInterface.getFormedEmbeddingValue());
        similarityDto.setDcNum(similarityDtoInterface.getDcNum());
        similarityDto.setUrl(similarityDto.getBaseUrl() + similarityDto.getDcNum());
        return similarityDto;
    }

}
