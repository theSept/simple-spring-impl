package com.liuxu.springframework.beans.interfaces;

/**
 * 对象工厂，
 * - 用于三级缓存
 *
 * @date: 2025-06-20
 * @author: liuxu
 */
@FunctionalInterface
public interface ObjectFactory<T> {

    T getObject();

}
