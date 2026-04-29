package com.example.usermanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "attendance",
        uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = {"employee_id", "attendance_date"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance extends BaseAuditEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(nullable = false)
    private boolean late;

    @Column(nullable = false)
    private boolean undertime;

    @Column(nullable = false)
    private boolean overtime;

    @Column(name = "late_minutes", nullable = false)
    private int lateMinutes;

    @Column(name = "undertime_minutes", nullable = false)
    private int undertimeMinutes;

    @Column(name = "overtime_minutes", nullable = false)
    private int overtimeMinutes;

    @Column(name = "working_hours", nullable = false)
    private double workingHours;

    public void recalculateWorkingHours() {
        if (checkInTime == null || checkOutTime == null) {
            workingHours = 0.0;
            undertime = false;
            overtime = false;
            undertimeMinutes = 0;
            overtimeMinutes = 0;
            return;
        }

        long minutes = Duration.between(checkInTime, checkOutTime).toMinutes();
        if (minutes < 0) {
            minutes += 24 * 60;
        }

        workingHours = minutes / 60.0;
        undertimeMinutes = 0;
        overtimeMinutes = 0;
        undertime = false;
        overtime = false;

        long expectedMinutes = 8 * 60;
        if (minutes < expectedMinutes) {
            undertime = true;
            undertimeMinutes = (int) (expectedMinutes - minutes);
        } else if (minutes > expectedMinutes) {
            overtime = true;
            overtimeMinutes = (int) (minutes - expectedMinutes);
        }
    }
}
