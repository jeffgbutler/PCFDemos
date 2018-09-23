package com.jgb.data;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jgb.model.User;

public interface UserRepository extends JpaRepository<User, Integer> {

}
