package com.example.usermanagement.dto.request;

import com.example.usermanagement.dto.LeaveDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveConfig {
    private LeaveDetails defaultDetails;
}
