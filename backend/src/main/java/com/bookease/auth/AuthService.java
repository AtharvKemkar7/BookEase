package com.bookease.auth;

import com.bookease.common.exception.BusinessException;
import com.bookease.common.exception.ErrorCode;
import com.bookease.common.validation.EmailNormalizer;
import com.bookease.security.CurrentUser;
import com.bookease.security.JwtService;
import com.bookease.user.User;
import com.bookease.user.UserMapper;
import com.bookease.user.UserRepository;
import com.bookease.user.UserResponse;
import com.bookease.user.UserRole;
import com.bookease.user.UserStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw emailAlreadyRegistered();
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(normalizePhone(request.phone()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);

        try {
            User saved = userRepository.saveAndFlush(user);
            return UserMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw emailAlreadyRegistered();
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(AuthService::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_DISABLED,
                    HttpStatus.FORBIDDEN,
                    "Account is not active");
        }

        return new AuthResponse(
                jwtService.createAccessToken(user),
                "Bearer",
                jwtService.expiresInSeconds(),
                UserMapper.toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        Long userId = currentUser.requireUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.AUTHENTICATION_REQUIRED,
                        HttpStatus.UNAUTHORIZED,
                        "Authentication is required"));
        return UserMapper.toResponse(user);
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BusinessException emailAlreadyRegistered() {
        return new BusinessException(
                ErrorCode.EMAIL_ALREADY_REGISTERED,
                HttpStatus.CONFLICT,
                "An account with this email already exists");
    }

    private static BusinessException invalidCredentials() {
        return new BusinessException(
                ErrorCode.INVALID_CREDENTIALS,
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password");
    }
}
