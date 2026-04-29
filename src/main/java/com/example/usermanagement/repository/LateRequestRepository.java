package com.example.usermanagement.repository;

import com.example.usermanagement.entity.LateRequest;
import com.example.usermanagement.entity.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LateRequestRepository extends JpaRepository<LateRequest, UUID> {
    List<LateRequest> findByEmployeeId(UUID employeeId);
    List<LateRequest> findByStatus(RequestStatus status);
}
