package com.example.usermanagement.controller;

import com.example.usermanagement.dto.ApiResponse;
import com.example.usermanagement.dto.PageResponse;
import com.example.usermanagement.dto.request.EmployeeRequest;
import com.example.usermanagement.dto.response.EmployeeResponse;
import com.example.usermanagement.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.success(employeeService.createEmployee(request), "Employee created successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<EmployeeResponse> updateEmployee(@PathVariable UUID id, @Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.success(employeeService.updateEmployee(id, request), "Employee updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteEmployee(@PathVariable UUID id) {
        employeeService.deleteEmployee(id);
        return ApiResponse.success(null, "Employee deleted successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<EmployeeResponse> getEmployee(@PathVariable UUID id) {
        return ApiResponse.success(employeeService.getEmployee(id), "Employee retrieved successfully");
    }

    @GetMapping
    public ApiResponse<PageResponse<EmployeeResponse>> getEmployees(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(employeeService.getEmployees(keyword, page, size), "Employees retrieved successfully");
    }

    @PostMapping("/debug/concurrent-create")
    public ApiResponse<List<String>> concurrentCreate(
            @RequestParam String email,
            @RequestParam(defaultValue = "10") int threadCount) {
        return ApiResponse.success(employeeService.simulateConcurrentCreate(email, threadCount), "Concurrent create simulated");
    }

    @PostMapping("/debug/concurrent-create-two")
    public ApiResponse<List<String>> concurrentCreateTwo(@Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.success(employeeService.simulateTwoConcurrentCreates(request), "Concurrent create x2 simulated");
    }
}
