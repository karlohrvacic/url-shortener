package cc.hrva.urlshortener.controller;

import java.util.Collections;
import cc.hrva.urlshortener.dto.UpdatePasswordDto;
import cc.hrva.urlshortener.dto.UserDto;
import cc.hrva.urlshortener.dto.UserUpdateDto;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService)).build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldGetCurrentUser() throws Exception {
        final var userDto = UserDto.builder().id(1L).email("test@example.com").build();
        when(userService.fetchCurrentUser()).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void shouldGetAllUsers() throws Exception {
        final var users = Collections.singletonList(User.builder().id(1L).email("test@example.com").build());
        when(userService.fetchAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/v1/user/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void shouldDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/v1/user/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUserById(1L);
    }

    @Test
    void shouldUpdateUser() throws Exception {
        final var user = User.builder().id(1L).email("test@example.com").build();
        when(userService.updateUser(any(UserUpdateDto.class))).thenReturn(user);

        mockMvc.perform(put("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UserUpdateDto.builder().id(1L).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldUpdatePassword() throws Exception {
        final var user = User.builder().id(1L).email("test@example.com").build();
        when(userService.updatePassword(any(UpdatePasswordDto.class))).thenReturn(user);

        mockMvc.perform(put("/api/v1/user/update-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdatePasswordDto.builder().oldPassword("old").newPassword("new").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
