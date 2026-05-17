package com.lake_team.fistserios.service;

import com.lake_team.fistserios.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsStartupChecker {

    private final NewsRepository newsRepository;
    private final NewsApiService newsApiService;
    private final GuardianService guardianService;
    private final GNewsService gNewsService;

    @EventListener(ApplicationReadyEvent.class)
    public void checkAndUpdateNews() {
        long count = newsRepository.count();

        if (count == 0) {
            log.info("News table is empty. Fetching from all sources...");
            fetchFromAllSources();
            return;
        }

        newsRepository.findTopByOrderByPublishedAtDesc()
                .ifPresentOrElse(latestNews -> {
                    LocalDateTime publishedAt = latestNews.getPublishedAt();
                    if (publishedAt.isBefore(LocalDateTime.now().minusHours(168))) {
                        log.info("News older than 1 week. Refreshing all sources...");
                        fetchFromAllSources();
                    } else {
                        log.info("News are up-to-date. No API fetch needed.");
                    }
                }, () -> {
                    log.info("No news found. Fetching from all sources...");
                    fetchFromAllSources();
                });
    }

    private void fetchFromAllSources() {
        newsApiService.fetchAndSaveTopHeadlines("us");
        guardianService.fetchAndSaveNews(null);
        gNewsService.fetchAndSaveTopHeadlines("en");
    }
}
