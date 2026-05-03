package com.example.usermanagement.service;

import com.example.usermanagement.dto.request.LeaveRequestRequest;
import com.example.usermanagement.dto.response.LeaveBalanceResponse;
import com.example.usermanagement.dto.response.LeaveRequestResponse;
import com.example.usermanagement.entity.Employee;
import com.example.usermanagement.entity.LeaveBalance;
import com.example.usermanagement.entity.LeaveRequest;
import com.example.usermanagement.entity.enums.RequestStatus;
import com.example.usermanagement.exception.InvalidRequestException;
import com.example.usermanagement.exception.ResourceNotFoundException;
import com.example.usermanagement.mapper.LeaveBalanceMapper;
import com.example.usermanagement.mapper.LeaveRequestMapper;
import com.example.usermanagement.repository.EmployeeRepository;
import com.example.usermanagement.repository.LeaveBalanceRepository;
import com.example.usermanagement.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private static final int DEFAULT_ANNUAL_LEAVE_DAYS = 12;

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    private final LeaveBalanceMapper leaveBalanceMapper;
    private final AttendanceService attendanceService;

    @Transactional
    public LeaveRequestResponse requestLeave(LeaveRequestRequest request) {
        Employee employee = getEmployee(request.getEmployeeId());
        int requestedDays = calculateRequestedDays(request.getStartDate(), request.getEndDate());
        int year = request.getStartDate().getYear();

        LeaveBalance balance = getOrCreateBalance(employee.getId(), year);
        int reservedDays = countPendingRequestedDays(employee.getId(), year);
        int availableDays = balance.getRemainingLeaveDays() - reservedDays;

        if (requestedDays > availableDays) {
            throw new InvalidRequestException("Requested leave days exceed available balance");
        }

        LeaveRequest leaveRequest = leaveRequestMapper.toEntity(request, employee);
        leaveRequest.setRequestedDays(requestedDays);
        leaveRequest.setStatus(RequestStatus.PENDING);

        return leaveRequestMapper.toResponse(leaveRequestRepository.save(leaveRequest));
    }

    @Transactional
    public LeaveRequestResponse approveLeave(UUID id) {
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
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        attendanceService.recalculateScoresForRange(
                saved.getEmployee().getId(),
                saved.getStartDate(),
                saved.getEndDate()
        );

        return leaveRequestMapper.toResponse(saved);
    }

    @Transactional
    public LeaveRequestResponse rejectLeave(UUID id) {
        LeaveRequest leaveRequest = getLeaveRequest(id);
        if (leaveRequest.getStatus() != RequestStatus.PENDING) {
            throw new InvalidRequestException("Only PENDING leave requests can be rejected");
        }

        leaveRequest.setStatus(RequestStatus.REJECTED);
        leaveRequest.setRejectedAt(LocalDate.now());
        return leaveRequestMapper.toResponse(leaveRequestRepository.save(leaveRequest));
    }

    @Transactional
    public LeaveBalanceResponse getLeaveBalance(UUID employeeId, Integer year) {
        Employee employee = getEmployee(employeeId);
        int targetYear = year != null ? year : LocalDate.now().getYear();
        LeaveBalance balance = getOrCreateBalance(employee.getId(), targetYear);
        return leaveBalanceMapper.toResponse(balance, employee.getId());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getLeaveRequests(UUID employeeId, RequestStatus status) {
        getEmployee(employeeId);
        List<LeaveRequest> requests = status == null
                ? leaveRequestRepository.findByEmployeeId(employeeId)
                : leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, status);
        return requests.stream().map(leaveRequestMapper::toResponse).toList();
    }

    private int calculateRequestedDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidRequestException("Start date and end date are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new InvalidRequestException("End date must be on or after start date");
        }
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
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

    private Employee getEmployee(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
    }
}
