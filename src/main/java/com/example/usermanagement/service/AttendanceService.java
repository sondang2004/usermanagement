package com.example.usermanagement.service;

import com.example.usermanagement.dto.request.AttendanceCheckInRequest;
import com.example.usermanagement.dto.request.AttendanceCheckOutRequest;
import com.example.usermanagement.dto.response.AttendanceMonthlySummaryResponse;
import com.example.usermanagement.dto.response.AttendancePenaltyDayResponse;
import com.example.usermanagement.dto.response.AttendanceResponse;
import com.example.usermanagement.dto.response.AttendanceSummaryResponse;
import com.example.usermanagement.entity.Attendance;
import com.example.usermanagement.entity.AttendanceScore;
import com.example.usermanagement.entity.Employee;
import com.example.usermanagement.entity.LeaveRequest;
import com.example.usermanagement.entity.enums.RequestStatus;
import com.example.usermanagement.exception.InvalidRequestException;
import com.example.usermanagement.exception.ResourceNotFoundException;
import com.example.usermanagement.mapper.AttendanceMapper;
import com.example.usermanagement.repository.AttendanceRepository;
import com.example.usermanagement.repository.AttendanceScoreRepository;
import com.example.usermanagement.repository.EmployeeRepository;
import com.example.usermanagement.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Bangkok");
    private static final LocalTime LATE_THRESHOLD = LocalTime.of(8, 30);
    private static final double BASE_SCORE = 100.0;
    private static final double ABSENT_PENALTY = 20.0;
    private static final double LATE_PENALTY = 2.0;
    private static final double EXTRA_LATE_PENALTY = 10.0;
    private static final double UNDERTIME_PENALTY = 3.0;

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceScoreRepository attendanceScoreRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceMapper attendanceMapper;

    @Transactional
    public AttendanceResponse checkIn(AttendanceCheckInRequest request) {
        Employee employee = getEmployee(request.getEmployeeId());
        LocalDateTime checkInAt = request.getCheckInAt() != null ? request.getCheckInAt() : LocalDateTime.now(ZONE_ID);
        LocalDate attendanceDate = checkInAt.toLocalDate();

        if (attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), attendanceDate).isPresent()) {
            throw new InvalidRequestException("Employee already has an attendance record for " + attendanceDate);
        }

        String warning = buildOpenAttendanceWarning(employee.getId(), attendanceDate);

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(attendanceDate);
        attendance.setCheckInTime(checkInAt.toLocalTime());
        attendance.setLate(checkInAt.toLocalTime().isAfter(LATE_THRESHOLD));
        attendance.setLateMinutes(attendance.isLate()
                ? (int) Duration.between(LATE_THRESHOLD, checkInAt.toLocalTime()).toMinutes()
                : 0);
        attendance.setUndertime(false);
        attendance.setOvertime(false);
        attendance.setUndertimeMinutes(0);
        attendance.setOvertimeMinutes(0);
        attendance.setWorkingHours(0.0);

        Attendance saved = attendanceRepository.save(attendance);
        recalculateMonthlyScore(employee.getId(), attendanceDate.getYear(), attendanceDate.getMonthValue());
        return attendanceMapper.toResponse(saved, warning);
    }

    @Transactional
    public AttendanceResponse checkOut(AttendanceCheckOutRequest request) {
        Employee employee = getEmployee(request.getEmployeeId());
        LocalDateTime checkOutAt = request.getCheckOutAt() != null ? request.getCheckOutAt() : LocalDateTime.now(ZONE_ID);

        Attendance attendance = attendanceRepository
                .findTopByEmployeeIdAndCheckOutTimeIsNullOrderByAttendanceDateDescCheckInTimeDesc(employee.getId())
                .orElseThrow(() -> new InvalidRequestException("No open attendance record found for employee " + employee.getId()));

        attendance.setCheckOutTime(checkOutAt.toLocalTime());
        attendance.recalculateWorkingHours();
        Attendance saved = attendanceRepository.save(attendance);

        recalculateMonthlyScore(employee.getId(), attendance.getAttendanceDate().getYear(), attendance.getAttendanceDate().getMonthValue());
        return attendanceMapper.toResponse(saved, null);
    }

    @Transactional
    public AttendanceMonthlySummaryResponse getAttendance(UUID employeeId, Integer month, Integer year, int page, int size) {
        getEmployee(employeeId);

        LocalDate reference = LocalDate.now(ZONE_ID);
        int targetMonth = month != null ? month : reference.getMonthValue();
        int targetYear = year != null ? year : reference.getYear();
        YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        Pageable pageable = PageRequest.of(page, size);
        Page<Attendance> attendancePage = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(employeeId, startDate, endDate, pageable);

        List<AttendanceResponse> responses = attendancePage.getContent().stream()
                .map(attendance -> attendanceMapper.toResponse(attendance, null))
                .toList();

        ScoreSnapshot scoreSnapshot = recalculateMonthlyScore(employeeId, targetYear, targetMonth);

        return attendanceMapper.toMonthlySummary(
                employeeId,
                targetMonth,
                targetYear,
                scoreSnapshot.score(),
                scoreSnapshot.lateCount(),
                scoreSnapshot.absentCount(),
                scoreSnapshot.undertimeCount(),
                scoreSnapshot.overtimeCount(),
                responses,
                attendancePage
        );
    }

    @Transactional
    public AttendanceSummaryResponse getEmployeeMonthlySummary(UUID employeeId, Integer month, Integer year) {
        getEmployee(employeeId);

        LocalDate reference = LocalDate.now(ZONE_ID);
        int targetMonth = month != null ? month : reference.getMonthValue();
        int targetYear = year != null ? year : reference.getYear();
        YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        MonthlyContext context = loadMonthlyContext(employeeId, startDate, endDate);
        List<AttendancePenaltyDayResponse> penaltyDays = buildPenaltyDays(employeeId, targetYear, targetMonth, context);
        ScoreSnapshot scoreSnapshot = recalculateMonthlyScore(employeeId, targetYear, targetMonth);

        return AttendanceSummaryResponse.builder()
                .employeeId(employeeId)
                .month(targetMonth)
                .year(targetYear)
                .workingDays(context.workingDays())
                .presentDays(context.presentDays())
                .lateDays(context.lateDays())
                .leaveDays(context.leaveDays())
                .absentDays(context.absentDays())
                .undertimeDays(context.undertimeDays())
                .overtimeDays(context.overtimeDays())
                .totalWorkingHours(context.totalWorkingHours())
                .attendanceScore(scoreSnapshot.score())
                .penaltyDays(penaltyDays)
                .build();
    }

    @Transactional
    public List<AttendancePenaltyDayResponse> getEmployeePenaltyDays(UUID employeeId, Integer month, Integer year) {
        getEmployee(employeeId);

        LocalDate reference = LocalDate.now(ZONE_ID);
        int targetMonth = month != null ? month : reference.getMonthValue();
        int targetYear = year != null ? year : reference.getYear();
        YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return buildPenaltyDays(employeeId, targetYear, targetMonth, loadMonthlyContext(employeeId, startDate, endDate));
    }

    @Transactional
    public ScoreSnapshot recalculateMonthlyScore(UUID employeeId, int year, int month) {
        Employee employee = getEmployee(employeeId);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        MonthlyContext context = loadMonthlyContext(employeeId, startDate, endDate);

        double score = BASE_SCORE;
        long lateCount = 0;
        long absentCount = 0;
        long undertimeCount = 0;
        long overtimeCount = 0;

        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            if (isWeekend(cursor) || context.approvedLeaveDates().contains(cursor)) {
                cursor = cursor.plusDays(1);
                continue;
            }

            Attendance attendance = context.attendanceMap().get(cursor);
            if (attendance == null) {
                absentCount++;
                score -= ABSENT_PENALTY;
                cursor = cursor.plusDays(1);
                continue;
            }

            if (attendance.isLate()) {
                lateCount++;
                score -= LATE_PENALTY;
            }
            if (attendance.isUndertime()) {
                undertimeCount++;
                score -= UNDERTIME_PENALTY;
            }
            if (attendance.isOvertime()) {
                overtimeCount++;
            }

            cursor = cursor.plusDays(1);
        }

        if (lateCount > 3) {
            score -= EXTRA_LATE_PENALTY;
        }

        score = Math.max(0.0, score);

        AttendanceScore attendanceScore = attendanceScoreRepository
                .findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElseGet(AttendanceScore::new);
        attendanceScore.setEmployee(employee);
        attendanceScore.setMonth(month);
        attendanceScore.setYear(year);
        attendanceScore.setScore(score);
        attendanceScoreRepository.save(attendanceScore);

        return new ScoreSnapshot(score, lateCount, absentCount, undertimeCount, overtimeCount);
    }

    @Transactional
    public void recalculateScoresForRange(UUID employeeId, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidRequestException("End date must not be before start date");
        }

        YearMonth cursor = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);
        while (!cursor.isAfter(endMonth)) {
            recalculateMonthlyScore(employeeId, cursor.getYear(), cursor.getMonthValue());
            cursor = cursor.plusMonths(1);
        }
    }

    private AttendancePenaltyDayResponse toAbsentPenaltyDay(LocalDate date) {
        return attendanceMapper.toPenaltyDay(
                date,
                "ABSENT",
                "Absent without approved leave",
                null,
                null,
                0.0,
                (int) ABSENT_PENALTY,
                0,
                0,
                false
        );
    }

    private AttendancePenaltyDayResponse toApprovedLeavePenaltyDay(LocalDate date) {
        return attendanceMapper.toPenaltyDay(
                date,
                "LEAVE",
                "Approved leave",
                null,
                null,
                0.0,
                0,
                0,
                0,
                true
        );
    }

    private AttendancePenaltyDayResponse toPenaltyDay(Attendance attendance, LocalDate date) {
        String type;
        String label;
        int penaltyPoints;

        if (attendance.isLate() && attendance.isUndertime()) {
            type = "LATE+UNDERTIME";
            label = "Late and undertime";
            penaltyPoints = (int) (LATE_PENALTY + UNDERTIME_PENALTY);
        } else if (attendance.isLate()) {
            type = "LATE";
            label = "Late check-in";
            penaltyPoints = (int) LATE_PENALTY;
        } else if (attendance.isUndertime()) {
            type = "UNDERTIME";
            label = "Undertime";
            penaltyPoints = (int) UNDERTIME_PENALTY;
        } else {
            type = "OVERTIME";
            label = "Overtime";
            penaltyPoints = 0;
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

        return attendanceMapper.toPenaltyDay(
                date,
                type,
                label,
                checkInAt,
                checkOutAt,
                attendance.getWorkingHours(),
                penaltyPoints,
                attendance.getLateMinutes(),
                attendance.getUndertimeMinutes(),
                false
        );
    }

    private List<AttendancePenaltyDayResponse> buildPenaltyDays(UUID employeeId, int year, int month, MonthlyContext context) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<AttendancePenaltyDayResponse> result = new java.util.ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            if (isWeekend(cursor)) {
                cursor = cursor.plusDays(1);
                continue;
            }

            if (context.approvedLeaveDates().contains(cursor)) {
                result.add(toApprovedLeavePenaltyDay(cursor));
                cursor = cursor.plusDays(1);
                continue;
            }

            Attendance attendance = context.attendanceMap().get(cursor);
            if (attendance == null) {
                result.add(toAbsentPenaltyDay(cursor));
                cursor = cursor.plusDays(1);
                continue;
            }

            if (attendance.isLate() || attendance.isUndertime() || attendance.isOvertime()) {
                result.add(toPenaltyDay(attendance, cursor));
            }

            cursor = cursor.plusDays(1);
        }

        return result;
    }

    private MonthlyContext loadMonthlyContext(UUID employeeId, LocalDate startDate, LocalDate endDate) {
        List<Attendance> attendances = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(employeeId, startDate, endDate);

        Map<LocalDate, Attendance> attendanceMap = new HashMap<>();
        for (Attendance attendance : attendances) {
            attendanceMap.put(attendance.getAttendanceDate(), attendance);
        }

        Set<LocalDate> approvedLeaveDates = new HashSet<>();
        for (LeaveRequest leaveRequest : leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, RequestStatus.APPROVED)) {
            LocalDate leaveStart = leaveRequest.getStartDate().isBefore(startDate) ? startDate : leaveRequest.getStartDate();
            LocalDate leaveEnd = leaveRequest.getEndDate().isAfter(endDate) ? endDate : leaveRequest.getEndDate();
            if (leaveStart.isAfter(leaveEnd)) {
                continue;
            }
            LocalDate cursor = leaveStart;
            while (!cursor.isAfter(leaveEnd)) {
                approvedLeaveDates.add(cursor);
                cursor = cursor.plusDays(1);
            }
        }

        long workingDays = 0;
        long presentDays = 0;
        long lateDays = 0;
        long leaveDays = 0;
        long absentDays = 0;
        long undertimeDays = 0;
        long overtimeDays = 0;
        double totalWorkingHours = 0.0;

        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            if (isWeekend(cursor)) {
                cursor = cursor.plusDays(1);
                continue;
            }

            workingDays++;

            if (approvedLeaveDates.contains(cursor)) {
                leaveDays++;
                cursor = cursor.plusDays(1);
                continue;
            }

            Attendance attendance = attendanceMap.get(cursor);
            if (attendance == null) {
                absentDays++;
                cursor = cursor.plusDays(1);
                continue;
            }

            presentDays++;
            totalWorkingHours += attendance.getWorkingHours();
            if (attendance.isLate()) {
                lateDays++;
            }
            if (attendance.isUndertime()) {
                undertimeDays++;
            }
            if (attendance.isOvertime()) {
                overtimeDays++;
            }

            cursor = cursor.plusDays(1);
        }

        return new MonthlyContext(attendanceMap, approvedLeaveDates, workingDays, presentDays, lateDays, leaveDays, absentDays, undertimeDays, overtimeDays, totalWorkingHours);
    }

    private Employee getEmployee(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
    }

    private String buildOpenAttendanceWarning(UUID employeeId, LocalDate attendanceDate) {
        Attendance openAttendance = attendanceRepository
                .findTopByEmployeeIdAndCheckOutTimeIsNullOrderByAttendanceDateDescCheckInTimeDesc(employeeId)
                .orElse(null);
        if (openAttendance != null && openAttendance.getAttendanceDate().isBefore(attendanceDate)) {
            return "Previous attendance has no check-out";
        }
        return null;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    public record ScoreSnapshot(double score, long lateCount, long absentCount, long undertimeCount, long overtimeCount) {
    }

    private record MonthlyContext(
            Map<LocalDate, Attendance> attendanceMap,
            Set<LocalDate> approvedLeaveDates,
            long workingDays,
            long presentDays,
            long lateDays,
            long leaveDays,
            long absentDays,
            long undertimeDays,
            long overtimeDays,
            double totalWorkingHours) {
    }
}
