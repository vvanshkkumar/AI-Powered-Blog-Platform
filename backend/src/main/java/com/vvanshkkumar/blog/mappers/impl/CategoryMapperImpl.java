package com.vvanshkkumar.blog.mappers.impl;

import com.vvanshkkumar.blog.domain.dtos.CategoryDto;
import com.vvanshkkumar.blog.domain.dtos.CreateCategoryRequest;
import com.vvanshkkumar.blog.domain.entities.Category;
import com.vvanshkkumar.blog.mappers.CategoryMapper;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapperImpl implements CategoryMapper {

  @Override
  public CategoryDto toDto(Category category) {
    return CategoryDto.builder()
        .id(category.getId())
        .name(category.getName())
        .postCount(category.getPosts() != null ? category.getPosts().size() : 0)
        .build();
  }

  @Override
  public Category toEntity(CreateCategoryRequest createCategoryRequest) {
    return Category.builder()
        .name(createCategoryRequest.getName())
        .build();
  }
}
