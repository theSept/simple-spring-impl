package com.liuxu.springframework.beans.destroy;

/**
 * 销毁扩展接口
 *
 * @date: 2025-07-17
 * @author: liuxu
 */
public interface DisposableBean {

    /**
     * 在 {@code BeanFactory} 销毁 bean 时调用
     *
     * @throws Exception
     */
    void destroy() throws Exception;
}
