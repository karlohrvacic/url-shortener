package me.oncut.urlshortener.service;

import me.oncut.urlshortener.dto.UserUpdateDto;
import me.oncut.urlshortener.model.User;
import java.util.List;

public interface UserService {

    User register(User user);

    User getUserFromToken();

    User fetchCurrentUser();

    User fetchUserFromEmail(String email);

    void persistUser(User user);

    List<User> fetchAllUsers();

    void deleteUserById(Long id);

    User updateUser(UserUpdateDto userUpdateDto);
}
