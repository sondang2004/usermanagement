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
public class AttendanceMonthlySummaryResponse {

    private UUID employeeId;
    private int month;
    private int year;
    private double score;
    private long lateCount;
    private long absentCount;
    private long undertimeCount;
    private long overtimeCount;
    private List<AttendanceResponse> attendances;
    private int pageNo;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
