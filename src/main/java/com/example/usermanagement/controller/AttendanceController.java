package com.example.usermanagement.controller;

import com.example.usermanagement.dto.ApiResponse;
import com.example.usermanagement.dto.request.AttendanceCheckInRequest;
import com.example.usermanagement.dto.request.AttendanceCheckOutRequest;
import com.example.usermanagement.dto.response.AttendanceMonthlySummaryResponse;
import com.example.usermanagement.dto.response.AttendancePenaltyDayResponse;
import com.example.usermanagement.dto.response.AttendanceResponse;
import com.example.usermanagement.dto.response.AttendanceSummaryResponse;
import com.example.usermanagement.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    public ApiResponse<AttendanceResponse> checkIn(@Valid @RequestBody AttendanceCheckInRequest request) {
        return ApiResponse.success(attendanceService.checkIn(request), "Check-in successful");
    }

    @PostMapping("/check-out")
    public ApiResponse<AttendanceResponse> checkOut(@Valid @RequestBody AttendanceCheckOutRequest request) {
        return ApiResponse.success(attendanceService.checkOut(request), "Check-out successful");
    }

    @GetMapping("/{employeeId}")
    public ApiResponse<AttendanceMonthlySummaryResponse> getAttendance(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ApiResponse.success(attendanceService.getAttendance(employeeId, month, year, page, size), "Attendance retrieved successfully");
    }


    @GetMapping("/{employeeId}/summary")
    public ApiResponse<AttendanceSummaryResponse> getAttendanceSummary(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ApiResponse.success(attendanceService.getEmployeeMonthlySummary(employeeId, month, year), "Attendance summary retrieved successfully");
    }

    @GetMapping("/{employeeId}/penalty-days")
    public ApiResponse<List<AttendancePenaltyDayResponse>> getPenaltyDays(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ApiResponse.success(attendanceService.getEmployeePenaltyDays(employeeId, month, year), "Penalty days retrieved successfully");
    }
}
