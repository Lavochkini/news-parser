package com.lake_team.fistserios.model.reddit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedditListing {
    private RedditListingData data;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RedditListingData {
        private List<RedditChild> children;
        private String after; // pagination cursor
    }
}
