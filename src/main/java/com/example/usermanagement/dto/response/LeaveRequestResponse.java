package com.example.usermanagement.dto.response;

import com.example.usermanagement.entity.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponse {

    private UUID id;
    private EmployeeResponse employee;
    private String reason;
    private LocalDate startDate;
    private LocalDate endDate;
    private int requestedDays;
    private RequestStatus status;
    private LocalDate approvedAt;
    private LocalDate rejectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
