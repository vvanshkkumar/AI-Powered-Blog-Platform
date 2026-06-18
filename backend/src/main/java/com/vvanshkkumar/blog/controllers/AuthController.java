package com.vvanshkkumar.blog.controllers;

import com.vvanshkkumar.blog.domain.dtos.AuthResponse;
import com.vvanshkkumar.blog.domain.dtos.AuthorDto;
import com.vvanshkkumar.blog.domain.dtos.LoginRequest;
import com.vvanshkkumar.blog.domain.dtos.RegisterRequest;
import com.vvanshkkumar.blog.domain.entities.User;
import com.vvanshkkumar.blog.services.AuthenticationService;
import com.vvanshkkumar.blog.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        UserDetails userDetails = authenticationService.authenticate(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        );
        String tokenValue = authenticationService.generateToken(userDetails);
        AuthResponse authResponse = AuthResponse.builder()
                .token(tokenValue)
                .expiresIn(86400)
                .build();
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        User newUser = userService.registerUser(
                registerRequest.getName(),
                registerRequest.getEmail(),
                registerRequest.getPassword()
        );

        // Auto-login after registration: authenticate and return token
        UserDetails userDetails = authenticationService.authenticate(
                registerRequest.getEmail(),
                registerRequest.getPassword()
        );
        String tokenValue = authenticationService.generateToken(userDetails);
        AuthResponse authResponse = AuthResponse.builder()
                .token(tokenValue)
                .expiresIn(86400)
                .build();
        return new ResponseEntity<>(authResponse, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthorDto> getCurrentUser(@RequestAttribute UUID userId) {
        User user = userService.getUserById(userId);
        AuthorDto profile = AuthorDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
        return ResponseEntity.ok(profile);
    }
}
