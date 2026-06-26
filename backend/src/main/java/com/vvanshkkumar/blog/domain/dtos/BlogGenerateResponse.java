package com.vvanshkkumar.blog.domain.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlogGenerateResponse {

    private String title;
    private String content;
    private List<String> suggestedTags;
}
