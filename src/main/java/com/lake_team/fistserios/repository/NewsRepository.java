package com.lake_team.fistserios.repository;

import com.lake_team.fistserios.model.NewsItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<NewsItem, Long> {

    Page<NewsItem> findAllByOrderByPublishedAtDesc(Pageable pageable);

    Optional<NewsItem> findByUrl(String url);
    Optional<NewsItem> findTopByOrderByPublishedAtDesc();
}
