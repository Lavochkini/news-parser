package com.lake_team.fistserios.repository;

import com.lake_team.fistserios.model.NewsItem;
import com.lake_team.fistserios.model.NewsSourceType;
import org.springframework.data.jpa.domain.Specification;

public class NewsSpecification {

    public static Specification<NewsItem> hasCategory(String category) {
        return (root, query, cb) -> {
            if (category == null || category.isBlank()) return cb.conjunction();
            return cb.equal(cb.lower(root.get("category")), category.toLowerCase());
        };
    }

    public static Specification<NewsItem> hasSourceType(NewsSourceType sourceType) {
        return (root, query, cb) -> {
            if (sourceType == null) return cb.conjunction();
            return cb.equal(root.get("sourceType"), sourceType);
        };
    }
}
