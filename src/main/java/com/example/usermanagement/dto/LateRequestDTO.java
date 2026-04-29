package com.example.usermanagement.dto;

import com.example.usermanagement.entity.enums.RequestStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class LateRequestDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotNull(message = "Employee ID is required")
        private UUID employeeId;

        @NotBlank(message = "Reason is required")
        private String reason;

        @NotNull(message = "Date is required")
        private LocalDate date;

        @NotNull(message = "Minutes late is required")
        @Min(value = 1, message = "Minutes late must be at least 1")
        private Integer minutesLate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private EmployeeDTO.Response employee;
        private String reason;
        private LocalDate date;
        private Integer minutesLate;
        private RequestStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
