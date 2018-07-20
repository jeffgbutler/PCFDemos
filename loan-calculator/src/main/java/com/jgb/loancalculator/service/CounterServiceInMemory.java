package com.jgb.loancalculator.service;

public class CounterServiceInMemory implements CounterService {

    private long count;
    
    @Override
    public long incrementCounter() {
        return ++count;
    }

    @Override
    public void resetCount() {
        count = 0;
    }
}
