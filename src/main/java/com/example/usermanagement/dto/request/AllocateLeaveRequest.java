package com.example.usermanagement.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllocateLeaveRequest {
    private LeaveConfig config;
    private TrainingEmployee mockEmployeeData;
}
