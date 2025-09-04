package com.liuxu.example.testaop;

import com.liuxu.springframework.beans.annotion.Autowired;
import com.liuxu.springframework.beans.annotion.Component;

/**
 * @date: 2025-09-04
 * @author: liuxu
 */
@Component
public class BeanAop3 {

    @Autowired
    private BeanAop2 beanAop2;

    public void foo03() {
        System.out.println("beanAop03 foo03()....");
    }

    public int testCycle(int i, int b) {
        return beanAop2.testBeanAop1(i, b);
    }

    public BeanAop2 getBeanAop2() {
        return beanAop2;
    }
}
