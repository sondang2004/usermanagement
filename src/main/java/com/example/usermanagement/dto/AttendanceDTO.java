package com.example.usermanagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AttendanceDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckInRequest {
        @NotNull(message = "Employee ID is required")
        private UUID employeeId;
        private LocalDateTime checkInAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckOutRequest {
        @NotNull(message = "Employee ID is required")
        private UUID employeeId;
        private LocalDateTime checkOutAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID employeeId;
        private LocalDate attendanceDate;
        private LocalDateTime checkInAt;
        private LocalDateTime checkOutAt;
        private boolean late;
        private int lateMinutes;
        private boolean undertime;
        private int undertimeMinutes;
        private boolean overtime;
        private int overtimeMinutes;
        private double workingHours;
        private String warning;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlySummary {
        private UUID employeeId;
        private int month;
        private int year;
        private double score;
        private long lateCount;
        private long absentCount;
        private long undertimeCount;
        private long overtimeCount;
        private List<Response> attendances;
        private int pageNo;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }
}
