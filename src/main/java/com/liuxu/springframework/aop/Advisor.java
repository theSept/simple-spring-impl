package com.liuxu.springframework.aop;

/**
 * 切面顶级接口
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public interface Advisor {

    Advice EMPTY_ADVICE = new Advice() {
    };

    /**
     * 返回当前切面的的通知信息
     *
     * @return 通知信息
     */
    Advice getAdvice();

}
