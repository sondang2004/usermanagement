package com.example.usermanagement.service;

import com.example.usermanagement.dto.PageResponse;
import com.example.usermanagement.dto.request.EmployeeRequest;
import com.example.usermanagement.dto.response.EmployeeResponse;
import com.example.usermanagement.entity.Employee;
import com.example.usermanagement.entity.LeaveBalance;
import com.example.usermanagement.exception.InvalidRequestException;
import com.example.usermanagement.exception.ResourceNotFoundException;
import com.example.usermanagement.mapper.EmployeeMapper;
import com.example.usermanagement.repository.AttendanceScoreRepository;
import com.example.usermanagement.repository.EmployeeRepository;
import com.example.usermanagement.repository.LeaveBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final AttendanceScoreRepository attendanceScoreRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new InvalidRequestException("Email exists");
        }

        simulateProcessingLatency();

        Employee employee = employeeMapper.toEntity(request);
        return toResponse(employeeRepository.save(employee));
    }

    public EmployeeResponse updateEmployee(UUID id, EmployeeRequest request) {
        Employee employee = getEmployeeById(id);
        employeeMapper.updateEntity(employee, request);
        return toResponse(employeeRepository.save(employee));
    }

    public void deleteEmployee(UUID id) {
        Employee employee = getEmployeeById(id);
        employeeRepository.delete(employee);
    }

    public EmployeeResponse getEmployee(UUID id) {
        return toResponse(getEmployeeById(id));
    }

    public PageResponse<EmployeeResponse> getEmployees(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Employee> employees;
        if (keyword != null && !keyword.isBlank()) {
            employees = employeeRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword, pageable);
        } else {
            employees = employeeRepository.findAll(pageable);
        }
        return PageResponse.of(employees.map(this::toResponse));
    }

    public Employee getEmployeeById(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    private EmployeeResponse toResponse(Employee employee) {
        EmployeeResponse response = employeeMapper.toResponse(employee);
        if (employee.getId() == null) {
            return response;
        }

        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        attendanceScoreRepository.findByEmployeeIdAndMonthAndYear(employee.getId(), currentMonth, currentYear)
                .ifPresent(score -> response.setCurrentMonthAttendanceScore(score.getScore()));

        LeaveBalance leaveBalance = leaveBalanceRepository.findByEmployeeIdAndYear(employee.getId(), currentYear)
                .orElse(null);
        if (leaveBalance != null) {
            response.setCurrentYearLeaveBalance(leaveBalance.getRemainingLeaveDays());
        }

        return response;
    }

    public List<String> simulateConcurrentCreate(String email, int threadCount) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        List<String> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    EmployeeResponse created = createEmployee(EmployeeRequest.builder()
                            .name("Employee-" + index)
                            .email(email)
                            .build());
                    synchronized (results) {
                        results.add("SUCCESS:" + created.getId());
                    }
                } catch (Exception ex) {
                    synchronized (results) {
                        results.add("ERROR:" + ex.getMessage());
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        try {
            ready.await();
            start.countDown();
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }

        return results;
    }

    public List<String> simulateTwoConcurrentCreates(String email) {
        return simulateConcurrentCreate(email, 2);
    }

    public List<String> simulateTwoConcurrentCreates(EmployeeRequest request) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        List<String> results = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    EmployeeResponse created = createEmployee(EmployeeRequest.builder()
                            .name(request.getName())
                            .email(request.getEmail())
                            .position(request.getPosition())
                            .department(request.getDepartment())
                            .build());
                    synchronized (results) {
                        results.add("SUCCESS-" + index + ":" + created.getId());
                    }
                } catch (Exception ex) {
                    synchronized (results) {
                        results.add("ERROR-" + index + ":" + ex.getMessage());
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        try {
            ready.await();
            start.countDown();
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }

        return results;
    }

    private void simulateProcessingLatency() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
