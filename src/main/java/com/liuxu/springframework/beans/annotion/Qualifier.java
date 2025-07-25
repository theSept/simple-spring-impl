package com.liuxu.springframework.beans.annotion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 指定注入的beanName
 * - 当使用 {@link Autowired} 注解依赖注入时，可使用该注解指定 beanName
 *
 * @date: 2025-07-05
 * @author: liuxu
 */
@Target(ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Qualifier {

    /**
     * 需要注入的 beanName
     */
    String value();

}
