package com.jgb.http;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jgb.model.User;
import com.jgb.service.BatchInsertService;

@RestController
@RequestMapping("/userBatch")
public class UserBatchController {

    @Autowired
    private BatchInsertService userService;
    
    @PostMapping
    public ResponseEntity<String> insert(@RequestBody List<User> users) {
        userService.insertBatch(users);
        
        return new ResponseEntity<>("OK", HttpStatus.OK);
    }
}
