package com.example.usermanagement.service;

import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse create(User user);
    List<UserResponse> getAll();
    UserResponse getById(UUID id);
    UserResponse update(UUID id, User user);
    void delete(UUID id);
}
