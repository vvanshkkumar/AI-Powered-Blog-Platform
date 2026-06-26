package com.vvanshkkumar.blog.domain.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlogGenerateRequest {

    @NotBlank(message = "Topic is required")
    private String topic;
}
