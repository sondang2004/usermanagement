package com.example.usermanagement.repository;

import com.example.usermanagement.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {
    Optional<LeaveBalance> findByEmployeeIdAndYear(UUID employeeId, int year);
    Optional<LeaveBalance> findTopByEmployeeIdOrderByYearDesc(UUID employeeId);
}
