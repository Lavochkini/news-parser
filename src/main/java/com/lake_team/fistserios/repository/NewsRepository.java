package com.lake_team.fistserios.repository;/*
  @author Bogdan
  @project fistserios
  @class NewsRepository
  @version 1.0.0
  @since 16.09.2025 - 22.50
*/

import com.lake_team.fistserios.model.NewsItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<NewsItem, Long> {
    Optional<NewsItem> findByUrl(String url);

    Optional<NewsItem> findTopByOrderByPublishedAtDesc();
}
