package com.example.usermanagement.controller;

import com.example.usermanagement.dto.ApiResponse;
import com.example.usermanagement.dto.AttendanceDTO;
import com.example.usermanagement.dto.AttendancePenaltyDayDTO;
import com.example.usermanagement.dto.AttendanceSummaryDTO;
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
    public ApiResponse<AttendanceDTO.Response> checkIn(@Valid @RequestBody AttendanceDTO.CheckInRequest request) {
        return ApiResponse.success(attendanceService.checkIn(request), "Check-in successful");
    }

    @PostMapping("/check-out")
    public ApiResponse<AttendanceDTO.Response> checkOut(@Valid @RequestBody AttendanceDTO.CheckOutRequest request) {
        return ApiResponse.success(attendanceService.checkOut(request), "Check-out successful");
    }

    @GetMapping("/{employeeId}")
    public ApiResponse<AttendanceDTO.MonthlySummary> getAttendance(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ApiResponse.success(attendanceService.getAttendance(employeeId, month, year, page, size), "Attendance retrieved successfully");
    }


    @GetMapping("/{employeeId}/summary")
    public ApiResponse<AttendanceSummaryDTO.Response> getAttendanceSummary(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ApiResponse.success(attendanceService.getEmployeeMonthlySummary(employeeId, month, year), "Attendance summary retrieved successfully");
    }

    @GetMapping("/{employeeId}/penalty-days")
    public ApiResponse<List<AttendancePenaltyDayDTO.Response>> getPenaltyDays(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ApiResponse.success(attendanceService.getEmployeePenaltyDays(employeeId, month, year), "Penalty days retrieved successfully");
    }
}
