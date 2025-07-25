package com.liuxu.example.test;

import com.liuxu.springframework.beans.annotion.Autowired;
import com.liuxu.springframework.beans.annotion.Component;
import com.liuxu.springframework.beans.annotion.Qualifier;

import java.util.Optional;

/**
 * @date: 2025-07-21
 * @author: liuxu
 */
@Component
public class Bean5MultiBean {

    @Qualifier("service01Impl02")
    @Autowired
    private Service01 service01;

    public void print() {
        Optional.ofNullable(service01).ifPresent(Service01::print);
    }

}
