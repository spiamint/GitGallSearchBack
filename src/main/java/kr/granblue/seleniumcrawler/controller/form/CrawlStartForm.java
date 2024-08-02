package kr.granblue.seleniumcrawler.controller.form;

import lombok.Data;

@Data
public class CrawlStartForm {
    private Long startPage;
    private Long endPage;
}
