package com.liuxu.springframework.aop.autoproxy;

import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.aspectj.annotation.BeanFactoryAspectJAdvisorsBuilder;
import com.liuxu.springframework.aop.interceptor.ExposeInvocationInterceptor;
import com.liuxu.springframework.aop.utils.AspectJProxyUtils;
import com.liuxu.springframework.beans.DefaultListableBeanFactory;
import com.liuxu.springframework.beans.interfaces.BeanFactory;

import java.util.List;

/**
 * 注解处理AspectJ 切面，并自动代理创建。
 * <p>
 * 处理当前应用程序上下文中所有 AspectJ 注解方面的子类，以及 Spring Advisors。
 * 任何 AspectJ 注解类都将被自动识别，如果 Spring AOP 的基于代理的模型能够应用它，则应用它们的建议。这涵盖了方法执行连
 *
 * @date: 2025-08-17
 * @author: liuxu
 */
public class AnnotationAwareAspectJAutoProxyCreator extends AbstractAutoProxyCreator {

    //  用于从Bean容器中检索构建AspectJ切面
    private BeanFactoryAspectJAdvisorsBuilder aspectJAdvisorsBuilder;


    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);
        this.aspectJAdvisorsBuilder = new BeanFactoryAspectJAdvisorsBuilder((DefaultListableBeanFactory) beanFactory);
    }

    @Override
    protected List<Advisor> findCandidateAdvisors() {
        // 从父类的此方法获取手工代码编写的切面配置
        List<Advisor> advisors = super.findCandidateAdvisors();

        if (this.aspectJAdvisorsBuilder != null) {
            try {
                // 处理注解方式配置的切面
                advisors.addAll(this.aspectJAdvisorsBuilder.buildAspectJAdvisors());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return advisors;
    }

    /**
     * 扩展切面列表
     * <p>
     * 如果存在AspectJ切面，如果存在会尝试在切面列表头添加 {@link ExposeInvocationInterceptor#ADVISOR} 作为切面链头，
     * 用于在线程上下文中暴露当前的方法调用器.
     *
     * @param candidateAdvisors 候选切面
     */
    protected void extendAdvisors(List<Advisor> candidateAdvisors) {
        // 检查是否存在AspectJ切面，如果存在会尝试在切面列表头添加 ExposeInvocationInterceptor.ADVISOR，
        // 作为切面链头，用于在线程上下文中存放当前的方法调用器
        AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(candidateAdvisors);
    }


    /**
     * 切面是否已经预先进行了类级别匹配
     *
     * @return 当前类已完成类级别匹配
     */
    protected boolean advisorsPreFiltered() {
        return true;
    }

}
