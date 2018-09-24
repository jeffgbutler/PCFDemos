package com.jgb.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.jgb.model.User;

public interface UserRepository extends JpaRepository<User, Integer> {

    List<User> findByLastName(@Param("name") String name);
}
