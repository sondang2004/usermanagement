package com.example.usermanagement.repository;

import com.example.usermanagement.entity.AttendanceScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceScoreRepository extends JpaRepository<AttendanceScore, UUID> {
    Optional<AttendanceScore> findByEmployeeIdAndMonthAndYear(UUID employeeId, int month, int year);
}
