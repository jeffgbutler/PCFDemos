package com.jgb.loancalculator;

import com.jgb.loancalculator.service.CounterService;
import com.jgb.loancalculator.service.CounterServiceInMemory;
import com.jgb.loancalculator.service.Crasher;
import com.jgb.loancalculator.service.PaymentCalculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
    
    @Bean
    public PaymentCalculator paymentCalulator() {
        return new PaymentCalculator();
    }
    
    @Bean
    public Crasher crasher() {
        return new Crasher();
    }

    @Bean
    @Profile("!cloud")
    public CounterService counterService() {
        return new CounterServiceInMemory();
    }
}
