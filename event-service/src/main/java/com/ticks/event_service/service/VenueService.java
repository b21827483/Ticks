package com.ticks.event_service.service;

import com.ticks.event_service.dto.request.CreateVenueRequestDTO;
import com.ticks.event_service.dto.request.UpdateVenueRequestDTO;
import com.ticks.event_service.dto.response.VenueResponseDTO;
import com.ticks.event_service.entity.Venue;
import com.ticks.event_service.entity.VenueStatus;
import com.ticks.event_service.exception.DuplicateResourceException;
import com.ticks.event_service.exception.VenueNotFoundException;
import com.ticks.event_service.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;

    @Transactional
    public VenueResponseDTO create(CreateVenueRequestDTO request) {
        if (venueRepository.existsByNameAndCity(request.getName(), request.getCity())) {
            throw new DuplicateResourceException("Venue '" + request.getName() + "' already exists in " + request.getCity());
        }

        Venue venue = Venue.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .capacity(request.getCapacity())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .build();

        venueRepository.save(venue);
        log.info("Venue created: {} in {}", venue.getName(), venue.getCity());
        return mapToResponse(venue);
    }

    @Transactional(readOnly = true)
    public VenueResponseDTO getById(UUID id) {
        return venueRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new VenueNotFoundException("Venue not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<VenueResponseDTO> getAll(String city, Pageable pageable) {
        if (city != null && !city.isBlank()) {
            return venueRepository
                    .findByCityIgnoreCaseAndStatus(city, VenueStatus.ACTIVE, pageable)
                    .map(this::mapToResponse);
        }
        return venueRepository
                .findByStatus(VenueStatus.ACTIVE, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public VenueResponseDTO update(UUID id, UpdateVenueRequestDTO request) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException("Venue not found: " + id));

        if (request.getName()        != null) venue.setName(request.getName());
        if (request.getAddress()     != null) venue.setAddress(request.getAddress());
        if (request.getCity()        != null) venue.setCity(request.getCity());
        if (request.getCountry()     != null) venue.setCountry(request.getCountry());
        if (request.getPostalCode()  != null) venue.setPostalCode(request.getPostalCode());
        if (request.getCapacity()    != null) venue.setCapacity(request.getCapacity());
        if (request.getDescription() != null) venue.setDescription(request.getDescription());
        if (request.getImageUrl()    != null) venue.setImageUrl(request.getImageUrl());

        venueRepository.save(venue);
        log.info("Venue updated: {}", id);
        return mapToResponse(venue);
    }

    @Transactional
    public void deactivate(UUID id) {
        venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException("Venue not found: " + id));
        venueRepository.updateStatus(id, VenueStatus.INACTIVE);
        log.info("Venue deactivated: {}", id);
    }

    @Transactional
    public void reactivate(UUID id) {
        venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException("Venue not found: " + id));
        venueRepository.updateStatus(id, VenueStatus.ACTIVE);
        log.info("Venue reactivated: {}", id);
    }

    public VenueResponseDTO mapToResponse(Venue venue) {
        return VenueResponseDTO.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .city(venue.getCity())
                .country(venue.getCountry())
                .postalCode(venue.getPostalCode())
                .capacity(venue.getCapacity())
                .description(venue.getDescription())
                .imageUrl(venue.getImageUrl())
                .status(venue.getStatus())
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt())
                .build();
    }
}
