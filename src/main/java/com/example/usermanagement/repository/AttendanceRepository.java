package com.example.usermanagement.repository;

import com.example.usermanagement.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    Optional<Attendance> findByEmployeeIdAndAttendanceDate(UUID employeeId, LocalDate attendanceDate);

    Optional<Attendance> findTopByEmployeeIdAndCheckOutTimeIsNullOrderByAttendanceDateDescCheckInTimeDesc(UUID employeeId);

    List<Attendance> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(UUID employeeId, LocalDate startDate, LocalDate endDate);

    Page<Attendance> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(UUID employeeId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("""
    SELECT a FROM Attendance a
    WHERE a.employee.id = :employeeId
    AND a.attendanceDate BETWEEN :startDate AND :endDate
    AND (
        a.late = true 
        OR a.undertime = true
        OR a.checkOutTime IS NULL
        OR a.workingHours = 0
    )
    ORDER BY a.attendanceDate ASC
""")
    List<Attendance> findPenaltyRiskDays(
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

}
