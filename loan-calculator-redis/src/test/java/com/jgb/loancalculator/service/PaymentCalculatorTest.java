package com.jgb.loancalculator.service;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

import com.jgb.loancalculator.service.PaymentCalculator;

public class PaymentCalculatorTest {
    @Test
    public void testCalculatorHappyPath() {
        PaymentCalculator pc = new PaymentCalculator();

        BigDecimal c = pc.calculate(200000.0, 6.5, 30);

        assertEquals(new BigDecimal("1264.14"), c);
    }

    @Test
    public void testCalculatorZeroRate() {
        PaymentCalculator pc = new PaymentCalculator();

        BigDecimal c = pc.calculate(180000.0, 0.0, 30);

        assertEquals(new BigDecimal("500.00"), c);
    }
}
