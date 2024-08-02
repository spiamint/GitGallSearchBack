package kr.granblue.seleniumcrawler.controller;

import kr.granblue.seleniumcrawler.controller.form.CrawlStartForm;
import kr.granblue.seleniumcrawler.crawler.DcCrawler;
import kr.granblue.seleniumcrawler.repository.DcBoardRepository;
import kr.granblue.seleniumcrawler.repository.dto.SimilarityDto;
import kr.granblue.seleniumcrawler.repository.dto.SimilarityDtoInterface;
import kr.granblue.seleniumcrawler.service.DcBoardEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@Slf4j
@RequiredArgsConstructor
public class IndexController {

    private final DcCrawler dcCrawler;
    private final DcBoardRepository dcBoardRepository;
    private final EmbeddingClient embeddingClient;
    private final DcBoardEmbeddingService boardEmbeddingService;

    @RequestMapping("/command")
    @Transactional
    public String command() {
//        embeddingRepository.alterTimeout();
//        embeddingRepository.alterTimeout2();
//        embeddingRepository.deleteRow();
//        embeddingRepository.deleteRow2();
        return "index";
    }


    @RequestMapping("/start")
    public String start(@ModelAttribute("crawlStartForm") CrawlStartForm crawlStartForm){
        try {
            dcCrawler.crawlStart(crawlStartForm.getStartPage(), crawlStartForm.getEndPage());
        } catch (Exception e) {
            log.error("[ERROR] dcCrawler.crawlStart();=====================================");
            e.printStackTrace();
            dcCrawler.getWebDriver().quit(); // 예외 발생시 드라이버 종료
        }
        return "redirect:/";
    }

    @RequestMapping("/embed")
    public String embed() {
        boardEmbeddingService.embedNotEmbedded();
        return "redirect:/";
    }

    @RequestMapping("/embed-loop")
    public String embedLoop() {
        boardEmbeddingService.embedLoop();
        return "redirect:/";
    }

    @RequestMapping("/search")
    public String search(@RequestParam(name = "query") String query) {
        log.info("query: {}", query);
        List<Double> embeddedQuery = embeddingClient.embed(query);
        List<SimilarityDtoInterface> similarityTop10 = dcBoardRepository.getSimilarityTop10(embeddedQuery.toString());
        List<SimilarityDto> similarityDtos = similarityTop10.stream().map(SimilarityDto::of).toList();
        similarityDtos.forEach(similarityDto -> {
            log.info("similarityDto: {}", similarityDto);
        });
        return "index";
    }

}


