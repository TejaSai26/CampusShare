package com.crsms.controller;

import com.crsms.dto.ApiDtos.AnalyticsResponse;
import com.crsms.dto.ApiDtos.StatusUpdate;
import com.crsms.dto.ApiDtos.UserResponse;
import com.crsms.service.AnalyticsService;
import com.crsms.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AnalyticsService analytics;
    private final UserService users;

    public AdminController(AnalyticsService analytics, UserService users) {
        this.analytics = analytics;
        this.users = users;
    }

    @GetMapping("/analytics")
    public AnalyticsResponse analytics() {
        return analytics.snapshot();
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return users.listUsers();
    }

    @PutMapping("/users/{id}/status")
    public UserResponse status(@PathVariable Long id, @Valid @RequestBody StatusUpdate update) {
        return users.setStatus(id, update.accountStatus());
    }
}
