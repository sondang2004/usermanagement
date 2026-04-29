package com.example.usermanagement.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {
    private boolean isAbsent;
    private boolean hasApprovedLeave;
    private boolean isLate;

    public boolean hasApprovedLeave() {
        return this.hasApprovedLeave;
    }
}
