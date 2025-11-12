package com.lake_team.fistserios.service;

import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsApiService {

    private final NewsRepository repo;

    public List<NewsItem> fetchAndSaveTopHeadlines(String country) {
        // TODO replace with real HTTP call + mapping
        List<NewsItem> items = new ArrayList<>();
        items.add(NewsItem.builder()
                .title("Demo headline")
                .description("This is a demo item. Replace with real API data.")
                .url("https://example.com/demo")
                .imageUrl(null)
                .publishedAt(LocalDateTime.now())
                .source("DemoSource")
                .build());

        repo.saveAll(items);
        return items;
    }

    @Async
    public void refreshAsync() {
        fetchAndSaveTopHeadlines("us");
    }
}
