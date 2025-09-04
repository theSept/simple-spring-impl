package com.liuxu.example.testaop;

import com.liuxu.springframework.beans.annotion.Autowired;
import com.liuxu.springframework.beans.annotion.Component;

/**
 * @date: 2025-08-28
 * @author: liuxu
 */
@Component
public class BeanAop1 implements BeanAopSupport {


    @Autowired
    private BeanAop3 beanAop3;

    @Override
    public void foo() {
        System.out.println("target foo()....");
        // int i = 1 / 0;
    }

    @Override
    public int foo2Sum(int i1, int i2) {
        return i1 + i2;
    }

    @Override
    public BeanAop3 getBeanAop3() {
        return this.beanAop3;
    }

}
