package com.vvanshkkumar.blog.mappers;

import com.vvanshkkumar.blog.domain.dtos.CategoryDto;
import com.vvanshkkumar.blog.domain.dtos.CreateCategoryRequest;
import com.vvanshkkumar.blog.domain.entities.Category;

public interface CategoryMapper {

    CategoryDto toDto(Category category);

    Category toEntity(CreateCategoryRequest createCategoryRequest);

}
