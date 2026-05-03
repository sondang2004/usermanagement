package com.example.usermanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceCheckOutRequest {

    @NotNull(message = "Employee ID is required")
    private UUID employeeId;

    private LocalDateTime checkOutAt;
}
