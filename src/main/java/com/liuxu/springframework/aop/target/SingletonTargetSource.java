package com.liuxu.springframework.aop.target;

import com.liuxu.springframework.aop.TargetSource;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 单列的目标对象
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public class SingletonTargetSource implements TargetSource {
    private final Object target;

    public SingletonTargetSource(Object target) {
        this.target = target;
    }

    @Override
    public Class<?> getTargetClass() {
        return (this.target != null ? this.target.getClass() : null);
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public Object getTarget() throws Exception {
        return this.target;
    }

    @Override
    public void releaseTarget(Object target) throws Exception {

    }

    @Override
    public boolean equals(Object obj) {
        return (this.target == obj || (obj instanceof SingletonTargetSource targetSource &&
                this.target.equals(targetSource.target)));
    }

    @Override
    public String toString() {
        return "SingletonTargetSource for target object [" + ObjectUtils.identityToString(this.target) + "]";
    }

    @Override
    public int hashCode() {
        return this.target.hashCode();
    }


}
