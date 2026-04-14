package com.example.usermanagement.mapper;

import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.entity.User;

public class UserMapper {

    public static UserResponse toResponse(User user) {
        if (user == null) return null;

        UserResponse res = new UserResponse();
        res.setId(user.getId());
        res.setName(user.getName());
        res.setEmail(user.getEmail());
        return res;
    }
}
