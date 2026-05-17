package com.lake_team.fistserios.model.reddit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedditPost {
    private String title;
    private String url;
    private String domain;
    private String selftext;
    private String subreddit;

    @JsonProperty("created_utc")
    private double createdUtc;

    @JsonProperty("is_self")
    private boolean isSelf;

    private int score;
}
