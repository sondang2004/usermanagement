package com.example.usermanagement.mapper;

import com.example.usermanagement.dto.request.LeaveRequestRequest;
import com.example.usermanagement.dto.response.EmployeeResponse;
import com.example.usermanagement.dto.response.LeaveRequestResponse;
import com.example.usermanagement.entity.Employee;
import com.example.usermanagement.entity.LeaveRequest;
import org.springframework.stereotype.Component;

@Component
public class LeaveRequestMapper {

    private final EmployeeMapper employeeMapper;

    public LeaveRequestMapper(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    public LeaveRequest toEntity(LeaveRequestRequest request, Employee employee) {
        if (request == null) {
            return null;
        }

        return LeaveRequest.builder()
                .employee(employee)
                .reason(request.getReason().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
    }

    public LeaveRequestResponse toResponse(LeaveRequest leaveRequest) {
        if (leaveRequest == null) {
            return null;
        }

        EmployeeResponse employeeResponse = employeeMapper.toResponse(leaveRequest.getEmployee());
        return LeaveRequestResponse.builder()
                .id(leaveRequest.getId())
                .employee(employeeResponse)
                .reason(leaveRequest.getReason())
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .requestedDays(leaveRequest.getRequestedDays())
                .status(leaveRequest.getStatus())
                .approvedAt(leaveRequest.getApprovedAt())
                .rejectedAt(leaveRequest.getRejectedAt())
                .createdAt(leaveRequest.getCreatedAt())
                .updatedAt(leaveRequest.getUpdatedAt())
                .build();
    }
}
