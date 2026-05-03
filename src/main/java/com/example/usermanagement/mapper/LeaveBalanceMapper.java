package com.example.usermanagement.mapper;

import com.example.usermanagement.dto.response.LeaveBalanceResponse;
import com.example.usermanagement.entity.LeaveBalance;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LeaveBalanceMapper {

    public LeaveBalanceResponse toResponse(LeaveBalance leaveBalance, UUID employeeId) {
        if (leaveBalance == null) {
            return null;
        }

        return LeaveBalanceResponse.builder()
                .employeeId(employeeId)
                .year(leaveBalance.getYear())
                .totalLeaveDays(leaveBalance.getTotalLeaveDays())
                .usedLeaveDays(leaveBalance.getUsedLeaveDays())
                .remainingLeaveDays(leaveBalance.getRemainingLeaveDays())
                .build();
    }
}
