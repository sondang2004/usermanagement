package com.example.usermanagement.mapper;

import com.example.usermanagement.dto.response.AttendanceMonthlySummaryResponse;
import com.example.usermanagement.dto.response.AttendancePenaltyDayResponse;
import com.example.usermanagement.dto.response.AttendanceResponse;
import com.example.usermanagement.entity.Attendance;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class AttendanceMapper {

    public AttendanceResponse toResponse(Attendance attendance, String warning) {
        if (attendance == null) {
            return null;
        }

        LocalDateTime checkInAt = attendance.getCheckInTime() == null
                ? null
                : attendance.getAttendanceDate().atTime(attendance.getCheckInTime());

        LocalDateTime checkOutAt = null;
        if (attendance.getCheckOutTime() != null) {
            checkOutAt = attendance.getAttendanceDate().atTime(attendance.getCheckOutTime());
            if (attendance.getCheckInTime() != null && attendance.getCheckOutTime().isBefore(attendance.getCheckInTime())) {
                checkOutAt = checkOutAt.plusDays(1);
            }
        }

        return AttendanceResponse.builder()
                .id(attendance.getId())
                .employeeId(attendance.getEmployee() != null ? attendance.getEmployee().getId() : null)
                .attendanceDate(attendance.getAttendanceDate())
                .checkInAt(checkInAt)
                .checkOutAt(checkOutAt)
                .late(attendance.isLate())
                .lateMinutes(attendance.getLateMinutes())
                .undertime(attendance.isUndertime())
                .undertimeMinutes(attendance.getUndertimeMinutes())
                .overtime(attendance.isOvertime())
                .overtimeMinutes(attendance.getOvertimeMinutes())
                .workingHours(attendance.getWorkingHours())
                .warning(warning)
                .build();
    }

    public AttendancePenaltyDayResponse toPenaltyDay(
            LocalDate date,
            String type,
            String label,
            LocalDateTime checkInAt,
            LocalDateTime checkOutAt,
            double workingHours,
            int penaltyPoints,
            int lateMinutes,
            int undertimeMinutes,
            boolean approvedLeave) {
        return AttendancePenaltyDayResponse.builder()
                .date(date)
                .type(type)
                .label(label)
                .checkInAt(checkInAt)
                .checkOutAt(checkOutAt)
                .workingHours(workingHours)
                .penaltyPoints(penaltyPoints)
                .lateMinutes(lateMinutes)
                .undertimeMinutes(undertimeMinutes)
                .approvedLeave(approvedLeave)
                .build();
    }

    public AttendanceMonthlySummaryResponse toMonthlySummary(
            java.util.UUID employeeId,
            int month,
            int year,
            double score,
            long lateCount,
            long absentCount,
            long undertimeCount,
            long overtimeCount,
            java.util.List<AttendanceResponse> attendances,
            org.springframework.data.domain.Page<Attendance> attendancePage) {
        return AttendanceMonthlySummaryResponse.builder()
                .employeeId(employeeId)
                .month(month)
                .year(year)
                .score(score)
                .lateCount(lateCount)
                .absentCount(absentCount)
                .undertimeCount(undertimeCount)
                .overtimeCount(overtimeCount)
                .attendances(attendances)
                .pageNo(attendancePage.getNumber())
                .pageSize(attendancePage.getSize())
                .totalElements(attendancePage.getTotalElements())
                .totalPages(attendancePage.getTotalPages())
                .last(attendancePage.isLast())
                .build();
    }
}
