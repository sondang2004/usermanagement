package com.example.usermanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(
        name = "leave_balances",
        uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = {"employee_id", "balance_year"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance extends BaseAuditEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "balance_year", nullable = false)
    private int year;

    @Column(name = "total_leave_days", nullable = false)
    private int totalLeaveDays;

    @Column(name = "used_leave_days", nullable = false)
    private int usedLeaveDays;

    @Column(name = "remaining_leave_days", nullable = false)
    private int remainingLeaveDays;
}
