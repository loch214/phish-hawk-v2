package com.phishhawk.phishhawkv2.dto;

import lombok.Data;
import java.util.List;

@Data // Lombok annotation to generate getters, setters, etc.
public class AnalysisResult {
    private String fromHeader;
    private String returnPathHeader;
    private List<String> foundUrls;
    private boolean suspicious;
    private String analysisSummary;

    // Fields for the AI Dashboard
    private boolean aiEnabled;
    private String aiVerdict;
    private String aiConfidence;
    private boolean aiSuspicious;
}