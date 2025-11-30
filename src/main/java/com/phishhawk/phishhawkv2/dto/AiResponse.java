package com.phishhawk.phishhawkv2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data // Lombok makes this cleaner
public class AiResponse {
    private String label;

    @JsonProperty("confidence_score")
    private double confidenceScore;

    @JsonProperty("confidence_percentage")
    private String confidencePercentage;

    @JsonProperty("is_spam")
    private boolean isSpam;
}