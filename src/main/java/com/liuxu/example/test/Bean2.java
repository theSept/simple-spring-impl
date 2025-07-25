package com.liuxu.example.test;

import com.liuxu.springframework.beans.annotion.Autowired;
import com.liuxu.springframework.beans.annotion.Component;

/**
 * @date: 2025-07-01
 * @author: liuxu
 */
@Component
public class Bean2 {

    @Autowired(required = false)
    private Bean1 bean1;

    public String getName() {
        return "My Bean2";
    }
}
