package com.example.usermanagement.mapper;

import com.example.usermanagement.dto.LateRequestDTO;
import com.example.usermanagement.entity.LateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {EmployeeMapper.class})
public interface LateRequestMapper {
    @Mapping(target = "employee", ignore = true)
    LateRequest toEntity(LateRequestDTO.Request request);

    LateRequestDTO.Response toResponse(LateRequest lateRequest);
}
