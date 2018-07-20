package com.jgb.loancalculator.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jgb.loancalculator.service.Crasher;

import io.swagger.annotations.ApiOperation;

@RestController
public class CrashController {

    @Autowired
    private Crasher crasher;
    
    @ApiOperation("Warning! Will crash the application 2 seconds after this method is called")
    @CrossOrigin(origins="*")
    @GetMapping("/crash")
    public String crash() {
        crasher.crashIt();
        return "OK";
    }
}