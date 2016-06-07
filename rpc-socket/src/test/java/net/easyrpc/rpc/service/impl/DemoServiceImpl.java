package net.easyrpc.rpc.service.impl;

import net.easyrpc.rpc.service.api.DemoService;
import org.springframework.stereotype.Service;

@Service
public class DemoServiceImpl implements DemoService {

    @Override
    public Integer add(Integer a, Integer b) {
        return a + b;
    }

    @Override
    public Integer min(Integer a, Integer b) {
        return a - b;
    }

    @Override
    public Float div(Integer a, Integer b) {
        return (float) a / b;
    }

    @Override
    public Integer mul(Integer a, Integer b) {
        return a * b;
    }

    @Override
    public Integer pow(Integer a, Integer b) {
        return (int) Math.pow(a, b);
    }

}