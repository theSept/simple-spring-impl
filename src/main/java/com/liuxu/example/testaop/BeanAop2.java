package com.liuxu.example.testaop;

import com.liuxu.springframework.beans.annotion.Autowired;
import com.liuxu.springframework.beans.annotion.Component;
import com.liuxu.springframework.beans.annotion.Qualifier;

/**
 * @date: 2025-08-28
 * @author: liuxu
 */
@Component
public class BeanAop2 {

    @Qualifier("beanAop1")
    @Autowired
    private BeanAopSupport beanAop1;

    public void foo() {
        System.out.println("beanAop2 Target foo()....");
    }

    public String foo2(int num, String name) {
        System.out.println("beanAop2 Target foo2()....");
        return num + name;
    }

    public void fooEx() {
        System.out.println("beanAop2 Target fooEx()....");
        int i = 1 / 0;
    }


    // 测试循环依赖
    public int testBeanAop1(int i, int b) {
        return beanAop1.foo2Sum(i, b);
    }

    public BeanAopSupport getBeanAop1() {
        return this.beanAop1;
    }

}
