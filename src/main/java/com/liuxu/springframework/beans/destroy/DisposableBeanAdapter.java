package com.liuxu.springframework.beans.destroy;

import com.liuxu.springframework.beans.beandefinition.AbstractBeanDefinition;
import com.liuxu.springframework.beans.beandefinition.RootBeanDefinition;
import com.liuxu.springframework.beans.interfaces.DestructionAwareBeanPostProcessor;
import com.liuxu.springframework.utils.BeanUtils;
import com.liuxu.springframework.utils.ReflectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 适配类，适配销毁bean的各种销毁方法
 *
 * @date: 2025-07-17
 * @author: liuxu
 */
public class DisposableBeanAdapter implements DisposableBean {

    private static final String DESTROY_METHOD_NAME = "destroy";

    private static final String CLOSE_METHOD_NAME = "close";

    private static final String SHUTDOWN_METHOD_NAME = "shutdown";
    private static final Logger log = LoggerFactory.getLogger(DisposableBeanAdapter.class);

    private final Object bean;

    private final String beanName;

    // 是否实现 DisposableBean 接口，将会调用 destroy() 方法
    private final boolean invokeDisposableBean;

    // 是否实现 AutoCloseable 接口，将会调用 close() 方法
    private boolean invokeAutoCloseable;

    // 销毁方法名称列表
    private String[] destroyMethodNames;

    // 销毁方法实例列表
    private transient Method[] destroyMethods;

    private List<DestructionAwareBeanPostProcessor> beanPostProcessors;


    public DisposableBeanAdapter(Object bean, String beanName, RootBeanDefinition beanDefinition,
                                 List<DestructionAwareBeanPostProcessor> postProcessors) {
        this.bean = bean;
        this.beanName = beanName;
        this.invokeDisposableBean = (bean instanceof DisposableBean) &&
                !beanDefinition.isExternallyManagedDestroyMethods(DESTROY_METHOD_NAME); // 如果该方法有 @PreDestroy 注释，不会再重复调用。


        // 推断销毁方法
        String[] destroyMethodNames = inferDestroyMethodsIfNecessary(bean.getClass(), beanDefinition);

        /*
		处理推断出的结果：
		    1.
		    如果有推断出销毁方法
		    并且 (bean未实现DisposableBean接口 且 推断的方法名称不是destroy)
		    并且 (推断的方法不是外部管理 (和 @PreDestroy 注解) 的方法)

		    2.才进行判断 (bean 未实现AutoCloseable接口 且 方法名称不是close)

			3.才进行推断方法处理：
				拿到bean中该名称相同的情况下方法参数列表最少的，并且最大不能超过1个参数的方法，作为销毁方法

		-----
		 */
        if (!ObjectUtils.isEmpty(destroyMethodNames) &&
                !(this.invokeDisposableBean && DESTROY_METHOD_NAME.equals(destroyMethodNames[0])) &&
                !beanDefinition.isExternallyManagedDestroyMethods(destroyMethodNames[0])) {

            this.invokeAutoCloseable = (AutoCloseable.class.isAssignableFrom(bean.getClass())
                    && CLOSE_METHOD_NAME.equals(destroyMethodNames[0]));
            if (!invokeDisposableBean) {
                // 处理推断方法
                this.destroyMethodNames = destroyMethodNames;
                ArrayList<Method> methodArrayList = new ArrayList<>(destroyMethodNames.length);
                for (String destroyMethodName : destroyMethodNames) {

                    /* 根据方法名匹配销毁方法实例 ，并校验方法的合法性 */
                    Method destroyMethod = determineDestroyMethod(destroyMethodName);
                    if (destroyMethod == null) {
                        // 不存在销毁方法，如果是必须要存在销毁方法，这块则抛出异常
                        return;
                    } else {

                        // 校验方法参数
                        if (destroyMethod.getParameterCount() > 0) {
                            Class<?>[] parameterTypes = destroyMethod.getParameterTypes();
                            if (parameterTypes.length > 1) {
                                throw new IllegalArgumentException("无效的销毁方法: " + destroyMethodName + ". 在 bean: " + beanName + " 中发现：该方法接受多个参数");
                            } else if (parameterTypes.length == 1 && parameterTypes[0] != boolean.class) {
                                throw new IllegalArgumentException("无效的销毁方法: " + destroyMethodName + ". 在 bean: " + beanName + " 中发现：该方法接受一个参数，但该参数不是 boolean");
                            }
                        }
                        // TODO 待AOP代理后再回来处理，Spring中此处会尝试去找接口中同名的方法。

                        methodArrayList.add(destroyMethod);
                    }
                }

                this.destroyMethods = methodArrayList.toArray(new Method[]{});
            }
        }

        // 过滤出执行该 bean 销毁的后处理器
        this.beanPostProcessors = filterPostProcessors(postProcessors, bean);

    }

