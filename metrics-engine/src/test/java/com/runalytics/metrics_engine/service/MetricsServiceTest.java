package com.runalytics.metrics_engine.service;

import com.runalytics.metrics_engine.dto.ActivityMetricsDto;
import com.runalytics.metrics_engine.dto.ActivitySampleDto;
import com.runalytics.metrics_engine.dto.LapMetricsDto;
import com.runalytics.metrics_engine.entity.ActivityMetrics;
import com.runalytics.metrics_engine.entity.ActivitySample;
import com.runalytics.metrics_engine.entity.LapMetrics;
import com.runalytics.metrics_engine.kafka.MetricsProducer;
import com.runalytics.metrics_engine.mapper.ActivityMetricsMapper;
import com.runalytics.metrics_engine.mapper.LapMetricsMapper;
import com.runalytics.metrics_engine.repository.ActivityMetricsRepository;
import com.runalytics.metrics_engine.repository.ActivitySampleRepository;
import com.runalytics.metrics_engine.repository.LapMetricsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock private ActivityMetricsCalculator activityCalculator;
    @Mock private ActivityMetricsRepository activityRepository;
    @Mock private LapMetricsRepository lapRepository;
    @Mock private ActivitySampleRepository sampleRepository;
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

    @Test
    void shouldReturnLatestActivityMetricsForUser() {
        // Given
        String userId = "demo";
        UUID activityId = UUID.randomUUID();
        ActivityMetrics entity = new ActivityMetrics();
        entity.setActivityId(activityId);
        List<LapMetrics> lapEntities = List.of(new LapMetrics());
        List<LapMetricsDto> lapDtos = List.of(buildTestLapDto());
        ActivityMetricsDto expectedDto = buildTestDto(activityId);

        when(activityRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(entity));
        when(lapRepository.findByActivityIdOrderByLapNumberAsc(activityId)).thenReturn(lapEntities);
        when(lapMapper.toDtoList(lapEntities)).thenReturn(lapDtos);
        when(activityMapper.toFullDto(entity, lapDtos)).thenReturn(expectedDto);

        // When
        Optional<ActivityMetricsDto> result = metricsService.getLatestActivityMetrics(userId);

        // Then
        assertThat(result).isPresent().contains(expectedDto);
        verify(activityRepository).findFirstByUserIdOrderByCreatedAtDesc(userId);
        verify(lapRepository).findByActivityIdOrderByLapNumberAsc(activityId);
    }

    @Test
    void shouldReturnEmptyLatestWhenNoActivityForUser() {
        // Given
        String userId = "demo";
        when(activityRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.empty());

        // When
        Optional<ActivityMetricsDto> result = metricsService.getLatestActivityMetrics(userId);

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

    @Test
    void shouldReturnSamplesForActivity() {
        // Given
        UUID activityId = UUID.randomUUID();
        List<ActivitySample> entities = List.of(buildSampleEntity(activityId), buildSampleEntity(activityId));
        when(sampleRepository.findByActivityIdOrderByTimestampAsc(activityId)).thenReturn(entities);

        // When
        List<ActivitySampleDto> result = metricsService.getSamples(activityId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).heartRate()).isEqualTo(145);
        assertThat(result.get(0).latitude()).isEqualTo(40.4);
        verify(sampleRepository).findByActivityIdOrderByTimestampAsc(activityId);
    }

    @Test
    void shouldReturnEmptySamplesWhenNoneExist() {
        // Given
        UUID activityId = UUID.randomUUID();
        when(sampleRepository.findByActivityIdOrderByTimestampAsc(activityId)).thenReturn(List.of());

        // When
        List<ActivitySampleDto> result = metricsService.getSamples(activityId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldDownsampleToMaxWhenTooManySamples() {
        // Given
        UUID activityId = UUID.randomUUID();
        List<ActivitySample> bigList = IntStream.range(0, 1000)
                .mapToObj(i -> buildSampleEntity(activityId))
                .collect(java.util.stream.Collectors.toList());
        when(sampleRepository.findByActivityIdOrderByTimestampAsc(activityId)).thenReturn(bigList);

        // When
        List<ActivitySampleDto> result = metricsService.getSamples(activityId);

        // Then
        assertThat(result).hasSizeLessThanOrEqualTo(500);
    }

    private ActivitySample buildSampleEntity(UUID activityId) {
        ActivitySample s = new ActivitySample();
        s.setActivityId(activityId);
        s.setTimestamp(Instant.parse("2025-01-01T10:00:00Z"));
        s.setLatitude(40.4);
        s.setLongitude(-3.7);
        s.setHeartRate(145);
        s.setCadence(170);
        s.setAltitude(650.0);
        s.setSpeed(3.0);
        s.setPower(null);
        s.setDistance(1000.0);
        return s;
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