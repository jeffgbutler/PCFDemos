package com.jgb.service;

import java.util.List;

import com.jgb.model.User;

public interface BatchInsertService {
    void insertBatch(List<User> users);
}
