package com.example.usermanagement.mapper;

import com.example.usermanagement.dto.LeaveRequestDTO;
import com.example.usermanagement.entity.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {EmployeeMapper.class})
public interface LeaveRequestMapper {
    @Mapping(target = "employee", ignore = true)
    LeaveRequest toEntity(LeaveRequestDTO.Request request);

    LeaveRequestDTO.Response toResponse(LeaveRequest leaveRequest);
}
