package com.example.usermanagement.service;

import com.example.usermanagement.dto.EmployeeDTO;
import com.example.usermanagement.dto.PageResponse;
import com.example.usermanagement.entity.Employee;
import com.example.usermanagement.entity.LeaveBalance;
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

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final AttendanceScoreRepository attendanceScoreRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    public EmployeeDTO.Response createEmployee(EmployeeDTO.Request request) {
        Employee employee = employeeMapper.toEntity(request);
        employee = employeeRepository.save(employee);
        return toResponse(employee);
    }

    public EmployeeDTO.Response updateEmployee(UUID id, EmployeeDTO.Request request) {
        Employee employee = getEmployeeById(id);
        employeeMapper.updateEntity(employee, request);
        employee = employeeRepository.save(employee);
        return toResponse(employee);
    }

    public void deleteEmployee(UUID id) {
        Employee employee = getEmployeeById(id);
        employeeRepository.delete(employee);
    }

    public EmployeeDTO.Response getEmployee(UUID id) {
        return toResponse(getEmployeeById(id));
    }

    public PageResponse<EmployeeDTO.Response> getEmployees(String keyword, int page, int size) {
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

    private EmployeeDTO.Response toResponse(Employee employee) {
        EmployeeDTO.Response response = employeeMapper.toResponse(employee);
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
}
