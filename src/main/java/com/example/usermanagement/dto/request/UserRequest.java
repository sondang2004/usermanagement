package com.example.usermanagement.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class UserRequest {
    private String name;
    private String email;
}
