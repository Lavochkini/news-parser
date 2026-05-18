package com.lake_team.fistserios.analysis;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NewsAnalysisRepository extends MongoRepository<NewsAnalysis, String> {
    Optional<NewsAnalysis> findByNewsItemId(String newsItemId);
    boolean existsByNewsItemId(String newsItemId);
    List<NewsAnalysis> findAllByAnalyzedByUsernameOrderByAnalyzedAtDesc(String username);
}
