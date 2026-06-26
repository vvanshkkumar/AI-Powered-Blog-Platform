package com.vvanshkkumar.blog.controllers;

import com.vvanshkkumar.blog.domain.dtos.BlogGenerateRequest;
import com.vvanshkkumar.blog.domain.dtos.BlogGenerateResponse;
import com.vvanshkkumar.blog.services.BlogGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/posts/generate")
@RequiredArgsConstructor
public class BlogGeneratorController {

    private final BlogGeneratorService blogGeneratorService;

    @PostMapping
    public ResponseEntity<BlogGenerateResponse> generatePost(
            @Valid @RequestBody BlogGenerateRequest request,
            @RequestAttribute UUID userId) {
        BlogGenerateResponse response = blogGeneratorService.generate(request.getTopic());
        return ResponseEntity.ok(response);
    }
}
