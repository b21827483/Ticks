package com.ticks.event_service.service;

import com.ticks.event_service.dto.request.CreateEventRequestDTO;
import com.ticks.event_service.dto.request.UpdateEventRequestDTO;
import com.ticks.event_service.dto.response.CategoryResponseDTO;
import com.ticks.event_service.dto.response.EventResponseDTO;
import com.ticks.event_service.dto.response.ScheduleResponseDTO;
import com.ticks.event_service.dto.response.VenueResponseDTO;
import com.ticks.event_service.entity.*;
import com.ticks.event_service.exception.*;
import com.ticks.event_service.repository.CategoryRepository;
import com.ticks.event_service.repository.EventRepository;
import com.ticks.event_service.repository.EventScheduleRepository;
import com.ticks.event_service.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository         eventRepository;
    private final VenueRepository         venueRepository;
    private final CategoryRepository      categoryRepository;
    private final EventScheduleRepository scheduleRepository;

    @Transactional
    public EventResponseDTO create(CreateEventRequestDTO request, UUID organizerId) {
        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new VenueNotFoundException("Venue not found: " + request.getVenueId()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + request.getCategoryId()));

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .price(request.getPrice())
                .status(EventStatus.DRAFT)
                .organizerId(organizerId)
                .venue(venue)
                .category(category)
                .build();

        event = eventRepository.save(event);
        log.info("Event created: {} by organizer {}", event.getId(), organizerId);
        return mapToResponse(event);
    }

    @Transactional(readOnly = true)
    public EventResponseDTO getById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + id));
        return mapToResponse(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getPublished(UUID categoryId, UUID venueId,
                                               String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return eventRepository.searchByTitle(keyword, EventStatus.PUBLISHED, pageable)
                    .map(this::mapToResponse);
        }
        if (categoryId != null) {
            return eventRepository.findByCategoryIdAndStatus(categoryId, EventStatus.PUBLISHED, pageable)
                    .map(this::mapToResponse);
        }
        if (venueId != null) {
            return eventRepository.findByVenueIdAndStatus(venueId, EventStatus.PUBLISHED, pageable)
                    .map(this::mapToResponse);
        }
        return eventRepository.findByStatus(EventStatus.PUBLISHED, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getByOrganizer(UUID organizerId, Pageable pageable) {
        return eventRepository.findByOrganizerIdAndStatus(organizerId, EventStatus.PUBLISHED, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public EventResponseDTO update(UUID id, UpdateEventRequestDTO request, UUID requesterId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + id));

        assertOwnerOrAdmin(event, requesterId);

        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) {
            throw new InvalidEventStateException(
                    "Cannot update a " + event.getStatus().name().toLowerCase() + " event");
        }

        if (request.getTitle()       != null) event.setTitle(request.getTitle());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getImageUrl()    != null) event.setImageUrl(request.getImageUrl());
        if (request.getPrice()       != null) event.setPrice(request.getPrice());

        if (request.getVenueId() != null) {
            Venue venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new VenueNotFoundException("Venue not found: " + request.getVenueId()));
            event.setVenue(venue);
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + request.getCategoryId()));
            event.setCategory(category);
        }

        eventRepository.save(event);
        log.info("Event updated: {}", id);
        return mapToResponse(event);
    }

    @Transactional
    public void publish(UUID id, UUID requesterId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + id));

        assertOwnerOrAdmin(event, requesterId);

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new InvalidEventStateException(
                    "Only DRAFT events can be published, current status: " + event.getStatus());
        }

        List<EventSchedule> schedules = scheduleRepository.findByEventIdOrderByStartTime(id);
        if (schedules.isEmpty()) {
            throw new InvalidEventStateException(
                    "Event must have at least one schedule before it can be published");
        }

        eventRepository.updateStatus(id, EventStatus.PUBLISHED);
        log.info("Event published: {}", id);
    }

    // Cancels a PUBLISHED event.
    @Transactional
    public void cancel(UUID id, String reason, UUID requesterId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + id));

        assertOwnerOrAdmin(event, requesterId);

        if (event.getStatus() == EventStatus.CANCELLED
                || event.getStatus() == EventStatus.COMPLETED) {
            throw new InvalidEventStateException(
                    "Cannot cancel a " + event.getStatus().name().toLowerCase() + " event");
        }

        eventRepository.updateStatus(id, EventStatus.CANCELLED);
        scheduleRepository.updateStatusByEventId(id, ScheduleStatus.CANCELLED);
        log.info("Event cancelled: {} reason: {}", id, reason);
    }

    // Mapping
    private EventResponseDTO mapToResponse(Event event) {
        VenueResponseDTO venueDTO = VenueResponseDTO.builder()
                .id(event.getVenue().getId())
                .name(event.getVenue().getName())
                .address(event.getVenue().getAddress())
                .city(event.getVenue().getCity())
                .country(event.getVenue().getCountry())
                .capacity(event.getVenue().getCapacity())
                .status(event.getVenue().getStatus())
                .build();

        CategoryResponseDTO categoryDTO = CategoryResponseDTO.builder()
                .id(event.getCategory().getId())
                .name(event.getCategory().getName())
                .slug(event.getCategory().getSlug())
                .build();

        List<ScheduleResponseDTO> scheduleDTOs = scheduleRepository
                .findByEventIdOrderByStartTime(event.getId()).stream()
                .map(s -> ScheduleResponseDTO.builder()
                        .id(s.getId())
                        .eventId(event.getId())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .doorsOpenTime(s.getDoorsOpenTime())
                        .totalCapacity(s.getTotalCapacity())
                        .status(s.getStatus())
                        .createdAt(s.getCreatedAt())
                        .updatedAt(s.getUpdatedAt())
                        .build())
                .toList();

        return EventResponseDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .imageUrl(event.getImageUrl())
                .price(event.getPrice())
                .status(event.getStatus())
                .organizerId(event.getOrganizerId())
                .venue(venueDTO)
                .category(categoryDTO)
                .schedules(scheduleDTOs)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    // Organizers can only mutate their own events.
    private void assertOwnerOrAdmin(Event event, UUID requesterId) {
        if (!event.getOrganizerId().equals(requesterId)) {
            throw new AccessDeniedException(
                    "You do not have permission to modify this event");
        }
    }
}
