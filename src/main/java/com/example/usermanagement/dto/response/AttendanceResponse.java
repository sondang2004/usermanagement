package com.example.usermanagement.dto.response;

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
public class AttendanceResponse {

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
