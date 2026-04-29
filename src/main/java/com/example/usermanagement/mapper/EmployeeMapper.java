package com.example.usermanagement.mapper;

import com.example.usermanagement.dto.EmployeeDTO;
import com.example.usermanagement.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public abstract class EmployeeMapper {

    public abstract Employee toEntity(EmployeeDTO.Request request);
    
    public abstract EmployeeDTO.Response toResponse(Employee employee);
    
    public abstract void updateEntity(@MappingTarget Employee employee, EmployeeDTO.Request request);
}
