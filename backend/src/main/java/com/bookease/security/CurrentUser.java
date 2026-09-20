package com.bookease.security;

import com.bookease.common.exception.BusinessException;
import com.bookease.common.exception.ErrorCode;
import com.bookease.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public AuthenticatedUser require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(
                    ErrorCode.AUTHENTICATION_REQUIRED,
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required");
        }
        return user;
    }

    public Long requireUserId() {
        return require().userId();
    }

    public AuthenticatedUser requireRole(UserRole... roles) {
        AuthenticatedUser user = require();
        for (UserRole role : roles) {
            if (user.role() == role) {
                return user;
            }
        }
        throw new BusinessException(
                ErrorCode.ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                "Access is denied");
    }
}
