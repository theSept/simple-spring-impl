package com.liuxu.springframework.utils;

import com.liuxu.springframework.beans.DefaultListableBeanFactory;
import com.liuxu.springframework.beans.interfaces.Aware;
import com.liuxu.springframework.beans.interfaces.BeanFactory;
import com.liuxu.springframework.beans.interfaces.BeanFactoryAware;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.apache.commons.lang3.ObjectUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @date: 2025-06-20
 * @author: liuxu
 */
public abstract class ClassUtils {

    /** 公共修饰符和受保护修饰符组合的预计算值。. */
    private static final int OVERRIDABLE_MODIFIER = Modifier.PUBLIC | Modifier.PROTECTED;


    /** 空类数组 */
    private static final Class<?>[] EMPTY_CLASS_ARRAY = {};


    /**
     * 以原始包装器类型作为键，对应的原始类型作为值的映射，
     * 例如：Integer.class -> int.class
     */
    private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(9);

    /**
     * 以原始类型为键，对应的包装类型作为值，
     * 例如：int.class -> Integer.class
     */
    private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<>(9);


    static {
        primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
        primitiveWrapperTypeMap.put(Byte.class, byte.class);
        primitiveWrapperTypeMap.put(Character.class, char.class);
        primitiveWrapperTypeMap.put(Double.class, double.class);
        primitiveWrapperTypeMap.put(Float.class, float.class);
        primitiveWrapperTypeMap.put(Integer.class, int.class);
        primitiveWrapperTypeMap.put(Long.class, long.class);
        primitiveWrapperTypeMap.put(Short.class, short.class);
        primitiveWrapperTypeMap.put(Void.class, void.class);

        // Map entry iteration is less expensive to initialize than forEach with lambdas
        // 映射条目迭代的初始化成本低于使用 lambda 的 forEach
        for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
            primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
        }

    }

    /**
     * 查找指定包名下的所有类
     *
     * @param packageName 包名
     * @return 类列表
     */
    public static List<Class<?>> reflectionsFindClassByPath(String packageName) {
        try (ScanResult scan = new ClassGraph()
                .acceptPackages(packageName)  // 只扫描这个包及其子包
                .scan()) {
            return scan.getAllClasses().loadClasses();
        }
    }


    /**
     * 返回给定方法的限定名称，由完全限定的接口/类名 + “.” + 方法名组成。
     *
     * @param method 方法
     * @return 方法的限定名称
     */
    public static String getQualifiedMethodName(Method method) {
        return getQualifiedMethodName(method, null);
    }

    /**
     * 返回给定方法的限定名称，由完全限定的接口/类名 + “.” + 方法名组成。
     *
     * @param method 方法
     * @param clazz  方法所属的类
     * @return 方法的限定名称
     */
    public static String getQualifiedMethodName(Method method, Class<?> clazz) {
        return clazz != null ? clazz.toString() : method.getDeclaringClass().getName() + "." + method.getName();
    }


    /**
     * 根据类名获取Class
     *
     * @param className   类名
     * @param classLoader 类加载器
     * @return Class
     */
    public static Class<?> forName(String className, ClassLoader classLoader) throws ClassNotFoundException {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("class：" + className + " 加载类失败..");
        }

    }


    /**
     * 尝试根据字段名称和值获取常规的sett方法
     *
     * @param clazz     类
     * @param fieldName 字段名
     * @param value     字段值
     * @return sett方法或null
     */
    public static Method tryGetConventionSettMethodByFieldName(Class<?> clazz, String fieldName, Object value) {
        try {
            return clazz.getMethod(generateSettMethodName(fieldName), value.getClass());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 根据字段生成sett方法名称
     *
     * @param fieldName 字段名
     * @return sett方法名称
     */
    private static String generateSettMethodName(String fieldName) {
        // setProxyTargetClass
        return String.format("set%s%s", fieldName.substring(0, 1).toUpperCase(), fieldName.substring(1));
    }


    /**
     * 给定一个方法（可能来自接口）和当前反射调用中使用的目标类，如果有的话，请找到相应的目标方法
     * 从父类/接口的方法声明 → 找到目标类中最具体的实现
     *
     * @param method      给定的方法
     * @param targetClass 目标类
     * @return 具体实现的目标方法
     */
    public static Method getMostSpecificMethod(Method method, Class<?> targetClass) {
        if (targetClass != null && targetClass != method.getDeclaringClass() &&
                ((method.getModifiers() & OVERRIDABLE_MODIFIER) != 0 || !method.getDeclaringClass().isAssignableFrom(targetClass))) {

            try {
                if (Modifier.isPublic(method.getModifiers())) {
                    try {
                        return targetClass.getMethod(method.getName(), method.getParameterTypes());
                    } catch (NoSuchMethodException e) {
                        return method;
                    }
                } else {
                    Method specificMethod = ReflectionUtils.findMethod(targetClass, method.getName(), method.getParameterTypes());
                    return (specificMethod != null ? specificMethod : method);
                }
            } catch (SecurityException e) {
                // ....
            }
        }

        return method;

    }


    /**
     * 获取ClassLoader
     *
     * @return
     */
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = ClassUtils.class.getClassLoader();
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                }
            }
        }
        return cl;
    }


    /**
     * 确定给定类型是否可从给定值分配，假设通过反射进行设置。
     * 原始包装器类将会被视为可分配给相应的原始类型。
     *
     * @param type  目标类型
     * @param value 应该分配给目标类型的值
     * @return
     */
    public static boolean isAssignableValue(Class<?> type, Object value) {
        return (value != null ? isAssignable(type, value.getClass()) : !type.isPrimitive());
    }

    /**
     * 判断 lhsType 是否能接受 rhsType 类型的值
     *
     * @param lhsType 左侧类型
     * @param rhsType 右侧类型
     * @return 是否能接受
     */
    public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
        if (lhsType.isAssignableFrom(rhsType)) {
            return true;
        }

        // 是基元类型（也就是包装类型）
        if (lhsType.isPrimitive()) {
            // 拿到包装类
            Class<?> resolvedPrimitive = primitiveTypeToWrapperMap.get(rhsType);
            return lhsType == resolvedPrimitive;
        } else {
            // 不是基元类型。尝试拿到映射的基础类型，进行赋值比较
            Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
            return (resolvedWrapper != null && rhsType.isAssignableFrom(resolvedWrapper));
        }

    }


    /**
     * 为给定接口创建一个复合接口 Class，在一个 Class 中实现给定接口。
     * <p>此实现为给定接口构建一个 JDK 代理类。
     *
     * @param interfaces  要合并的接口
     * @param classLoader 用于来创建这个复合类的类加载器。
     * @return 合并接口的 Class
     */
    public static Class<?> createCompositeInterface(Class<?>[] interfaces, ClassLoader classLoader) {
        if (ObjectUtils.isEmpty(interfaces)) {
            throw new IllegalArgumentException("接口数组不得为空");
        }
        return Proxy.getProxyClass(classLoader, interfaces);
    }


    /**
     * 将集合转换成 Class 数组
     *
     * @param collection collection
     * @return Class[]
     */
    public static Class<?>[] toClassArray(Collection<Class<?>> collection) {
        return ObjectUtils.isEmpty(collection) ? EMPTY_CLASS_ARRAY : collection.toArray(EMPTY_CLASS_ARRAY);
    }


    /**
     * 获取指定类所有接口
     *
     * @param clazz 类
     * @return Class[]
     */
    public static Class<?>[] getAllInterfacesForClass(Class<?> clazz) {
        return toClassArray(getAllInterfacesForClassAsSet(clazz, null));
    }


    /**
     * 获取所有接口数组
     *
     * @param clazz       给定类
     * @param classLoader 类加载器
     * @return 接口数组
     */
    public static Class<?>[] getAllInterfacesForClass(Class<?> clazz, ClassLoader classLoader) {
        return toClassArray(getAllInterfacesForClassAsSet(clazz, classLoader));
    }

    /**
     * 返回给定类实现的所有接口 ，包括由超类实现的接口。
     * 如果类本身是一个接口，则它将作为唯一接口返回。
     *
     * @param clazz clazz
     * @return 所有接口
     */
    public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz) {
        return getAllInterfacesForClassAsSet(clazz, null);
    }


    /**
     * 返回给定类实现的所有接口 ，包括由超类实现的接口。
     * 如果类本身是一个接口，则它将作为唯一接口返回。
     *
     * @param clazz       类
     * @param classLoader 类加载器
     * @return 给定类实现的所有接口Set集合
     */
    public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz, ClassLoader classLoader) {

        if (clazz.isInterface() && isVisible(clazz, classLoader)) {
            return Collections.singleton(clazz);
        }
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        Class<?> current = clazz;
        while (current != null) {
            Class<?>[] ifcs = current.getInterfaces();
            for (Class<?> ifc : ifcs) {
                if (isVisible(ifc, classLoader)) {
                    interfaces.add(ifc);
                }
            }
            current = current.getSuperclass();
        }
        return interfaces;
    }


    /**
     * 检查给定的类在给定的 ClassLoader 中是否可见。
     *
     * @param clazz       要检查的类（通常是接口）
     * @param classLoader 要检查的 ClassLoader
     * @return 如果给定的类在给定的 ClassLoader 中可见，则为 true，否则为 false
     */
    public static boolean isVisible(Class<?> clazz, ClassLoader classLoader) {
        if (classLoader == null) {
            return true;
        }
        try {
            if (clazz.getClassLoader() == classLoader) {
                return true;
            }
        } catch (SecurityException ex) {
            // Fall through to loadable check below
        }

        // 检查给定的类是否可以在给定的 ClassLoader 中加载
        return isLoadable(clazz, classLoader);

    }

    /**
     * 检查给定的类是否可以在给定的 ClassLoader 中加载
     *
     * @param clazz       要检查的类
     * @param classLoader ClassLoader
     * @return 是否可以加载
     */
    private static boolean isLoadable(Class<?> clazz, ClassLoader classLoader) {
        try {
            return (clazz == classLoader.loadClass(clazz.getName()));
            // Else: different class with same name found
        } catch (ClassNotFoundException ex) {
            // No corresponding class found at all
            return false;
        }
    }


    /**
     * 获取指定类的实际类型<br/>
     * 指定的类可能是CGLIB的代理类，类的名字会有{@code $$} 标识，那就应该拿到它的父类
     *
     * @param clazz 类
     * @return 实际类型
     */
    public static Class<?> getUserClass(Class<?> clazz) {
        if (clazz.getName().contains("$$")) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return superclass;
            }
        }
        return clazz;

    }


    /**
     * 确定提供的 Class 是否是 JVM 为 lambda 表达式或方法引用生成的实现类。
     * 此方法根据适用于现代主流 JVM 的检查，尽最大努力尝试确定这一点。
     *
     * @param clazz
     * @return
     */
    public static boolean isLambdaClass(Class<?> clazz) {
        return (clazz.isSynthetic() && (clazz.getSuperclass() == Object.class) &&
                (clazz.getInterfaces().length > 0) && clazz.getName().contains("$$Lambda"));
    }


    /**
     * 实例化类,并指定类型
     *
     * @param clazz        需要实例化的类型
     * @param assignableTo 指定实例化后对象的类型
     * @param registry     bean工厂
     * @param <T>          泛型
     * @return 实例
     */
    public static <T> T instantiateClass(Class<?> clazz, Class<T> assignableTo, DefaultListableBeanFactory registry) {
        if (clazz.isInterface()) {
            throw new RuntimeException("Specified class is an interface");
        }

        if (!assignableTo.isAssignableFrom(clazz)) {
            throw new RuntimeException("Specified class is not assignable to " + assignableTo.getName());
        }

        ClassLoader classLoader = (registry != null ?
                registry.getBeanClassLoader() : getDefaultClassLoader());
        T instance = (T) createInstance(clazz, registry, classLoader);
        invokeAwareMethods(instance, registry, classLoader);
        return instance;
    }


    private static Object createInstance(Class<?> clazz, DefaultListableBeanFactory registry,
                                         ClassLoader classLoader) {

        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        if (constructors.length == 1 && constructors[0].getParameterCount() > 0) {
            try {
                Constructor<?> constructor = constructors[0];
                Object[] args = resolveArgs(constructor.getParameterTypes(), registry, classLoader);
                return BeanUtils.instantiateClass(constructor, args);
            } catch (Exception ex) {
                throw new RuntimeException("No suitable constructor found", ex);
            }
        }
        return BeanUtils.instantiateClass(clazz);
    }

    private static Object[] resolveArgs(Class<?>[] parameterTypes,

                                        DefaultListableBeanFactory registry, ClassLoader classLoader) {
        Object[] parameters = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameters[i] = resolveParameter(parameterTypes[i], registry, classLoader);
        }
        return parameters;
    }

    private static Object resolveParameter(Class<?> parameterType
            ,
                                           DefaultListableBeanFactory registry, ClassLoader classLoader) {
        if (parameterType == BeanFactory.class) {
            return registry;
        }
        if (parameterType == ClassLoader.class) {
            return classLoader;
        }
        throw new IllegalStateException("Illegal method parameter type: " + parameterType.getName());
    }

    // 调用 Aware 内部感知接口方法
    private static <T> void invokeAwareMethods(T instance, DefaultListableBeanFactory registry, ClassLoader classLoader) {
        if (instance instanceof Aware) {
            if (instance instanceof BeanFactoryAware beanFactoryAware) {
                beanFactoryAware.setBeanFactory(registry);
            }
        }

    }

}
