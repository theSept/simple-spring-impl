package com.liuxu.springframework.aop.aspectj.annotation;

import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.aspectj.instance.BeanFactoryAspectInstanceFactory;
import com.liuxu.springframework.aop.aspectj.instance.MetadataAwareAspectInstanceFactory;
import com.liuxu.springframework.beans.DefaultListableBeanFactory;
import com.liuxu.springframework.utils.BeanFactoryUtils;
import org.aspectj.lang.reflect.PerClauseKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于从 BeanFactory 检索 @AspectJ Bean 并基于它们构建 Spring Advisors 的代理，以用于自动代理
 *
 * @date: 2025-08-14
 * @author: liuxu
 */
public class BeanFactoryAspectJAdvisorsBuilder {
    private static final Logger log = LoggerFactory.getLogger(BeanFactoryAspectJAdvisorsBuilder.class);


    private final DefaultListableBeanFactory beanFactory;

    // 将 Aspectj 类中的通知转换为 Advisor 的工厂
    private final AspectJAdvisorFactory advisorFactory;

    // 有 @Aspect 注解的Bean 名称
    private volatile List<String> aspectBeanNames;


    // 缓存切面类中的所有切面方法实例
    private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

    // 缓存切面类对应的元数据工厂
    private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();


    public BeanFactoryAspectJAdvisorsBuilder(DefaultListableBeanFactory beanFactory) {
        this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
    }

    public BeanFactoryAspectJAdvisorsBuilder(DefaultListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
        this.advisorFactory = advisorFactory;
        this.beanFactory = beanFactory;
    }


    public List<Advisor> buildAspectJAdvisors() throws Exception {
        List<String> aspectNames = aspectBeanNames;

        if (aspectNames == null) {
            // 没有缓存 aspectJ 类的名称，需要手动查找
            synchronized (this) {
                aspectNames = aspectBeanNames;
                if (aspectNames == null) {
                    // 通知
                    List<Advisor> advisors = new ArrayList<>();
                    aspectNames = new ArrayList<>();

                    // 拿到所有的BeanName,筛选出是 Aspect 的切面类
                    String[] beanNames = BeanFactoryUtils.getBeanDefinitionNames(beanFactory);
                    for (String beanName : beanNames) {
                        if (!isEligibleBean(beanName)) {
                            continue;
                        }

                        // 校验类型是否是切面类
                        Class<?> beanType = this.beanFactory.getType(beanName);
                        if (beanType == null) {
                            continue;
                        }

                        if (this.advisorFactory.isAspect(beanType)) {
                            try {
                                AspectMetadata aspectMetadata = new AspectMetadata(beanName, beanType);
                                // AspectJ 切面的 实例化模型(默认单例)，控制切面类的生命周期
                                if (aspectMetadata.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
                                    // AspectJ 切面类实例工厂
                                    BeanFactoryAspectInstanceFactory aspectInstanceFactory = new BeanFactoryAspectInstanceFactory(beanName, this.beanFactory);
                                    // 解析切面类，组装成的SpringAOP方法切面
                                    List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(aspectInstanceFactory);
                                    if (this.beanFactory.isSingleton(beanName)) {
                                        // 单例直接缓存切面方法实例
                                        this.advisorsCache.put(beanName, classAdvisors);
                                    } else {
                                        this.aspectFactoryCache.put(beanName, aspectInstanceFactory);
                                    }

                                    advisors.addAll(classAdvisors);
                                } else {
                                    // AspectJ切面类的实例模型不是单例场景....
                                    // 暂时省略不处理...
                                }

                                aspectNames.add(beanName);
                            } catch (Exception e) {
                                log.error("初始化切面类失败", e);
                            }
                        }
                    }

                    this.aspectBeanNames = aspectNames;
                    return advisors;
                }
            }
        }

        if (aspectNames.isEmpty()) {
            return Collections.emptyList();
        }

        // 缓存了AspectJ切面类的名称，从缓存获取切面方法
        List<Advisor> advisors = new ArrayList<>();
        for (String aspectName : aspectNames) {
            List<Advisor> cache = this.advisorsCache.get(aspectName);
            if (cache == null) {
                // 不是单例，不会缓存切面方法实例，需要根据切面类实例工厂重新解析出切面方法
                MetadataAwareAspectInstanceFactory aspectInstanceFactory = this.aspectFactoryCache.get(aspectName);
                advisors.addAll(this.advisorFactory.getAdvisors(aspectInstanceFactory));
            } else {
                advisors.addAll(cache);
            }
        }
        return advisors;

    }


    /**
     * 返回具有给定名称的切面 Bean 是否符合条件.
     *
     * @param beanName aspect bean 的名称
     * @return bean 是否符合条件
     */
    protected boolean isEligibleBean(String beanName) {
        return true;
    }

}
