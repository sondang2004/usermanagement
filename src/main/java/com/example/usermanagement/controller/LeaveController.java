package com.example.usermanagement.controller;

import com.example.usermanagement.dto.ApiResponse;
import com.example.usermanagement.dto.request.LeaveRequestRequest;
import com.example.usermanagement.dto.response.LeaveBalanceResponse;
import com.example.usermanagement.dto.response.LeaveRequestResponse;
import com.example.usermanagement.entity.enums.RequestStatus;
import com.example.usermanagement.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LeaveRequestResponse> requestLeave(@Valid @RequestBody LeaveRequestRequest request) {
        return ApiResponse.success(leaveService.requestLeave(request), "Leave request created successfully");
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<LeaveRequestResponse> approveLeave(@PathVariable UUID id) {
        return ApiResponse.success(leaveService.approveLeave(id), "Leave request approved successfully");
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<LeaveRequestResponse> rejectLeave(@PathVariable UUID id) {
        return ApiResponse.success(leaveService.rejectLeave(id), "Leave request rejected successfully");
    }

    @GetMapping("/balance/{employeeId}")
    public ApiResponse<LeaveBalanceResponse> getLeaveBalance(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) Integer year) {
        return ApiResponse.success(leaveService.getLeaveBalance(employeeId, year), "Leave balance retrieved successfully");
    }

    @GetMapping("/requests/{employeeId}")
    public ApiResponse<List<LeaveRequestResponse>> getLeaveRequests(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) RequestStatus status) {
        return ApiResponse.success(leaveService.getLeaveRequests(employeeId, status), "Leave requests retrieved successfully");
    }
}
