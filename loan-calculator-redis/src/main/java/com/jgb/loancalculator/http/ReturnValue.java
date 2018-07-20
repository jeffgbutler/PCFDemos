package com.jgb.loancalculator.http;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

public class ReturnValue {
    private double amount;
    private double rate;
    private int years;
    private BigDecimal payment;
    private String instance;
    private Long count;
    
    private ReturnValue(Builder builder) {
        this.amount = builder.amount;
        this.rate = builder.rate;
        this.years = builder.years;
        this.payment = builder.payment;
        this.count = builder.count;
        this.instance = System.getenv("CF_INSTANCE_INDEX");
    }

    public BigDecimal getPayment() {
        return payment;
    }

    public String getInstance() {
        return instance;
    }
    
    public double getAmount() {
        return amount;
    }

    public double getRate() {
        return rate;
    }

    public int getYears() {
        return years;
    }

    public Long getCount() {
        return count;
    }
    
    @JsonIgnoreType
    public static class Builder {
        private double amount;
        private double rate;
        private int years;
        private BigDecimal payment;
        private Long count;

        public Builder withAmount(double amount) {
            this.amount = amount;
            return this;
        }
        
        public Builder withRate(double rate) {
            this.rate = rate;
            return this;
        }
        
        public Builder withYears(int years) {
            this.years = years;
            return this;
        }
        
        public Builder withPayment(BigDecimal payment) {
            this.payment = payment;
            return this;
        }
        
        public Builder withCount(Long count) {
            this.count = count;
            return this;
        }
        
        public ReturnValue build() {
            return new ReturnValue(this);
        }
    }
}
