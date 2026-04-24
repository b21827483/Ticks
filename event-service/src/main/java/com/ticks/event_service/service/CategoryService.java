package com.ticks.event_service.service;

import com.ticks.event_service.dto.request.CreateCategoryRequestDTO;
import com.ticks.event_service.dto.response.CategoryResponseDTO;
import com.ticks.event_service.entity.Category;
import com.ticks.event_service.exception.CategoryNotFoundException;
import com.ticks.event_service.exception.DuplicateResourceException;
import com.ticks.event_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponseDTO create(CreateCategoryRequestDTO request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category already exists: " + request.getName());
        }

        Category.CategoryBuilder builder = Category.builder()
                .name(request.getName())
                .slug(request.getSlug() != null ? request.getSlug() : toSlug(request.getName()))
                .description(request.getDescription())
                .imageUrl(request.getImageUrl());

        Category saved = categoryRepository.save(builder.build());
        log.info("Category created: {}", saved.getName());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO getById(UUID id) {
        return categoryRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + id));
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO getBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .map(this::mapToResponse)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + slug));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + id));
        categoryRepository.delete(category);
        log.info("Category deleted: {}", id);
    }

    private CategoryResponseDTO mapToResponse(Category category) {
        CategoryResponseDTO.CategoryResponseDTOBuilder builder = CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt());

        return builder.build();
    }

    private String toSlug(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
