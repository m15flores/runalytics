package com.runalitycs.report_generator.mapper;

import com.runalitycs.report_generator.dto.AthleteProfileDto;
import com.runalitycs.report_generator.entity.AthleteProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AthleteProfileMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AthleteProfile toEntity(AthleteProfileDto dto);

    AthleteProfileDto toDto(AthleteProfile entity);
}