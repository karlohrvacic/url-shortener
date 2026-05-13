package cc.hrva.urlshortener.controller;

import cc.hrva.urlshortener.dto.*;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.service.AuthService;
import cc.hrva.urlshortener.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, userService, request)).build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldRegister() throws Exception {
        when(authService.register(any(UserRegisterDto.class))).thenReturn("test@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UserRegisterDto.builder().email("test@example.com").password("password123").build())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRequestPasswordReset() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RequestPasswordResetDto.builder().email("test@example.com").build())))
                .andExpect(status().isAccepted());

        verify(userService).sendPasswordResetLinkToUser(any(RequestPasswordResetDto.class));
    }

    @Test
    void shouldResetPassword() throws Exception {
        final var user = User.builder().id(1L).email("test@example.com").build();
        when(userService.resetPassword(any(PasswordResetDto.class))).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/reset-password/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(PasswordResetDto.builder().email("test@example.com").token("token").password("newPassword").build())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"));
    }

    @Test
    void shouldLogin() throws Exception {
        final var jwtDto = new JWTTokenDto("Bearer token", UserDto.builder().id(1L).email("test@example.com").build());
        final var headers = new HttpHeaders();
        headers.add("Authorization", "Bearer token");
        when(authService.login(any(LoginDto.class), any())).thenReturn(new ResponseEntity<>(jwtDto, headers, org.springframework.http.HttpStatus.OK));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginDto.builder().email("test@example.com").password("password123").build())))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", "Bearer token"));
    }
}
