package com.phishhawk.phishhawkv2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class PhishHawkV2Application {

    public static void main(String[] args) {
        SpringApplication.run(PhishHawkV2Application.class, args);
    }

    // This creates the tool that allows Java to talk to the Python API
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}