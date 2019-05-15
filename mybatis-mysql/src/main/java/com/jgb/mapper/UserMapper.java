package com.jgb.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.jgb.model.User;

@Mapper
public interface UserMapper {

    @Select({
        "select id, first_name, last_name from user order by id"
    })
    List<User> selectAll();
    
    @Select({
        "select id, first_name, last_name from user where id = #{value}"
    })
    User selectById(Integer id);
    
    @Insert({
        "insert into user (first_name, last_name) values(#{firstName}, #{lastName})"
    })
    @Options(useGeneratedKeys=true, keyProperty="id")
    int insertUser(User user);
}
