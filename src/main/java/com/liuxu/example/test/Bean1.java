package com.liuxu.example.test;

import com.liuxu.springframework.beans.annotion.Autowired;
import com.liuxu.springframework.beans.annotion.Component;

/**
 * @date: 2025-07-01
 * @author: liuxu
 */
@Component()
public class Bean1 {

    @Autowired
    private Bean2 bean2;

    public String getName() {
        return "My Bean1";
    }
}
