package com.lake_team.fistserios.service;/*
  @author Bogdan
  @project fistserios
  @class NewsStartupChecker
  @version 1.0.0
  @since 17.09.2025 - 19.26
*/

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

    @EventListener(ApplicationReadyEvent.class)
    public void checkAndUpdateNews() {
        long count = newsRepository.count();

        if (count == 0) {
            log.info("News table is empty. Fetching news from API...");
            newsApiService.fetchAndSaveTopHeadlines("us");
            return;
        }

        newsRepository.findTopByOrderByPublishedAtDesc()
                .ifPresentOrElse(latestNews -> {
                    LocalDateTime publishedAt = latestNews.getPublishedAt();
                    LocalDateTime now = LocalDateTime.now();
                    if (publishedAt.isBefore(now.minusHours(168))) {
                        log.info("Latest news is older than 1 week. Fetching updates from API...");
                        newsApiService.fetchAndSaveTopHeadlines("us");
                    } else {
                        log.info("News are up-to-date. No API fetch needed.");
                    }
                }, () -> {
                    log.info("No news found. Fetching from API...");
                    newsApiService.fetchAndSaveTopHeadlines("us");
                });
    }
}