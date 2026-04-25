package com.ticks.event_service.service;

import com.ticks.event_service.dto.request.CreateScheduleRequestDTO;
import com.ticks.event_service.dto.response.ScheduleResponseDTO;
import com.ticks.event_service.entity.*;
import com.ticks.event_service.exception.EventNotFoundException;
import com.ticks.event_service.exception.InvalidEventStateException;
import com.ticks.event_service.exception.ScheduleNotFoundException;
import com.ticks.event_service.repository.EventRepository;
import com.ticks.event_service.repository.EventScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService{

    private final EventScheduleRepository scheduleRepository;
    private final EventRepository eventRepository;

    @Transactional
    public ScheduleResponseDTO addSchedule(UUID eventId,
                                           CreateScheduleRequestDTO request,
                                           UUID requesterId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));

        assertOwner(event, requesterId);

        if (event.getStatus() == EventStatus.CANCELLED
                || event.getStatus() == EventStatus.COMPLETED) {
            throw new InvalidEventStateException(
                    "Cannot add a schedule to a " + event.getStatus().name().toLowerCase() + " event");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new InvalidEventStateException("End time must be after start time");
        }

        EventSchedule schedule = EventSchedule.builder()
                .event(event)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .doorsOpenTime(request.getDoorsOpenTime())
                .totalCapacity(request.getTotalCapacity())
                .status(ScheduleStatus.SCHEDULED)
                .build();

        schedule = scheduleRepository.save(schedule);
        log.info("Schedule added to event {}: {}", eventId, schedule.getId());
        return mapToResponse(schedule);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponseDTO> getByEvent(UUID eventId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));
        return scheduleRepository.findByEventIdOrderByStartTime(eventId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleResponseDTO getById(UUID scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ScheduleNotFoundException("Schedule not found: " + scheduleId));
    }

    @Transactional
    public ScheduleResponseDTO putOnSale(UUID scheduleId, UUID requesterId) {
        EventSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException("Schedule not found: " + scheduleId));

        assertOwner(schedule.getEvent(), requesterId);

        if (schedule.getEvent().getStatus() != EventStatus.PUBLISHED) {
            throw new InvalidEventStateException(
                    "Event must be PUBLISHED before a schedule can go on sale");
        }
        if (schedule.getStatus() != ScheduleStatus.SCHEDULED) {
            throw new InvalidEventStateException(
                    "Only SCHEDULED schedules can be put on sale, current: " + schedule.getStatus());
        }

        scheduleRepository.updateStatus(scheduleId, ScheduleStatus.ON_SALE);
        schedule.setStatus(ScheduleStatus.ON_SALE);
        log.info("Schedule {} put on sale", scheduleId);
        return mapToResponse(schedule);
    }

    @Transactional
    public ScheduleResponseDTO cancel(UUID scheduleId, UUID requesterId) {
        EventSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException("Schedule not found: " + scheduleId));

        assertOwner(schedule.getEvent(), requesterId);

        if (schedule.getStatus() == ScheduleStatus.CANCELLED
                || schedule.getStatus() == ScheduleStatus.COMPLETED) {
            throw new InvalidEventStateException(
                    "Schedule is already " + schedule.getStatus().name().toLowerCase());
        }

        scheduleRepository.updateStatus(scheduleId, ScheduleStatus.CANCELLED);
        schedule.setStatus(ScheduleStatus.CANCELLED);
        log.info("Schedule {} cancelled", scheduleId);
        return mapToResponse(schedule);
    }

    private ScheduleResponseDTO mapToResponse(EventSchedule s) {
        return ScheduleResponseDTO.builder()
                .id(s.getId())
                .eventId(s.getEvent().getId())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .doorsOpenTime(s.getDoorsOpenTime())
                .totalCapacity(s.getTotalCapacity())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private void assertOwner(Event event, UUID requesterId) {
        if (!event.getOrganizerId().equals(requesterId)) {
            throw new AccessDeniedException("You do not have permission to modify this event");
        }
    }
}