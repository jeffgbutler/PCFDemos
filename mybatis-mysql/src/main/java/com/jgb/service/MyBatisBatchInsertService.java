package com.jgb.service;

import java.util.List;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jgb.mapper.UserMapper;
import com.jgb.model.User;

@Service
public class MyBatisBatchInsertService implements BatchInsertService {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Override
    @Transactional
    public void insertBatch(List<User> users) {

        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            UserMapper mapper = sqlSession.getMapper(UserMapper.class);
            for (User user : users) {
                mapper.insertUser(user);
            }
            sqlSession.commit();
        }
    }
}
