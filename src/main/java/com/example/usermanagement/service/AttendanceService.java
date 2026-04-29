package com.example.usermanagement.service;

import com.example.usermanagement.dto.AttendanceDTO;
import com.example.usermanagement.dto.AttendancePenaltyDayDTO;
import com.example.usermanagement.dto.AttendanceSummaryDTO;
import com.example.usermanagement.entity.Attendance;
import com.example.usermanagement.entity.AttendanceScore;
import com.example.usermanagement.entity.Employee;
import com.example.usermanagement.entity.LeaveRequest;
import com.example.usermanagement.entity.enums.RequestStatus;
import com.example.usermanagement.exception.InvalidRequestException;
import com.example.usermanagement.exception.ResourceNotFoundException;
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

    @Transactional
    public AttendanceDTO.Response checkIn(AttendanceDTO.CheckInRequest request) {
        Employee employee = getEmployee(request.getEmployeeId());
        LocalDateTime checkInAt = request.getCheckInAt() != null ? request.getCheckInAt() : LocalDateTime.now(ZONE_ID);
        LocalDate attendanceDate = checkInAt.toLocalDate();

        Attendance existingToday = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), attendanceDate)
                .orElse(null);
        if (existingToday != null) {
            throw new InvalidRequestException("Employee already has an attendance record for " + attendanceDate);
        }

        String warning = null;
        Attendance openAttendance = attendanceRepository
                .findTopByEmployeeIdAndCheckOutTimeIsNullOrderByAttendanceDateDescCheckInTimeDesc(employee.getId())
                .orElse(null);
        if (openAttendance != null && openAttendance.getAttendanceDate().isBefore(attendanceDate)) {
            warning = "Previous attendance has no check-out";
        }

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(attendanceDate);
        attendance.setCheckInTime(checkInAt.toLocalTime());
        attendance.setLate(checkInAt.toLocalTime().isAfter(LATE_THRESHOLD));
        attendance.setLateMinutes(attendance.isLate() ? (int) java.time.Duration.between(LATE_THRESHOLD, checkInAt.toLocalTime()).toMinutes() : 0);
        attendance.setUndertime(false);
        attendance.setOvertime(false);
        attendance.setUndertimeMinutes(0);
        attendance.setOvertimeMinutes(0);
        attendance.setWorkingHours(0.0);

        attendanceRepository.save(attendance);
        recalculateMonthlyScore(employee.getId(), attendanceDate.getYear(), attendanceDate.getMonthValue());

        return toResponse(attendance, warning);
    }

    @Transactional
    public AttendanceDTO.Response checkOut(AttendanceDTO.CheckOutRequest request) {
        Employee employee = getEmployee(request.getEmployeeId());
        LocalDateTime checkOutAt = request.getCheckOutAt() != null ? request.getCheckOutAt() : LocalDateTime.now(ZONE_ID);

        Attendance attendance = attendanceRepository.findTopByEmployeeIdAndCheckOutTimeIsNullOrderByAttendanceDateDescCheckInTimeDesc(employee.getId())
                .orElseThrow(() -> new InvalidRequestException("No open attendance record found for employee " + employee.getId()));

        attendance.setCheckOutTime(checkOutAt.toLocalTime());
        attendance.recalculateWorkingHours();
        attendanceRepository.save(attendance);

        recalculateMonthlyScore(employee.getId(), attendance.getAttendanceDate().getYear(), attendance.getAttendanceDate().getMonthValue());

        return toResponse(attendance, null);
    }
    @Transactional
    public AttendanceDTO.MonthlySummary getAttendance(UUID employeeId, Integer month, Integer year, int page, int size) {
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
        List<AttendanceDTO.Response> responses = attendancePage.getContent().stream()
                .map(attendance -> toResponse(attendance, null))
                .toList();

        ScoreSnapshot scoreSnapshot = recalculateMonthlyScore(employeeId, targetYear, targetMonth);

        return AttendanceDTO.MonthlySummary.builder()
                .employeeId(employeeId)
                .month(targetMonth)
                .year(targetYear)
                .score(scoreSnapshot.score())
                .lateCount(scoreSnapshot.lateCount())
                .absentCount(scoreSnapshot.absentCount())
                .undertimeCount(scoreSnapshot.undertimeCount())
                .overtimeCount(scoreSnapshot.overtimeCount())
                .attendances(responses)
                .pageNo(attendancePage.getNumber())
                .pageSize(attendancePage.getSize())
                .totalElements(attendancePage.getTotalElements())
                .totalPages(attendancePage.getTotalPages())
                .last(attendancePage.isLast())
                .build();
    }

    @Transactional
    public AttendanceSummaryDTO.Response getEmployeeMonthlySummary(UUID employeeId, Integer month, Integer year) {
        getEmployee(employeeId);
        LocalDate reference = LocalDate.now(ZONE_ID);
        int targetMonth = month != null ? month : reference.getMonthValue();
        int targetYear = year != null ? year : reference.getYear();
        YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

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
            DayOfWeek dayOfWeek = cursor.getDayOfWeek();
            boolean weekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
            if (weekend) {
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

        List<AttendancePenaltyDayDTO.Response> penaltyDays = buildPenaltyDays(employeeId, targetYear, targetMonth);
        ScoreSnapshot scoreSnapshot = recalculateMonthlyScore(employeeId, targetYear, targetMonth);

        return AttendanceSummaryDTO.Response.builder()
                .employeeId(employeeId)
                .month(targetMonth)
                .year(targetYear)
                .workingDays(workingDays)
                .presentDays(presentDays)
                .lateDays(lateDays)
                .leaveDays(leaveDays)
                .absentDays(absentDays)
                .undertimeDays(undertimeDays)
                .overtimeDays(overtimeDays)
                .totalWorkingHours(totalWorkingHours)
                .attendanceScore(scoreSnapshot.score())
                .penaltyDays(penaltyDays)
                .build();
    }

    @Transactional
    public List<AttendancePenaltyDayDTO.Response> getEmployeePenaltyDays(UUID employeeId, Integer month, Integer year) {
        getEmployee(employeeId);
        LocalDate reference = LocalDate.now(ZONE_ID);
        int targetMonth = month != null ? month : reference.getMonthValue();
        int targetYear = year != null ? year : reference.getYear();
        return buildPenaltyDays(employeeId, targetYear, targetMonth);
    }

    @Transactional
    public ScoreSnapshot recalculateMonthlyScore(UUID employeeId, int year, int month) {
        Employee employee = getEmployee(employeeId);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

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

        double score = BASE_SCORE;
        long lateCount = 0;
        long absentCount = 0;
        long undertimeCount = 0;
        long overtimeCount = 0;

        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            DayOfWeek dayOfWeek = cursor.getDayOfWeek();
            boolean weekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
            if (weekend || approvedLeaveDates.contains(cursor)) {
                cursor = cursor.plusDays(1);
                continue;
            }

            Attendance attendance = attendanceMap.get(cursor);
            if (attendance == null) {
                absentCount++;
                score -= ABSENT_PENALTY;
                cursor = cursor.plusDays(1);
                continue;
            }

            if (attendance.isLate()) {
                lateCount++;
                score -= LATE_PENALTY;
                if (lateCount > 3) {
                    score -= EXTRA_LATE_PENALTY;
                }
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

    private AttendanceDTO.Response toResponse(Attendance attendance, String warning) {
        LocalDateTime checkInAt = attendance.getCheckInTime() == null ? null : attendance.getAttendanceDate().atTime(attendance.getCheckInTime());
        LocalDateTime checkOutAt = null;
        if (attendance.getCheckOutTime() != null) {
            checkOutAt = attendance.getAttendanceDate().atTime(attendance.getCheckOutTime());
            if (attendance.getCheckInTime() != null && attendance.getCheckOutTime().isBefore(attendance.getCheckInTime())) {
                checkOutAt = checkOutAt.plusDays(1);
            }
        }

        return AttendanceDTO.Response.builder()
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

    private Employee getEmployee(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
    }

    private List<AttendancePenaltyDayDTO.Response> buildPenaltyDays(UUID employeeId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

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

        List<AttendancePenaltyDayDTO.Response> result = new java.util.ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            DayOfWeek dayOfWeek = cursor.getDayOfWeek();
            boolean weekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
            if (weekend) {
                cursor = cursor.plusDays(1);
                continue;
            }

            Attendance attendance = attendanceMap.get(cursor);
            if (approvedLeaveDates.contains(cursor)) {
                result.add(AttendancePenaltyDayDTO.Response.builder()
                        .date(cursor)
                        .type("LEAVE")
                        .label("Approved leave")
                        .workingHours(0.0)
                        .penaltyPoints(0)
                        .approvedLeave(true)
                        .build());
                cursor = cursor.plusDays(1);
                continue;
            }

            if (attendance == null) {
                result.add(AttendancePenaltyDayDTO.Response.builder()
                        .date(cursor)
                        .type("ABSENT")
                        .label("Absent without approved leave")
                        .workingHours(0.0)
                        .penaltyPoints((int) ABSENT_PENALTY)
                        .approvedLeave(false)
                        .build());
                cursor = cursor.plusDays(1);
                continue;
            }

            boolean penaltyDay = attendance.isLate() || attendance.isUndertime() || attendance.isOvertime();
            if (penaltyDay) {
                String type;
                String label;
                int penaltyPoints = 0;
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

                result.add(AttendancePenaltyDayDTO.Response.builder()
                        .date(cursor)
                        .type(type)
                        .label(label)
                        .checkInAt(attendance.getCheckInTime() == null ? null : attendance.getAttendanceDate().atTime(attendance.getCheckInTime()))
                        .checkOutAt(attendance.getCheckOutTime() == null ? null : attendance.getAttendanceDate().atTime(attendance.getCheckOutTime()))
                        .workingHours(attendance.getWorkingHours())
                        .penaltyPoints(penaltyPoints)
                        .lateMinutes(attendance.getLateMinutes())
                        .undertimeMinutes(attendance.getUndertimeMinutes())
                        .approvedLeave(false)
                        .build());
            }

            cursor = cursor.plusDays(1);
        }

        return result;
    }

    public record ScoreSnapshot(double score, long lateCount, long absentCount, long undertimeCount, long overtimeCount) {
    }
}
