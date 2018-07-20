package com.jgb.loancalculator.http;

import java.math.BigDecimal;

import com.jgb.loancalculator.service.CounterService;
import com.jgb.loancalculator.service.PaymentCalculator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;

@RestController
public class PaymentController {
    
    @Autowired
    private CounterService counterService;
    
    @Autowired
    private PaymentCalculator paymentCalculator;
    
    @ApiOperation("Calculate a loan payment")
    @CrossOrigin(origins="*")
    @GetMapping("/payment")
    public ReturnValue calculatePayment(@RequestParam("amount") double amount,
            @RequestParam("rate") double rate,
            @RequestParam("years") int years) {

        BigDecimal payment = paymentCalculator.calculate(amount, rate, years);
        
        return new ReturnValue.Builder()
                .withAmount(amount)
                .withRate(rate)
                .withYears(years)
                .withPayment(payment)
                .withCount(counterService.incrementCounter())
                .build();
    }
}