package com.vvanshkkumar.blog.services.impl;

import com.vvanshkkumar.blog.domain.entities.Category;
import com.vvanshkkumar.blog.repositories.CategoryRepository;
import com.vvanshkkumar.blog.services.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> listCategories() {
        return categoryRepository.findAllWithPostCount();
    }

    @Override
    @Transactional
    public Category createCategory(Category category) {
        String categoryName = category.getName();
        if(categoryRepository.existsByNameIgnoreCase(categoryName)) {
            throw new IllegalArgumentException("Category already exists with name: " + categoryName);
        }
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category updateCategory(UUID id, Category category) {
        Category existingCategory = getCategoryById(id);
        String newName = category.getName();
        // Only check for duplicates if the name actually changed
        if (!existingCategory.getName().equalsIgnoreCase(newName)) {
            if (categoryRepository.existsByNameIgnoreCase(newName)) {
                throw new IllegalArgumentException("Category already exists with name: " + newName);
            }
        }
        existingCategory.setName(newName);
        return categoryRepository.save(existingCategory);
    }

    @Override
    public void deleteCategory(UUID id) {
        Optional<Category> category = categoryRepository.findById(id);
        if(category.isPresent()) {
            if(!category.get().getPosts().isEmpty()) {
                throw new IllegalStateException("Category has posts associated with it");
            }
            categoryRepository.deleteById(id);
        }
    }

    @Override
    public Category getCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id" + id));
    }

}
