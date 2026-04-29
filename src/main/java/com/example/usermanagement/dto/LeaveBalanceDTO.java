package com.example.usermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class LeaveBalanceDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID employeeId;
        private int year;
        private int totalLeaveDays;
        private int usedLeaveDays;
        private int remainingLeaveDays;
    }
}
