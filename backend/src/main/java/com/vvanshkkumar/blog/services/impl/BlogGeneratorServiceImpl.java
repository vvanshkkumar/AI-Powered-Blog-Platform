package com.vvanshkkumar.blog.services.impl;

import com.vvanshkkumar.blog.domain.dtos.BlogGenerateResponse;
import com.vvanshkkumar.blog.services.BlogGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class BlogGeneratorServiceImpl implements BlogGeneratorService {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Override
    public BlogGenerateResponse generate(String topic) {
        String url = aiServiceUrl + "/generate";

        Map<String, String> requestBody = Map.of("topic", topic);

        try {
            ResponseEntity<BlogGenerateResponse> response = restTemplate.postForEntity(
                    url,
                    requestBody,
                    BlogGenerateResponse.class
            );
            return response.getBody();
        } catch (RestClientException e) {
            throw new RestClientException("AI service is currently unavailable: " + e.getMessage(), e);
        }
    }
}
