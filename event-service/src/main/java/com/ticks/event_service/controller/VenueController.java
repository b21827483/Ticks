package com.ticks.event_service.controller;

import com.ticks.event_service.dto.request.CreateVenueRequestDTO;
import com.ticks.event_service.dto.request.UpdateVenueRequestDTO;
import com.ticks.event_service.dto.response.ApiResponseDTO;
import com.ticks.event_service.dto.response.VenueResponseDTO;
import com.ticks.event_service.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<Page<VenueResponseDTO>>> getAll(
            @RequestParam(required = false) String city,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDTO.success("Venues retrieved", venueService.getAll(city, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<VenueResponseDTO>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponseDTO.success("Venue retrieved", venueService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<VenueResponseDTO>> create(
            @Valid @RequestBody CreateVenueRequestDTO request) {
        return ResponseEntity.ok(ApiResponseDTO.success("Venue created", venueService.create(request)));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<VenueResponseDTO>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateVenueRequestDTO request) {
        return ResponseEntity.ok(ApiResponseDTO.success("Venue updated", venueService.update(id, request)));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> deactivate(@PathVariable UUID id) {
        venueService.deactivate(id);
        return ResponseEntity.ok(ApiResponseDTO.success("Venue deactivated"));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> reactivate(@PathVariable UUID id) {
        venueService.reactivate(id);
        return ResponseEntity.ok(ApiResponseDTO.success("Venue reactivated"));
    }
}
