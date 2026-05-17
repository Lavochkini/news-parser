package com.lake_team.fistserios.repository;

import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.model.NewsSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<NewsItem, Long>, JpaSpecificationExecutor<NewsItem> {

    Page<NewsItem> findAllByOrderByPublishedAtDesc(Pageable pageable);

    Optional<NewsItem> findByUrl(String url);

    Optional<NewsItem> findTopByOrderByPublishedAtDesc();

    @Query("SELECT DISTINCT n.category FROM NewsItem n WHERE n.category IS NOT NULL ORDER BY n.category")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT n.sourceType FROM NewsItem n WHERE n.sourceType IS NOT NULL")
    List<NewsSourceType> findDistinctSourceTypes();
}
