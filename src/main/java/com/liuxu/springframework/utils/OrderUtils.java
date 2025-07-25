package com.liuxu.springframework.utils;

import com.liuxu.springframework.beans.annotion.Priority;

import java.util.Optional;

/**
 * 排序工具类
 *
 * @date: 2025-07-05
 * @author: liuxu
 */
public abstract class OrderUtils {

    /**
     * 获取类型中的 {@link com.liuxu.springframework.beans.annotion.Priority}
     *
     * @param type 类型
     * @return 优先级 或者 null(如果不存在)
     */
    public static Integer getPriority(Class<?> type) {
        return Optional.ofNullable(type.getAnnotation(Priority.class)).map(Priority::value).orElse(null);
    }

}
