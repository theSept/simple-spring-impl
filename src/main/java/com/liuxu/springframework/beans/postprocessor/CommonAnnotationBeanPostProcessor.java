package com.liuxu.springframework.beans.postprocessor;

import com.liuxu.springframework.beans.annotion.PostConstruct;
import com.liuxu.springframework.beans.annotion.PreDestroy;

/**
 * 常用的注解后处理器
 *
 * @date: 2025-07-17
 * @author: liuxu
 */
public class CommonAnnotationBeanPostProcessor extends InitDestroyAnnotationBeanPostProcessor {

    public CommonAnnotationBeanPostProcessor() {
        // 设置标识初始化、销毁方法的注解
        addInitAnnotationTypes(PostConstruct.class);
        addDestroyAnnotationTypes(PreDestroy.class);
    }
}
