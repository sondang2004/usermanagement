package com.example.usermanagement.dto;

import com.example.usermanagement.entity.enums.RequestStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class LeaveRequestDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotNull(message = "Employee ID is required")
        private UUID employeeId;

        @NotBlank(message = "Reason is required")
        private String reason;

        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        private LocalDate endDate;

        @AssertTrue(message = "End date must be on or after start date")
        public boolean isDateRangeValid() {
            return startDate != null && endDate != null && !endDate.isBefore(startDate);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private EmployeeDTO.Response employee;
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
}
