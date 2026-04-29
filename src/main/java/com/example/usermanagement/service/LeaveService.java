package com.example.usermanagement.service;

import com.example.usermanagement.dto.LeaveBalanceDTO;
import com.example.usermanagement.dto.LeaveRequestDTO;
import com.example.usermanagement.entity.Employee;
import com.example.usermanagement.entity.LeaveBalance;
import com.example.usermanagement.entity.LeaveRequest;
import com.example.usermanagement.entity.enums.RequestStatus;
import com.example.usermanagement.exception.InvalidRequestException;
import com.example.usermanagement.exception.ResourceNotFoundException;
import com.example.usermanagement.mapper.EmployeeMapper;
import com.example.usermanagement.repository.EmployeeRepository;
import com.example.usermanagement.repository.LeaveBalanceRepository;
import com.example.usermanagement.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private static final int DEFAULT_ANNUAL_LEAVE_DAYS = 12;

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final AttendanceService attendanceService;

    @Transactional
    public LeaveRequestDTO.Response requestLeave(LeaveRequestDTO.Request request) {
        Employee employee = getEmployee(request.getEmployeeId());
        int requestedDays = calculateRequestedDays(request.getStartDate(), request.getEndDate());
        int year = request.getStartDate().getYear();

        LeaveBalance balance = getOrCreateBalance(employee.getId(), year);
        int reservedDays = countPendingRequestedDays(employee.getId(), year);
        int availableDays = balance.getRemainingLeaveDays() - reservedDays;

        if (requestedDays > availableDays) {
            throw new InvalidRequestException("Requested leave days exceed available balance");
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .reason(request.getReason().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .requestedDays(requestedDays)
                .status(RequestStatus.PENDING)
                .build();

        leaveRequest = leaveRequestRepository.save(leaveRequest);
        return toResponse(leaveRequest);
    }

    @Transactional
    public LeaveRequestDTO.Response approveLeave(UUID id) {
        LeaveRequest leaveRequest = getLeaveRequest(id);
        if (leaveRequest.getStatus() != RequestStatus.PENDING) {
            throw new InvalidRequestException("Only PENDING leave requests can be approved");
        }

        int balanceYear = leaveRequest.getStartDate().getYear();
        LeaveBalance balance = getOrCreateBalance(leaveRequest.getEmployee().getId(), balanceYear);
        if (balance.getRemainingLeaveDays() < leaveRequest.getRequestedDays()) {
            throw new InvalidRequestException("Not enough leave balance to approve this request");
        }

        balance.setUsedLeaveDays(balance.getUsedLeaveDays() + leaveRequest.getRequestedDays());
        balance.setRemainingLeaveDays(balance.getRemainingLeaveDays() - leaveRequest.getRequestedDays());
        leaveBalanceRepository.save(balance);

        leaveRequest.setStatus(RequestStatus.APPROVED);
        leaveRequest.setApprovedAt(LocalDate.now());
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        attendanceService.recalculateScoresForRange(
                leaveRequest.getEmployee().getId(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate()
        );

        return toResponse(leaveRequest);
    }

    @Transactional
    public LeaveRequestDTO.Response rejectLeave(UUID id) {
        LeaveRequest leaveRequest = getLeaveRequest(id);
        if (leaveRequest.getStatus() != RequestStatus.PENDING) {
            throw new InvalidRequestException("Only PENDING leave requests can be rejected");
        }

        leaveRequest.setStatus(RequestStatus.REJECTED);
        leaveRequest.setRejectedAt(LocalDate.now());
        leaveRequest = leaveRequestRepository.save(leaveRequest);
        return toResponse(leaveRequest);
    }

    @Transactional
    public LeaveBalanceDTO.Response getLeaveBalance(UUID employeeId, Integer year) {
        Employee employee = getEmployee(employeeId);
        int targetYear = year != null ? year : LocalDate.now().getYear();
        LeaveBalance balance = getOrCreateBalance(employee.getId(), targetYear);
        return LeaveBalanceDTO.Response.builder()
                .employeeId(employee.getId())
                .year(balance.getYear())
                .totalLeaveDays(balance.getTotalLeaveDays())
                .usedLeaveDays(balance.getUsedLeaveDays())
                .remainingLeaveDays(balance.getRemainingLeaveDays())
                .build();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDTO.Response> getLeaveRequests(UUID employeeId, RequestStatus status) {
        getEmployee(employeeId);
        List<LeaveRequest> requests = status == null
                ? leaveRequestRepository.findByEmployeeId(employeeId)
                : leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, status);
        return requests.stream().map(this::toResponse).toList();
    }

    private int calculateRequestedDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidRequestException("Start date and end date are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new InvalidRequestException("End date must be on or after start date");
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    private LeaveBalance getOrCreateBalance(UUID employeeId, int year) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year)
                .orElseGet(() -> {
                    Employee employee = getEmployee(employeeId);
                    LeaveBalance leaveBalance = LeaveBalance.builder()
                            .employee(employee)
                            .year(year)
                            .totalLeaveDays(DEFAULT_ANNUAL_LEAVE_DAYS)
                            .usedLeaveDays(0)
                            .remainingLeaveDays(DEFAULT_ANNUAL_LEAVE_DAYS)
                            .build();
                    return leaveBalanceRepository.save(leaveBalance);
                });
    }

    private int countPendingRequestedDays(UUID employeeId, int year) {
        return leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, RequestStatus.PENDING).stream()
                .filter(request -> request.getStartDate().getYear() == year || request.getEndDate().getYear() == year)
                .mapToInt(LeaveRequest::getRequestedDays)
                .sum();
    }

    private LeaveRequest getLeaveRequest(UUID id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
    }

    private LeaveRequestDTO.Response toResponse(LeaveRequest leaveRequest) {
        return LeaveRequestDTO.Response.builder()
                .id(leaveRequest.getId())
                .employee(employeeMapper.toResponse(leaveRequest.getEmployee()))
                .reason(leaveRequest.getReason())
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .requestedDays(leaveRequest.getRequestedDays())
                .status(leaveRequest.getStatus())
                .approvedAt(leaveRequest.getApprovedAt())
                .rejectedAt(leaveRequest.getRejectedAt())
                .createdAt(leaveRequest.getCreatedAt())
                .updatedAt(leaveRequest.getUpdatedAt())
                .build();
    }

    private Employee getEmployee(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
    }
}
