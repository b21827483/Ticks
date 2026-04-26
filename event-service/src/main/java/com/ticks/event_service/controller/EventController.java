package com.ticks.event_service.controller;

import com.ticks.event_service.dto.request.CreateEventRequestDTO;
import com.ticks.event_service.dto.request.CreateScheduleRequestDTO;
import com.ticks.event_service.dto.request.UpdateEventRequestDTO;
import com.ticks.event_service.dto.response.ApiResponseDTO;
import com.ticks.event_service.dto.response.EventResponseDTO;
import com.ticks.event_service.dto.response.ScheduleResponseDTO;
import com.ticks.event_service.security.CurrentUser;
import com.ticks.event_service.service.EventService;
import com.ticks.event_service.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService    eventService;
    private final ScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<Page<EventResponseDTO>>> getPublished(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID venueId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(ApiResponseDTO.success("Events retrieved",
                eventService.getPublished(categoryId, venueId, keyword, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<EventResponseDTO>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponseDTO.success("Event retrieved", eventService.getById(id)));
    }

    @GetMapping("/{id}/schedules")
    public ResponseEntity<ApiResponseDTO<List<ScheduleResponseDTO>>> getSchedules(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponseDTO.success("Schedules retrieved",
                scheduleService.getByEvent(id)));
    }

    // Admin actions
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ORGANIZER','ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<EventResponseDTO>> create(
            @Valid @RequestBody CreateEventRequestDTO request,
            @CurrentUser UUID organizerId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("Event created",
                        eventService.create(request, organizerId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ORGANIZER','ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<EventResponseDTO>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequestDTO request,
            @CurrentUser UUID requesterId) {

        return ResponseEntity.ok(ApiResponseDTO.success("Event updated",
                eventService.update(id, request, requesterId)));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ROLE_ORGANIZER','ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> publish(
            @PathVariable UUID id,
            @CurrentUser UUID requesterId) {

        eventService.publish(id, requesterId);
        return ResponseEntity.ok(ApiResponseDTO.success("Event published"));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ROLE_ORGANIZER','ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> cancel(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @CurrentUser UUID requesterId) {

        String reason = body.getOrDefault("reason", "No reason provided");
        eventService.cancel(id, reason, requesterId);
        return ResponseEntity.ok(ApiResponseDTO.success("Event cancelled"));
    }

    @PostMapping("/{id}/schedules")
    @PreAuthorize("hasAnyRole('ROLE_ORGANIZER','ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<ScheduleResponseDTO>> addSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody CreateScheduleRequestDTO request,
            @CurrentUser UUID requesterId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("Schedule added",
                        scheduleService.addSchedule(id, request, requesterId)));
    }

    @PostMapping("/{eventId}/schedules/{scheduleId}/on-sale")
    @PreAuthorize("hasAnyRole('ROLE_ORGANIZER','ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<ScheduleResponseDTO>> putOnSale(
            @PathVariable UUID eventId,
            @PathVariable UUID scheduleId,
            @CurrentUser UUID requesterId) {
        return ResponseEntity.ok(ApiResponseDTO.success("Schedule on sale",
                scheduleService.putOnSale(scheduleId, requesterId)));
    }

    @PostMapping("/{eventId}/schedules/{scheduleId}/cancel")
    @PreAuthorize("hasAnyRole('ROLE_ORGANIZER','ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<ScheduleResponseDTO>> cancelSchedule(
            @PathVariable UUID eventId,
            @PathVariable UUID scheduleId,
            @CurrentUser UUID requesterId) {
        return ResponseEntity.ok(ApiResponseDTO.success("Schedule cancelled",
                scheduleService.cancel(scheduleId, requesterId)));
    }
}