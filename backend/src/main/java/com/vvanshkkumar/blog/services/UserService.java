package com.vvanshkkumar.blog.services;

import com.vvanshkkumar.blog.domain.entities.User;

import java.util.UUID;

public interface UserService {
    User getUserById(UUID id);
    User registerUser(String name, String email, String password);
}
