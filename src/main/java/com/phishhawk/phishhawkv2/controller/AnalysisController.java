package com.phishhawk.phishhawkv2.controller;

import com.phishhawk.phishhawkv2.dto.AnalysisResult;
import com.phishhawk.phishhawkv2.service.EmailAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/analyze")
public class AnalysisController {

    private final EmailAnalysisService emailAnalysisService;

    @Autowired
    public AnalysisController(EmailAnalysisService emailAnalysisService) {
        this.emailAnalysisService = emailAnalysisService;
    }

    @PostMapping("/email-content")
    public AnalysisResult analyzeEmailContent(@RequestBody String emailContent) {
        return emailAnalysisService.analyzeEmailContent(emailContent);
    }

    @PostMapping("/email")
    public AnalysisResult analyzeEmailFile(@RequestParam("emailFile") MultipartFile file) {
        return emailAnalysisService.analyzeEmail(file);
    }
}