package com.jgb.loancalculator.http;

import com.jgb.loancalculator.service.CounterService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResetCountController {

    @Autowired
    private CounterService counterService;
    
    @CrossOrigin(origins="*")
    @GetMapping("/resetCount")
    public String resetCount() {
        counterService.resetCount();
        return "OK";
    }
}