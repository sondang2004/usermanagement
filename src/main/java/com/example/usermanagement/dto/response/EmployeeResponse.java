package com.example.usermanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {

    private UUID id;
    private String name;
    private String email;
    private String position;
    private String department;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double currentMonthAttendanceScore;
    private Integer currentYearLeaveBalance;
}
