package com.example.usermanagement.service;

import com.example.usermanagement.dto.request.LeaveRequestRequest;
import com.example.usermanagement.dto.response.EmployeeResponse;
import com.example.usermanagement.dto.response.LeaveRequestResponse;
import com.example.usermanagement.entity.Employee;
import com.example.usermanagement.entity.LeaveBalance;
import com.example.usermanagement.entity.LeaveRequest;
import com.example.usermanagement.entity.enums.RequestStatus;
import com.example.usermanagement.mapper.EmployeeMapper;
import com.example.usermanagement.mapper.LeaveBalanceMapper;
import com.example.usermanagement.mapper.LeaveRequestMapper;
import com.example.usermanagement.repository.EmployeeRepository;
import com.example.usermanagement.repository.LeaveBalanceRepository;
import com.example.usermanagement.repository.LeaveRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AttendanceService attendanceService;

    private final EmployeeMapper employeeMapper = new EmployeeMapper();
    private final LeaveBalanceMapper leaveBalanceMapper = new LeaveBalanceMapper();
    private LeaveService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new LeaveService(
                leaveRequestRepository,
                leaveBalanceRepository,
                employeeRepository,
                new LeaveRequestMapper(employeeMapper),
                leaveBalanceMapper,
                attendanceService
        );
    }

    @Test
    void requestLeave_rejectsWhenBalanceIsInsufficient() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = Employee.builder().id(employeeId).name("Bob").email("bob@example.com").build();
        LeaveBalance balance = LeaveBalance.builder()
                .employee(employee)
                .year(2026)
                .totalLeaveDays(12)
                .usedLeaveDays(10)
                .remainingLeaveDays(2)
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, 2026)).thenReturn(Optional.of(balance));
        when(leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, RequestStatus.PENDING)).thenReturn(java.util.List.of());

        LeaveRequestRequest request = LeaveRequestRequest.builder()
                .employeeId(employeeId)
                .reason("Vacation")
                .startDate(LocalDate.of(2026, 4, 10))
                .endDate(LocalDate.of(2026, 4, 13))
                .build();

        assertThatThrownBy(() -> service.requestLeave(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Requested leave days exceed available balance");
    }

    @Test
    void approveLeave_deductsBalanceAndMarksApproved() {
        UUID employeeId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        Employee employee = Employee.builder().id(employeeId).name("Bob").email("bob@example.com").build();
        AtomicReference<LeaveBalance> balanceRef = new AtomicReference<>(LeaveBalance.builder()
                .employee(employee)
                .year(2026)
                .totalLeaveDays(12)
                .usedLeaveDays(0)
                .remainingLeaveDays(12)
                .build());

        AtomicReference<LeaveRequest> requestRef = new AtomicReference<>(LeaveRequest.builder()
                .id(requestId)
                .employee(employee)
                .reason("Vacation")
                .startDate(LocalDate.of(2026, 4, 10))
                .endDate(LocalDate.of(2026, 4, 12))
                .requestedDays(3)
                .status(RequestStatus.PENDING)
                .build());

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, 2026)).thenAnswer(invocation -> Optional.ofNullable(balanceRef.get()));
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(invocation -> {
            balanceRef.set(invocation.getArgument(0));
            return balanceRef.get();
        });
        when(leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, RequestStatus.PENDING)).thenReturn(java.util.List.of());
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            requestRef.set(invocation.getArgument(0));
            return requestRef.get();
        });
        when(leaveRequestRepository.findById(requestId)).thenAnswer(invocation -> Optional.ofNullable(requestRef.get()));

        LeaveRequestResponse created = service.requestLeave(LeaveRequestRequest.builder()
                .employeeId(employeeId)
                .reason("Vacation")
                .startDate(LocalDate.of(2026, 4, 10))
                .endDate(LocalDate.of(2026, 4, 12))
                .build());

        assertThat(created.getStatus()).isEqualTo(RequestStatus.PENDING);

        LeaveRequestResponse approved = service.approveLeave(requestId);

        assertThat(approved.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(balanceRef.get().getUsedLeaveDays()).isEqualTo(3);
        assertThat(balanceRef.get().getRemainingLeaveDays()).isEqualTo(9);
    }

    @Test
    void rejectLeave_doesNotChangeBalance() {
        UUID employeeId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        Employee employee = Employee.builder().id(employeeId).name("Bob").email("bob@example.com").build();
        AtomicReference<LeaveBalance> balanceRef = new AtomicReference<>(LeaveBalance.builder()
                .employee(employee)
                .year(2026)
                .totalLeaveDays(12)
                .usedLeaveDays(1)
                .remainingLeaveDays(11)
                .build());

        AtomicReference<LeaveRequest> requestRef = new AtomicReference<>(LeaveRequest.builder()
                .id(requestId)
                .employee(employee)
                .reason("Personal")
                .startDate(LocalDate.of(2026, 4, 10))
                .endDate(LocalDate.of(2026, 4, 10))
                .requestedDays(1)
                .status(RequestStatus.PENDING)
                .build());

        when(leaveRequestRepository.findById(requestId)).thenAnswer(invocation -> Optional.ofNullable(requestRef.get()));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            requestRef.set(invocation.getArgument(0));
            return requestRef.get();
        });

        LeaveRequestResponse rejected = service.rejectLeave(requestId);

        assertThat(rejected.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(balanceRef.get().getUsedLeaveDays()).isEqualTo(1);
        assertThat(balanceRef.get().getRemainingLeaveDays()).isEqualTo(11);
    }
}
