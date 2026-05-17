package com.lake_team.fistserios.repository;

import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.model.NewsSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsRepository extends MongoRepository<NewsItem, String> {

    Page<NewsItem> findAllByOrderByPublishedAtDesc(Pageable pageable);

    Page<NewsItem> findByCategoryIgnoreCaseOrderByPublishedAtDesc(String category, Pageable pageable);

    Page<NewsItem> findBySourceTypeOrderByPublishedAtDesc(NewsSourceType sourceType, Pageable pageable);

    Page<NewsItem> findByCategoryIgnoreCaseAndSourceTypeOrderByPublishedAtDesc(
            String category, NewsSourceType sourceType, Pageable pageable);

    Optional<NewsItem> findByUrl(String url);

    Optional<NewsItem> findTopByOrderByPublishedAtDesc();
}
