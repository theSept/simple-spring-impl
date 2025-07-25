package com.liuxu.example.test;

import com.liuxu.springframework.beans.annotion.Component;
import com.liuxu.springframework.beans.annotion.Priority;

/**
 * @date: 2025-07-21
 * @author: liuxu
 */
@Priority(10)
// @Primary
@Component
public class Service01Impl01 implements Service01 {
    @Override
    public void print() {
        System.out.println("Service01Impl01");
    }
}
