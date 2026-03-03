package com.runalytics.metrics_engine.service;

import com.runalytics.metrics_engine.dto.ActivityMetricsDto;
import com.runalytics.metrics_engine.dto.LapMetricsDto;
import com.runalytics.metrics_engine.entity.ActivityMetrics;
import com.runalytics.metrics_engine.entity.LapMetrics;
import com.runalytics.metrics_engine.kafka.MetricsProducer;
import com.runalytics.metrics_engine.mapper.ActivityMetricsMapper;
import com.runalytics.metrics_engine.mapper.LapMetricsMapper;
import com.runalytics.metrics_engine.repository.ActivityMetricsRepository;
import com.runalytics.metrics_engine.repository.LapMetricsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock private ActivityMetricsCalculator activityCalculator;
    @Mock private ActivityMetricsRepository activityRepository;
    @Mock private LapMetricsRepository lapRepository;
    @Mock private MetricsProducer producer;
    @Mock private ActivityMetricsMapper activityMapper;
    @Mock private LapMetricsMapper lapMapper;
    @Mock private Clock clock;

    @InjectMocks
    private MetricsService metricsService;

    @Test
    void shouldReturnActivityMetricsWhenFound() {
        // Given
        UUID activityId = UUID.randomUUID();
        ActivityMetrics entity = new ActivityMetrics();
        List<LapMetrics> lapEntities = List.of(new LapMetrics());
        List<LapMetricsDto> lapDtos = List.of(buildTestLapDto());
        ActivityMetricsDto expectedDto = buildTestDto(activityId);

        when(activityRepository.findByActivityId(activityId)).thenReturn(Optional.of(entity));
        when(lapRepository.findByActivityIdOrderByLapNumberAsc(activityId)).thenReturn(lapEntities);
        when(lapMapper.toDtoList(lapEntities)).thenReturn(lapDtos);
        when(activityMapper.toFullDto(entity, lapDtos)).thenReturn(expectedDto);

        // When
        Optional<ActivityMetricsDto> result = metricsService.getActivityMetrics(activityId);

        // Then
        assertThat(result).isPresent().contains(expectedDto);
        verify(activityRepository).findByActivityId(activityId);
        verify(lapRepository).findByActivityIdOrderByLapNumberAsc(activityId);
        verify(lapMapper).toDtoList(lapEntities);
        verify(activityMapper).toFullDto(entity, lapDtos);
    }

    @Test
    void shouldReturnEmptyWhenActivityNotFound() {
        // Given
        UUID activityId = UUID.randomUUID();
        when(activityRepository.findByActivityId(activityId)).thenReturn(Optional.empty());

        // When
        Optional<ActivityMetricsDto> result = metricsService.getActivityMetrics(activityId);

        // Then
        assertThat(result).isEmpty();
        verifyNoInteractions(lapRepository, activityMapper, lapMapper);
    }

    private ActivityMetricsDto buildTestDto(UUID activityId) {
        return new ActivityMetricsDto(
                activityId, "user-1", null,
                null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null,
                null, null, null, null,
                null, null, null, null,
                null, null,
                null, null, null,
                null, null,
                List.of(), null
        );
    }

    private LapMetricsDto buildTestLapDto() {
        return new LapMetricsDto(
                1, "Lap 1", "active",
                null, null, null, null,
                null, null, null, null, null,
                null, null, null,
                null, null,
                null, null, null, null,
                null, null, null,
                null, null
        );
    }
}