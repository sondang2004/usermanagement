package com.example.usermanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {

    private UUID employeeId;
    private int month;
    private int year;
    private long workingDays;
    private long presentDays;
    private long lateDays;
    private long leaveDays;
    private long absentDays;
    private long undertimeDays;
    private long overtimeDays;
    private double totalWorkingHours;
    private double attendanceScore;
    private List<AttendancePenaltyDayResponse> penaltyDays;
}
