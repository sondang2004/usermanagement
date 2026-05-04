package com.example.usermanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceResponse {

    private UUID employeeId;
    private int year;
    private int totalLeaveDays;
    private int usedLeaveDays;
    private int remainingLeaveDays;
}
