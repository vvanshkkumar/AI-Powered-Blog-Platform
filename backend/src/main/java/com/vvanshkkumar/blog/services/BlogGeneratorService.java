package com.vvanshkkumar.blog.services;

import com.vvanshkkumar.blog.domain.dtos.BlogGenerateResponse;

public interface BlogGeneratorService {
    BlogGenerateResponse generate(String topic);
}
