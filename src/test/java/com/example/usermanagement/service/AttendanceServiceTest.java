package com.example.usermanagement.service;

import com.example.usermanagement.dto.AttendanceDTO;
import com.example.usermanagement.entity.Attendance;
import com.example.usermanagement.entity.AttendanceScore;
import com.example.usermanagement.entity.Employee;
import com.example.usermanagement.entity.LeaveRequest;
import com.example.usermanagement.entity.enums.RequestStatus;
import com.example.usermanagement.repository.AttendanceRepository;
import com.example.usermanagement.repository.AttendanceScoreRepository;
import com.example.usermanagement.repository.EmployeeRepository;
import com.example.usermanagement.repository.LeaveRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AttendanceScoreRepository attendanceScoreRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    void checkIn_marksLateAndReturnsWarningWhenPreviousDayOpen() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = Employee.builder().id(employeeId).name("Alice").email("alice@example.com").build();
        Attendance openAttendance = Attendance.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .attendanceDate(LocalDate.of(2026, 4, 27))
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(employeeId), eq(LocalDate.of(2026, 4, 28))))
                .thenReturn(Optional.empty());
        when(attendanceRepository.findTopByEmployeeIdAndCheckOutTimeIsNullOrderByAttendanceDateDescCheckInTimeDesc(employeeId))
                .thenReturn(Optional.of(openAttendance));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(any(), any(), any()))
                .thenReturn(List.of());
        when(attendanceScoreRepository.findByEmployeeIdAndMonthAndYear(any(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(attendanceScoreRepository.save(any(AttendanceScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveRequestRepository.findByEmployeeIdAndStatus(any(), any())).thenReturn(List.of());

        AttendanceDTO.Response response = attendanceService.checkIn(
                AttendanceDTO.CheckInRequest.builder()
                        .employeeId(employeeId)
                        .checkInAt(LocalDateTime.of(2026, 4, 28, 8, 45))
                        .build()
        );

        assertThat(response.isLate()).isTrue();
        assertThat(response.getLateMinutes()).isEqualTo(15);
        assertThat(response.getWarning()).isEqualTo("Previous attendance has no check-out");
    }

    @Test
    void checkOut_calculatesUndertime() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = Employee.builder().id(employeeId).name("Alice").email("alice@example.com").build();
        Attendance openAttendance = Attendance.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .attendanceDate(LocalDate.of(2026, 4, 28))
                .checkInTime(LocalTime.of(8, 0))
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findTopByEmployeeIdAndCheckOutTimeIsNullOrderByAttendanceDateDescCheckInTimeDesc(employeeId))
                .thenReturn(Optional.of(openAttendance));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(any(), any(), any()))
                .thenReturn(List.of(openAttendance));
        when(attendanceScoreRepository.findByEmployeeIdAndMonthAndYear(any(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(attendanceScoreRepository.save(any(AttendanceScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveRequestRepository.findByEmployeeIdAndStatus(any(), any())).thenReturn(List.of());

        AttendanceDTO.Response response = attendanceService.checkOut(
                AttendanceDTO.CheckOutRequest.builder()
                        .employeeId(employeeId)
                        .checkOutAt(LocalDateTime.of(2026, 4, 28, 14, 0))
                        .build()
        );

        assertThat(response.isUndertime()).isTrue();
        assertThat(response.getUndertimeMinutes()).isEqualTo(120);
        assertThat(response.getWorkingHours()).isEqualTo(6.0);
    }

    @Test
    void recalculateMonthlyScore_appliesLatePenaltyOnce() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = Employee.builder().id(employeeId).name("Alice").email("alice@example.com").build();
        YearMonth month = YearMonth.of(2026, 4);
        List<Attendance> attendances = buildWeekdayAttendances(month, employeeId, employee);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(eq(employeeId), any(), any()))
                .thenReturn(attendances);
        when(attendanceScoreRepository.findByEmployeeIdAndMonthAndYear(employeeId, 4, 2026))
                .thenReturn(Optional.empty());
        ArgumentCaptor<AttendanceScore> scoreCaptor = ArgumentCaptor.forClass(AttendanceScore.class);
        when(attendanceScoreRepository.save(scoreCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveRequestRepository.findByEmployeeIdAndStatus(any(), any())).thenReturn(List.of());

        AttendanceService.ScoreSnapshot snapshot = attendanceService.recalculateMonthlyScore(employeeId, 2026, 4);

        assertThat(snapshot.lateCount()).isEqualTo(4);
        assertThat(scoreCaptor.getValue().getScore()).isEqualTo(82.0);
    }

    @Test
    void getEmployeeMonthlySummary_countsWorkingLateLeaveAndAbsentDays() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = Employee.builder().id(employeeId).name("Alice").email("alice@example.com").build();
        YearMonth month = YearMonth.of(2026, 4);
        Attendance attendance = Attendance.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .attendanceDate(month.atDay(1))
                .checkInTime(LocalTime.of(8, 45))
                .checkOutTime(LocalTime.of(17, 0))
                .late(true)
                .lateMinutes(15)
                .build();
        attendance.recalculateWorkingHours();

        LeaveRequest approvedLeave = LeaveRequest.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .reason("Vacation")
                .startDate(month.atDay(2))
                .endDate(month.atDay(2))
                .requestedDays(1)
                .status(RequestStatus.APPROVED)
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(eq(employeeId), any(), any()))
                .thenReturn(List.of(attendance));
        when(attendanceScoreRepository.findByEmployeeIdAndMonthAndYear(employeeId, 4, 2026))
                .thenReturn(Optional.empty());
        when(attendanceScoreRepository.save(any(AttendanceScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, RequestStatus.APPROVED))
                .thenReturn(List.of(approvedLeave));

        var summary = attendanceService.getEmployeeMonthlySummary(employeeId, 4, 2026);

        assertThat(summary.getWorkingDays()).isGreaterThan(0);
        assertThat(summary.getPresentDays()).isEqualTo(1);
        assertThat(summary.getLateDays()).isEqualTo(1);
        assertThat(summary.getLeaveDays()).isEqualTo(1);
        assertThat(summary.getAbsentDays()).isEqualTo(summary.getWorkingDays() - 2);
        assertThat(summary.getTotalWorkingHours()).isEqualTo(8.25);
    }

    private List<Attendance> buildWeekdayAttendances(YearMonth month, UUID employeeId, Employee employee) {
        List<LocalDate> weekdays = IntStream.rangeClosed(1, month.lengthOfMonth())
                .mapToObj(month::atDay)
                .filter(date -> date.getDayOfWeek().getValue() < 6)
                .collect(Collectors.toList());

        return weekdays.stream()
                .map(date -> {
                    boolean late = weekdays.indexOf(date) < 4;
                    Attendance attendance = Attendance.builder()
                            .id(UUID.randomUUID())
                            .employee(employee)
                            .attendanceDate(date)
                            .checkInTime(late ? LocalTime.of(8, 45) : LocalTime.of(8, 0))
                            .checkOutTime(LocalTime.of(17, 0))
                            .late(late)
                            .lateMinutes(late ? 15 : 0)
                            .build();
                    attendance.recalculateWorkingHours();
                    return attendance;
                })
                .collect(Collectors.toList());
    }
}
