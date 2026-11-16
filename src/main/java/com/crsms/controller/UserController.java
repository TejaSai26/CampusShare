package com.crsms.controller;

import com.crsms.dto.ApiDtos.ProfileUpdate;
import com.crsms.dto.ApiDtos.UserResponse;
import com.crsms.service.CurrentUserService;
import com.crsms.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final CurrentUserService current;
    private final UserService users;

    public UserController(CurrentUserService current, UserService users) {
        this.current = current;
        this.users = users;
    }

    @GetMapping("/me")
    public UserResponse me() {
        return users.toResponse(current.require());
    }

    @PutMapping("/me")
    public UserResponse update(@Valid @RequestBody ProfileUpdate update) {
        return users.updateProfile(current.require(), update);
    }
}
