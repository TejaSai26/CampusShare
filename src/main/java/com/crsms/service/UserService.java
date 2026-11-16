package com.crsms.service;

import com.crsms.dto.ApiDtos.LoginRequest;
import com.crsms.dto.ApiDtos.ProfileUpdate;
import com.crsms.dto.ApiDtos.RegisterRequest;
import com.crsms.dto.ApiDtos.UserResponse;
import com.crsms.exception.ApiException;
import com.crsms.model.enums.AccountStatus;
import com.crsms.model.enums.UserRole;
import com.crsms.model.user.Faculty;
import com.crsms.model.user.Staff;
import com.crsms.model.user.Student;
import com.crsms.model.user.User;
import com.crsms.repository.UserRepository;
import com.crsms.security.CampusUserDetails;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public UserService(UserRepository users, PasswordEncoder encoder, AuthenticationManager authenticationManager,
                       SecurityContextRepository securityContextRepository) {
        this.users = users;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(409, "An account with that email already exists");
        }
        UserRole role = parseMemberRole(request.role());
        String memberId = blankToNull(request.memberId());
        if (memberId == null) {
            memberId = role.name().substring(0, 3) + "-" + Math.abs(email.hashCode());
        }
        String hash = encoder.encode(request.password());
        User user = switch (role) {
            case STUDENT -> new Student(email, hash, request.fullName().trim(), blankToNull(request.department()), memberId);
            case FACULTY -> new Faculty(email, hash, request.fullName().trim(), blankToNull(request.department()),
                    memberId, blankToNull(request.designation()));
            case STAFF -> new Staff(email, hash, request.fullName().trim(), blankToNull(request.department()),
                    memberId, blankToNull(request.designation()), UserRole.STAFF);
            case ADMIN -> throw new ApiException(400, "Administrator accounts are not self-registered");
        };
        return toResponse(users.save(user));
    }

    public UserResponse login(LoginRequest request, HttpServletRequest http, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, http, response);
        CampusUserDetails details = (CampusUserDetails) authentication.getPrincipal();
        return toResponse(details.getUser());
    }

    @Transactional
    public UserResponse updateProfile(User current, ProfileUpdate update) {
        current.rename(update.fullName());
        current.changeDepartment(update.department());
        return toResponse(users.save(current));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return users.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse setStatus(Long id, String status) {
        User user = users.findById(id).orElseThrow(() -> new ApiException(404, "User not found"));
        AccountStatus next = AccountStatus.valueOf(status);
        if (next == AccountStatus.ACTIVE) {
            user.activate();
        } else {
            user.deactivate();
        }
        return toResponse(user);
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(),
                user.getAccountStatus().name(), user.getDepartment(), user.memberLabel());
    }

    private UserRole parseMemberRole(String raw) {
        try {
            return UserRole.valueOf(raw.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new ApiException(400, "Role must be STUDENT, FACULTY, or STAFF");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
