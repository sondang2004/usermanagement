package com.example.usermanagement.dto.response;

import com.example.usermanagement.dto.LeaveDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveAllocation {
    private UUID employeeId;
    private LeaveDetails leaveDetails;
}
