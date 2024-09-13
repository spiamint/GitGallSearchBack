package kr.github.gitgallsearchback.scraper.service;

import java.time.LocalDateTime;

public interface DcPageFinder {

    void findFirstPageByDate(LocalDateTime inputDateTime, String galleryId, boolean isMinorGallery);
}
