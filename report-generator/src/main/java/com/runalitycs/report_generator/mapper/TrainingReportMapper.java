package com.runalitycs.report_generator.mapper;

import com.runalitycs.report_generator.dto.TrainingReportDto;
import com.runalitycs.report_generator.entity.TrainingReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TrainingReportMapper {
    /*
     @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AthleteProfile toEntity(AthleteProfileDto dto);

    AthleteProfileDto toDto(AthleteProfile entity);
     */
    @Mapping(target = "createdAt", ignore = true)
    TrainingReport toEntity(TrainingReportDto dto);

    TrainingReportDto toDto(TrainingReport entity);
}
