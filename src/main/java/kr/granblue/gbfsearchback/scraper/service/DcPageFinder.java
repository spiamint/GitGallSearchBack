package kr.granblue.gbfsearchback.scraper.service;

import org.openqa.selenium.WebDriver;

import java.time.LocalDateTime;

public interface DcPageFinder {

    void findFirstPageByDate(LocalDateTime inputDateTime, String galleryId, boolean isMinorGallery, WebDriver webDriver);
}
