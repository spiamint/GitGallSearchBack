package kr.github.gitgallsearchback.scraper.dto;

import lombok.Getter;

@Getter
public class ScrapeRequest {

    private long startPage;
    private long endPage;
    private long interval;
    private String galleryId;

    /**
     * 스크래핑 요청 생성
     * @param galleryId : 갤러리 id (주소창의 lists/?id= 뒤에 있는 값)
     * @param startPage : 시작 페이지
     * @param endPage : 끝 페이지
     * @param interval : callback 을 실행할 간격
     * @return
     */
    public static ScrapeRequest of(String galleryId, long startPage, long endPage, long interval) {
        return new ScrapeRequest(galleryId, startPage, endPage, interval);
    }

    /**
     * 스크래핑 요청 생성
     * @param galleryId : 갤러리 id (주소창의 lists/?id= 뒤에 있는 값)
     * @param startPage : 시작 페이지
     * @param endPage : 끝 페이지
     * @return
     */
    public static ScrapeRequest of(String galleryId, long startPage, long endPage) {
        return new ScrapeRequest(galleryId, startPage, endPage, 0);
    }

    protected ScrapeRequest(String galleryId, long startPage, long endPage, long interval) {
        if (galleryId == null || galleryId.isEmpty()) {
            throw new IllegalArgumentException("galleryId must not be null or empty");
        }
        if (startPage < 0) {
            throw new IllegalArgumentException("startPage must not be less than zero");
        }

        if (endPage < 0 || endPage < startPage) {
            throw new IllegalArgumentException("endPage must not be less than zero or startpage");
        }

        if (interval < 0 || interval > endPage - startPage + 1) {
            throw new IllegalArgumentException("interval must not be less than zero or more than (endPage - startPage + 1)");
        }
        this.galleryId = galleryId;
        this.startPage = startPage;
        this.endPage = endPage;
        this.interval = interval;
    }



}
