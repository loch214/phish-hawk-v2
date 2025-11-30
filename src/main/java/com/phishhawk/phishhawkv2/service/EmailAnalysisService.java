package com.phishhawk.phishhawkv2.service;

import com.phishhawk.phishhawkv2.dto.AiRequest;
import com.phishhawk.phishhawkv2.dto.AiResponse;
import com.phishhawk.phishhawkv2.dto.AnalysisResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EmailAnalysisService {

    private final WebClient webClient;

    public EmailAnalysisService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://127.0.0.1:5000").build();
    }

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?:(?:https?|ftp)://|www\\.|ftp\\.)(?:\\([-A-Z0-9+&@#/%=~_|$?!:,.]*\\)|[-A-Z0-9+&@#/%=~_|$?!:,.])*(?:\\([-A-Z0-9+&@#/%=~_|$?!:,.]*\\)|[A-Z0-9+&@#/%=~_|$])",
            Pattern.CASE_INSENSITIVE);

    private static final List<String> SUSPICIOUS_CLOUD_DOMAINS = Arrays.asList(
            "googleapis.com", "firebasestorage.com", "amazonaws.com", "blob.core.windows.net",
            "dropbox.com", "drive.google.com", "docs.google.com", "herokuapp.com");

    public AnalysisResult analyzeEmail(MultipartFile emailFile) {
        if (emailFile == null || emailFile.isEmpty()) return createErrorResult("Error: No file provided.");
        try {
            String content = "";
            String filename = emailFile.getOriginalFilename();
            if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                try (PDDocument document = PDDocument.load(emailFile.getInputStream())) {
                    content = new PDFTextStripper().getText(document);
                }
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(emailFile.getInputStream()))) {
                    content = reader.lines().collect(Collectors.joining("\n"));
                }
            }
            return analyzeEmailContent(content);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResult("Critical Error processing file: " + e.getMessage());
        }
    }

    public AnalysisResult analyzeEmailContent(String emailContent) {
        if (emailContent == null || emailContent.trim().isEmpty()) return createErrorResult("Error: No content provided.");
        AnalysisResult result = new AnalysisResult();
        result.setFoundUrls(new ArrayList<>());
        try {
            extractBasicInfo(result, emailContent);
            List<String> technicalWarnings = checkTechnicalRules(result);

            try {
                callAiService(result, emailContent);
            } catch (Exception e) {
                System.err.println("❌ Python AI Failed: " + e.getMessage());
                result.setAiEnabled(false);
                technicalWarnings.add("AI Service is offline.");
            }

            finalizeVerdict(result, technicalWarnings);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResult("Error processing content.");
        }
        return result;
    }

    private void callAiService(AnalysisResult result, String emailContent) {
        System.out.println("🦅 Sending to Python AI...");
        AiRequest request = new AiRequest(emailContent);
        AiResponse response = webClient.post()
                .uri("/predict")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiResponse.class)
                .block();

        if (response != null) {
            result.setAiEnabled(true);
            result.setAiVerdict(response.getLabel());
            result.setAiConfidence(response.getConfidencePercentage());
            result.setAiSuspicious(response.isSpam());
            System.out.println("✅ AI Response: " + response.getLabel());
        }
    }

    private void finalizeVerdict(AnalysisResult result, List<String> technicalWarnings) {
        boolean isTechnicallySuspicious = !technicalWarnings.isEmpty();
        boolean isAiSuspicious = result.isAiSuspicious();
        result.setSuspicious(isTechnicallySuspicious || isAiSuspicious);
        StringBuilder summary = new StringBuilder();
        if (result.isAiEnabled()) {
            summary.append("AI Verdict: ").append(result.getAiVerdict())
                    .append(" (").append(result.getAiConfidence()).append(")");
        } else {
            summary.append("AI Unavailable");
        }
        if (isTechnicallySuspicious) {
            summary.append(" | Technical Issues: ").append(String.join(", ", technicalWarnings));
        } else if (!isAiSuspicious) {
            summary.append(" | No technical threats found.");
        }
        result.setAnalysisSummary(summary.toString());
    }

    private void extractBasicInfo(AnalysisResult result, String emailContent) {
        String fromHeader = "Not Found";
        String returnPathHeader = "Not Found";
        for (String line : emailContent.split("\n")) {
            if (line.toLowerCase().startsWith("from:")) fromHeader = line.substring(5).trim();
            if (line.toLowerCase().startsWith("return-path:")) returnPathHeader = line.substring(12).trim();
        }
        result.setFromHeader(fromHeader);
        result.setReturnPathHeader(returnPathHeader);
        Document doc = Jsoup.parse(emailContent);
        for (Element link : doc.select("a[href]")) result.getFoundUrls().add(link.attr("href"));
        Matcher matcher = URL_PATTERN.matcher(emailContent);
        while (matcher.find()) {
            String url = matcher.group(0);
            if (!result.getFoundUrls().contains(url)) result.getFoundUrls().add(url);
        }
    }

    private List<String> checkTechnicalRules(AnalysisResult result) {
        List<String> issues = new ArrayList<>();
        String fromDomain = getDomainFromEmail(result.getFromHeader());
        String returnPathDomain = getDomainFromEmail(result.getReturnPathHeader());
        if (returnPathDomain != null && fromDomain != null && !fromDomain.equalsIgnoreCase(returnPathDomain)) {
            issues.add("Header Spoofing detected");
        }
        if (result.getFoundUrls() != null && !result.getFoundUrls().isEmpty()) {
            for (String url : result.getFoundUrls()) {
                String linkDomain = getDomainFromUrl(url);
                if (linkDomain == null) continue;
                for (String cloud : SUSPICIOUS_CLOUD_DOMAINS) {
                    if (linkDomain.contains(cloud)) issues.add("Suspicious Cloud Link: " + cloud);
                }
            }
        }
        return issues;
    }

    private String getDomainFromEmail(String email) {
        if (email == null || !email.contains("@")) return null;
        int atIndex = email.lastIndexOf('@');
        String part = email.substring(atIndex + 1);
        if (part.contains(">")) part = part.substring(0, part.indexOf('>'));
        return part.trim();
    }

    private String getDomainFromUrl(String url) {
        try {
            return new java.net.URI(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private AnalysisResult createErrorResult(String message) {
        AnalysisResult result = new AnalysisResult();
        result.setSuspicious(true);
        result.setAnalysisSummary(message);
        result.setFoundUrls(new ArrayList<>());
        return result;
    }
}