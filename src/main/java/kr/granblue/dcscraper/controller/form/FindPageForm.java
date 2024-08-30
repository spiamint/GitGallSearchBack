package kr.granblue.dcscraper.controller.form;

import lombok.Data;

@Data
public class FindPageForm {
    private int year;
    private int month;
    private int day;
    private String galleryId;
    private String isMinorGallery;
}
