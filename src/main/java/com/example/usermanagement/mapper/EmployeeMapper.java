package com.example.usermanagement.mapper;

import com.example.usermanagement.dto.request.EmployeeRequest;
import com.example.usermanagement.dto.response.EmployeeResponse;
import com.example.usermanagement.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public Employee toEntity(EmployeeRequest request) {
        if (request == null) {
            return null;
        }

        return Employee.builder()
                .name(request.getName())
                .email(request.getEmail())
                .position(request.getPosition())
                .department(request.getDepartment())
                .build();
    }

    public EmployeeResponse toResponse(Employee employee) {
        if (employee == null) {
            return null;
        }

        return EmployeeResponse.builder()
                .id(employee.getId())
                .name(employee.getName())
                .email(employee.getEmail())
                .position(employee.getPosition())
                .department(employee.getDepartment())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }

    public void updateEntity(Employee employee, EmployeeRequest request) {
        if (employee == null || request == null) {
            return;
        }

        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setPosition(request.getPosition());
        employee.setDepartment(request.getDepartment());
    }
}
