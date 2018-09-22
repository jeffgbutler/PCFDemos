package com.jgb.service;

import java.util.List;

import com.jgb.model.User;

public interface UserService {

    List<User> getAllUsers();
    User getUser(Integer id);
    void insertUser(User user);
}
