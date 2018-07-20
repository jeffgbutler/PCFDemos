package com.jgb.loancalculator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PaymentCalculator {
    public BigDecimal calculate(double amount, double rate, int years) {
        if (rate == 0.0) {
            return calculateWithoutInterest(amount, years);
        } else {
            return calculateWithInterest(amount, rate, years);
        }
    }

    private BigDecimal calculateWithInterest(double amount, double rate, int years) {
        double monthlyRate = rate / 100.0 / 12.0;
        int numberOfPayments = years * 12;
        double payment = (monthlyRate * amount) / (1.0 - Math.pow(1.0 + monthlyRate, -numberOfPayments));
        return toMoney(payment);
    }

    private BigDecimal calculateWithoutInterest(double amount, int years) {
        int numberOfPayments = years * 12;
        return toMoney(amount / numberOfPayments);
    }
    
    private BigDecimal toMoney(double d) {
        BigDecimal bd = new BigDecimal(d);
        return bd.setScale(2, RoundingMode.HALF_UP);
    }
}