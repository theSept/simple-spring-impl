package com.liuxu.springframework.beans.annotion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识销毁方法的注解
 *
 *
 * @date: 2025-07-17
 * @author: liuxu
 */
@Retention(RetentionPolicy.RUNTIME) // 如果不添加这两个元注解：编译器不会将注解信息写入 .class 文件JVM 在运行时无法通过反射读取到该注解
@Target(ElementType.METHOD)
public @interface PreDestroy {
}
