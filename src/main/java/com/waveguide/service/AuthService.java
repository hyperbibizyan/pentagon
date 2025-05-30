package com.waveguide.service;

import com.waveguide.exception.AuthenticationException;
import com.waveguide.model.dto.request.LoginRequest;
import com.waveguide.model.dto.request.UserRegistrationRequest;
import com.waveguide.model.dto.response.AuthResponse;
import com.waveguide.model.entity.User;
import com.waveguide.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final LogService logService;

    @Transactional
    public AuthResponse register(UserRegistrationRequest request) {
        User user = userService.registerUser(request);
        
        String token = tokenProvider.generateToken(user);
        long expiresIn = tokenProvider.getExpirationFromToken(token).getTime();
        
        return AuthResponse.builder()
                .token(token)
                .expiresIn(expiresIn)
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            User user = userService.getUserByEmail(request.getEmail())
                    .orElseThrow(() -> new AuthenticationException("User not found"));
            
            String token = tokenProvider.generateToken(user);
            long expiresIn = tokenProvider.getExpirationFromToken(token).getTime();
            
            logService.logUserAction(user, "LOGIN", "User logged in successfully");
            
            return AuthResponse.builder()
                    .token(token)
                    .expiresIn(expiresIn)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build();
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new AuthenticationException("Invalid email or password");
        }
    }
    
    @Transactional
    public void logout(String token, User user) {
        tokenProvider.blacklistToken(token);
        logService.logUserAction(user, "LOGOUT", "User logged out successfully");
    }
}