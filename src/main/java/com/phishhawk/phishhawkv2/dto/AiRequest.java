package com.phishhawk.phishhawkv2.dto;

// No need for Lombok here, it's simple enough
public class AiRequest {
    private String text;

    public AiRequest() {} // Default constructor for JSON mapping

    public AiRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}