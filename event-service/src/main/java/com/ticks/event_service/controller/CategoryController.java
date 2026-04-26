package com.ticks.event_service.controller;

import com.ticks.event_service.dto.request.CreateCategoryRequestDTO;
import com.ticks.event_service.dto.response.ApiResponseDTO;
import com.ticks.event_service.dto.response.CategoryResponseDTO;
import com.ticks.event_service.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<CategoryResponseDTO>>> getAll() {
        return ResponseEntity.ok(
                ApiResponseDTO.success("Categories retrieved", categoryService.getAllCategories()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<CategoryResponseDTO>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponseDTO.success("Category retrieved", categoryService.getById(id)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponseDTO<CategoryResponseDTO>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(
                ApiResponseDTO.success("Category retrieved", categoryService.getBySlug(slug)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<CategoryResponseDTO>> create(
            @Valid @RequestBody CreateCategoryRequestDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("Category created", categoryService.create(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponseDTO.success("Category deleted"));
    }
}