    private DisposableBeanAdapter(Object bean, String beanName, boolean invokeDisposableBean, boolean invokeAutoCloseable) {
        this.bean = bean;
        this.beanName = beanName;
        this.invokeDisposableBean = invokeDisposableBean;
        this.invokeAutoCloseable = invokeAutoCloseable;
    }

    /**
     * 根据指定的销毁方法名称，确定最终的销毁方法，并返回销毁方法对象
     *
     * @param destroyMethodName 销毁方法名称
     * @return 销毁方法实例 或 null
     */
    private Method determineDestroyMethod(String destroyMethodName) {

        // 处理销毁方法名称，创建方法描述符类
        MethodDescriptor descriptor = MethodDescriptor.create(this.beanName, this.bean.getClass(), destroyMethodName);
        // 声明的类
        Class<?> declaringClass = descriptor.declaringClass();
        // 方法的名称
        String methodName = descriptor.methodName();

        try {
            // 拿到方法实例
            Method destroyMethod = findDestroyMethod(declaringClass, methodName);
            if (destroyMethod != null) {
                return destroyMethod;
            }

            return null;
        } catch (NoSuchMethodException e) {
            log.error("methodName: {} 获取销毁方法失败.", methodName);
            throw new RuntimeException("methodName:" + methodName + " 获取销毁方法失败", e);
        }

    }

    /**
     * 从指定类中获取销毁方法
     *
     * @param declaringClass 声明的类
     * @param methodName     方法名称
     * @return 销毁方法实
     */
    private Method findDestroyMethod(Class<?> declaringClass, String methodName) throws NoSuchMethodException {
        return BeanUtils.findMethodWithMinimalParameters(declaringClass, methodName);
    }

    @Override
    public void destroy() throws Exception {
        /*
			最终销毁方法入口，销毁方法执行顺序：
			1. 先执行 @PreDestroy 注解的方法，由后处理器 DestructionAwareBeanPostProcessor.postProcessBeforeDestruction() 反射调用
			2. 执行 DisposableBean 接口 destroy() 方法（如果存在）
			3. ① 如果实现 AutoCloseable 接口，执行 destroy() 方法 ,
			   ② 否则，检查配置或推断的销毁方法实例，存在则执行，
			   ③ 否则，检查配置或推断的方法名称，再次尝试通过方法名获取方法实例，如果拿到方法实例则将其作为销毁方法执行..
     	*/
        if (this.beanPostProcessors != null && !this.beanPostProcessors.isEmpty()) {
            for (DestructionAwareBeanPostProcessor beanPostProcessor : this.beanPostProcessors) {
                beanPostProcessor.postProcessBeforeDestruction(this.bean, this.beanName);
            }
        }

        if (this.invokeDisposableBean) {

            try {
                ((DisposableBean) this.bean).destroy();
            } catch (Exception e) {
                log.error("BeanName: {} 销毁方法 DisposableBean.destroy() 执行出现错误 {}", this.beanName, e.getMessage());
                throw new RuntimeException(e);
            }
        }

        if (this.invokeAutoCloseable) {
            try {
                ((AutoCloseable) this.bean).close();
            } catch (Exception e) {
                log.error("BeanName: {} 销毁方法 AutoCloseable.close() 执行出现错误 {}", this.beanName, e.getMessage());
                throw new RuntimeException(e);
            }
        } else if (this.destroyMethods != null) {
            for (Method destroyMethod : destroyMethods) {
                invokeCustomDestroyMethod(destroyMethod);
            }
        } else if (this.destroyMethodNames != null) {
            for (String destroyMethodName : destroyMethodNames) {
                Method destroyMethod = determineDestroyMethod(destroyMethodName);
                if (destroyMethod != null) {
                    // TODO 待AOP代理后再回来处理，Spring中此处会尝试去找接口中同名的方法进行调用。
                }
            }
        }

    }

