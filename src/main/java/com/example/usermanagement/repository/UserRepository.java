package com.example.usermanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.usermanagement.entity.User;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
