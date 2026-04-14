package com.example.usermanagement.controller;

import com.example.usermanagement.dto.response.ApiResponse;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // CREATE
    @PostMapping
    public ApiResponse<UserResponse> create(@RequestBody User user) {
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("User created successfully")
                .result(userService.create(user))
                .build();
    }

    // GET ALL
    @GetMapping
    public ApiResponse<List<UserResponse>> getAll() {
        return ApiResponse.<List<UserResponse>>builder()
                .code(1000)
                .message("Success")
                .result(userService.getAll())
                .build();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("Success")
                .result(userService.getById(id))
                .build();
    }

    // UPDATE
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(
            @PathVariable UUID id,
            @RequestBody User user
    ) {
        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("Updated successfully")
                .result(userService.update(id, user))
                .build();
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        userService.delete(id);

        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Deleted successfully")
                .build();
    }
}
