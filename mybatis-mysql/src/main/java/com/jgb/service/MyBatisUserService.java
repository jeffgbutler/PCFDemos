package com.jgb.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jgb.mapper.UserMapper;
import com.jgb.model.User;

@Service
public class MyBatisUserService implements UserService {

    @Autowired
    private UserMapper userMapper;
    
    @Override
    @Transactional
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }

    @Override
    @Transactional
    public void insertUser(User user) {
        userMapper.insertUser(user);
    }

    @Override
    @Transactional
    public User getUser(Integer id) {
        return userMapper.selectById(id);
    }
}
