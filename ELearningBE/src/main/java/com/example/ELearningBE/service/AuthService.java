package com.example.ELearningBE.service;

import com.example.ELearningBE.dto.request.auth.LoginRequest;
import com.example.ELearningBE.dto.request.auth.RegisterRequest;
import com.example.ELearningBE.dto.response.auth.AuthResponse;
import com.example.ELearningBE.dto.response.auth.RegisterPendingResponse;
import com.example.ELearningBE.dto.response.user.UserResponse;
import com.example.ELearningBE.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    RegisterPendingResponse register(RegisterRequest request, HttpServletRequest httpRequest);

    String confirmAccount(String token, HttpServletRequest httpRequest);

    AuthResponse login(LoginRequest request, HttpServletResponse response);

    AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);

    UserResponse getCurrentUser(CustomUserDetails userDetails);
}