    /**
     * 调用自定义销毁方法
     *
     * @param destroyMethod 销毁方法实例
     */
    private void invokeCustomDestroyMethod(Method destroyMethod) {
        int parameterCount = destroyMethod.getParameterCount();
        Object[] objects = new Object[parameterCount];
        if (parameterCount == 1) {
            objects[0] = Boolean.TRUE;
        }
        try {
            ReflectionUtils.makeAccessible(destroyMethod);
            destroyMethod.invoke(this.bean, objects);
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.error("BeanName: {} 销毁方法 {} 执行出现错误 {}", this.beanName, destroyMethod.getName(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 过滤掉不需要执行指定bean销毁的 BeanPostProcessor
     *
     * @param processors BeanPostProcessor 列表
     * @param bean       bean 实例
     * @return BeanPostProcessor 列表
     */
    private static List<DestructionAwareBeanPostProcessor> filterPostProcessors(
            List<DestructionAwareBeanPostProcessor> processors, Object bean) {

        List<DestructionAwareBeanPostProcessor> postProcessors = null;

        if (!processors.isEmpty()) {
            postProcessors = new ArrayList<>(processors.size());
            for (DestructionAwareBeanPostProcessor postProcessor : processors) {
                if (postProcessor.requiresDestruction(bean)) {
                    postProcessors.add(postProcessor);
                }
            }
        }

        return processors;
    }


    /**
     * 推断指定类中的销毁方法
     *
     * @param target         目标类
     * @param beanDefinition BeanDefinition
     * @return 销毁方法名称列表 或 {@code null}（如果不存在）
     */
    static String[] inferDestroyMethodsIfNecessary(Class<?> target, RootBeanDefinition beanDefinition) {
        // 如果 Bean 显示配置值 destroyMethod ，并且配置的销毁方法有两个或两个以上，则使用该值。例如：<bean destroyMethod="close1,close2">
        String[] destroyMethodNames = beanDefinition.getDestroyMethodNames();
        if (destroyMethodNames != null && destroyMethodNames.length > 1) {
            return destroyMethodNames;
        }


        /*
			理解：@Bean destroyMethod 默认销毁方法是 "(inferred)"
				如果没实现 DisposableBean 接口，也没实现AutoCloseable接口，
				则会进行推断检查将 bean 中名为 destroy 或 shutdown 的方法作为销毁方法

			简单的说：默认未显示配置销毁方法，如果没有实现 DisposableBean 接口，也没有实现 AutoCloseable 接口，则会尝试查找名为 destroy 或 shutdown 的方法

		---------------------------
		 	尝试推断：
				拿到第一个销毁方法 （例如 @Bean(destroyMethod="clean")）
				如果 没有配置destroyMethod 或 (销毁的方法是null 且 bean实现AutoCloseable接口)，则进行进行后面步骤
				置空销毁，如果 bean 实现 DisposableBean，则直接结束，只在没有实现DisposableBean接口的情况下进行推理：
				推理，再如果 bean 实现AutoCloseable接口，则取 close() 方法
				尝试查找名为 destroy 或 shutdown 的方法
		 */
        // 尝试从缓存获取销毁方法
        String destroyMethodName = beanDefinition.resolvedDestroyMethodName;
        if (destroyMethodName == null) {
            destroyMethodName = beanDefinition.getDestroyMethod();
            boolean isAutoCloseable = AutoCloseable.class.isAssignableFrom(target);
            if (AbstractBeanDefinition.INFER_METHOD.equals(destroyMethodName) ||
                    (destroyMethodName == null && isAutoCloseable)) {

                destroyMethodName = null;
                // 在没有实现 DisposableBean 接口，进行销毁方法推理
                if (!(DisposableBean.class.isAssignableFrom(target))) {
                    if (isAutoCloseable) {
                        // 如果实现了 AutoCloseable 接口，则取 close 方法。
                        destroyMethodName = CLOSE_METHOD_NAME;
                    } else {
                        /* 既没有实现DisposableBean也没有实现AutoCloseable. 尝试获取 close 和 shutdown 方法作为销毁方法*/
                        try {
                            destroyMethodName = target.getMethod(CLOSE_METHOD_NAME).getName();
                        } catch (NoSuchMethodException e) {
                            try {
                                destroyMethodName = target.getMethod(SHUTDOWN_METHOD_NAME).getName();
                            } catch (NoSuchMethodException ex) {
                                // 没有匹配的销毁方法
                            }
                        }
                    }
                }
            }
            // 缓存推理的方法
            beanDefinition.resolvedDestroyMethodName = (destroyMethodName != null ? destroyMethodName : "");
        }

        return (StringUtils.isNotBlank(destroyMethodName) ? new String[]{destroyMethodName} : null);
    }


    /**
     * 校验指定bean是否包含销毁方法
     *
     * @param bean           bean实例
     * @param beanDefinition BeanDefinition
     * @return true：包含销毁方法
     */
    public static boolean hasDestroyMethod(Object bean, RootBeanDefinition beanDefinition) {
        return (bean instanceof DisposableBean) ||
                inferDestroyMethodsIfNecessary(bean.getClass(), beanDefinition) != null;
    }

    /**
     * 校验后处理器列表中是否有匹配指定 bean 的后处理器
     *
     * @param bean           bean 实例
     * @param postProcessors 后处理器列表
     * @return true：有匹配的后处理器
     */
    public static boolean hasApplicableProcessors(Object bean, List<DestructionAwareBeanPostProcessor> postProcessors) {
        if (postProcessors != null && !postProcessors.isEmpty()) {
            for (DestructionAwareBeanPostProcessor postProcessor : postProcessors) {
                if (postProcessor.requiresDestruction(bean)) {
                    return true;
                }
            }
        }
        return false;
    }

}
