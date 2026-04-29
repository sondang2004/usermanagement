package com.example.usermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendancePenaltyDayDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private LocalDate date;
        private String type;
        private String label;
        private LocalDateTime checkInAt;
        private LocalDateTime checkOutAt;
        private double workingHours;
        private int penaltyPoints;
        private int lateMinutes;
        private int undertimeMinutes;
        private boolean approvedLeave;
    }
}
