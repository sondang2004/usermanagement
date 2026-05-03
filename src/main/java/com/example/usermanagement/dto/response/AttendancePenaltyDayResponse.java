package com.example.usermanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendancePenaltyDayResponse {

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
