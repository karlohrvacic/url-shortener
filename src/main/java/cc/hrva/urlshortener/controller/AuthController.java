package cc.hrva.urlshortener.controller;

import cc.hrva.urlshortener.dto.*;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.service.AuthService;
import cc.hrva.urlshortener.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final HttpServletRequest request;

    @Operation(summary = "Register a new user", description = "Create a new user account with email and password.")
    @ApiResponse(responseCode = "400", description = "Validation error or bad request")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody final UserRegisterDto user) {
        log.info("Register controller invoked for user {}", user.getEmail());
        return ResponseEntity.ok(authService.register(user));
    }

    @Operation(summary = "Request password reset", description = "Send a password reset link to the given email address.")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PostMapping("/password-reset")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody final RequestPasswordResetDto dto) {
        log.info("Forgot password controller invoked for {}", dto.getEmail());
        userService.sendPasswordResetLinkToUser(dto);

        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Confirm password reset", description = "Complete the password reset process using the token sent via email.")
    @ApiResponse(responseCode = "400", description = "Validation error or bad request")
    @ApiResponse(responseCode = "404", description = "User or token not found")
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<User> resetPassword(@Valid @RequestBody final PasswordResetDto passwordResetDto) {
        log.info("Forgot password controller invoked for {}", passwordResetDto.getEmail());
        return ResponseEntity.ok(userService.resetPassword(passwordResetDto));
    }

    @Operation(summary = "Login", description = "Authenticate with email and password and receive a JWT token.")
    @ApiResponse(responseCode = "401", description = "Bad credentials")
    @PostMapping("/login")
    public ResponseEntity<JWTTokenDto> login(@Valid @RequestBody final LoginDto login) {
        log.info("Login controller invoked for user {}", login.getEmail());
        return authService.login(login, request);
    }
}
