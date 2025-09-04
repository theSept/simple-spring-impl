/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liuxu.springframework.aop.aspectj;

import com.liuxu.springframework.aop.utils.AspectJProxyUtils;
import com.liuxu.springframework.core.ParameterNameDiscoverer;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;


/**
 * ParameterNameDiscoverer 尝试从切入点表达式、返回和抛出子句中推断出通知方法的参数名称的实现。如果没有明确的注解，则返回 null。
 * 算法总结
 * 如果可以推断出明确的绑定，那么它就是。如果无法满足建议要求，null则返回。通过将属性设置为 raiseExceptions ，将引发描述性异常，而不是在无法发现参数名称的情况下返回null。true
 *
 * 算法详情
 * 此类按以下方式解释参数：
 * 如果方法的第一个参数类型 JoinPoint 为 or ProceedingJoinPoint，则假定它用于传递 thisJoinPoint 给通知，并且参数名称将被分配值 "thisJoinPoint"。
 * 如果方法的第一个参数类型 JoinPoint.StaticPart为 ，则假定它用于传递 "thisJoinPointStaticPart" 给通知，并且参数名称将被分配值 "thisJoinPointStaticPart"。
 * 如果已设置 a throwingName ，并且没有 类型的未绑定参数 Throwable+，则引发 an IllegalArgumentException 。如果存在多个类型的未绑定参数 Throwable+，则会引发 an AspectJAdviceParameterNameDiscoverer.AmbiguousBindingException 。如果只有一个类型的 Throwable+未绑定参数，则为相应的参数名称分配值 <throwingName>。
 * 如果仍然存在未绑定的参数，则检查切入点表达式。设为 a 以绑定形式使用的基于注释的切入点表达式（@annotation、@this、@target、@args、@within、@withincode）的数量。绑定形式的用法本身有待推断：如果切入点内的表达式是满足 Java 变量名称约定的单个字符串文字，则假定它是变量名称。如果为零，我们a进入下一阶段。如果> 1，则a加起 anAmbiguousBindingException。如果 == 1，并且没有类型的Annotation+未绑定参数，则a引发 anIllegalArgumentException。如果恰好有一个这样的参数，则为相应的参数名称分配切入点表达式中的值。
 * 如果已设置 a returningName ，并且没有未绑定的参数，则引发 an IllegalArgumentException 。如果存在多个未绑定的参数，则引发 an AmbiguousBindingException 。如果只有一个未绑定的参数，则相应的参数名称将被分配 returningName.
 * 如果仍然存在未绑定的参数，则再次thistargetargs检查绑定形式中使用的切入点表达式，以及切入点表达式（绑定形式按照基于注释的切入点的描述进行推断）。如果仍然存在多个原始类型的未绑定参数（只能绑定在 args中），则引发 anAmbiguousBindingException。如果只有一个原始类型的参数，那么如果找到一个args绑定变量，我们为相应的参数名称分配变量名称。如果未args找到绑定变量，则引发 。IllegalStateException如果有多个args绑定变量，则引发 anAmbiguousBindingException。此时，如果还有多个未绑定的参数，我们提出一个 AmbiguousBindingException。如果没有剩余的未绑定的参数，我们就完成了。如果只有一个未绑定的参数剩余，并且只有一个候选变量名从 、 target或 args中解绑this，则将其赋值为相应的参数名称。如果存在多种可能性，则引发 anAmbiguousBindingException。
 * 引发 IllegalArgumentException or AmbiguousBindingException 的行为可配置为允许将此发现器用作责任链的一部分。默认情况下，将记录条件，并且该 getParameterNames(Method) 方法将简单地返回 null。如果属性 raiseExceptions 设置为 true，则条件将分别抛出为 IllegalArgumentException AmbiguousBindingException和
 */
public class AspectJAdviceParameterNameDiscoverer implements ParameterNameDiscoverer {

    private static final String THIS_JOIN_POINT = "thisJoinPoint";
    private static final String THIS_JOIN_POINT_STATIC_PART = "thisJoinPointStaticPart";

    // Steps in the binding algorithm...
    private static final int STEP_JOIN_POINT_BINDING = 1;
    private static final int STEP_THROWING_BINDING = 2;
    private static final int STEP_ANNOTATION_BINDING = 3;
    private static final int STEP_RETURNING_BINDING = 4;
    private static final int STEP_PRIMITIVE_ARGS_BINDING = 5;
    private static final int STEP_THIS_TARGET_ARGS_BINDING = 6;
    private static final int STEP_REFERENCE_PCUT_BINDING = 7;
    private static final int STEP_FINISHED = 8;

    private static final Set<String> singleValuedAnnotationPcds = Set.of(
            "@this",
            "@target",
            "@within",
            "@withincode",
            "@annotation");

    private static final Set<String> nonReferencePointcutTokens = new HashSet<>();


    static {
        Set<PointcutPrimitive> pointcutPrimitives = PointcutParser.getAllSupportedPointcutPrimitives();
        for (PointcutPrimitive primitive : pointcutPrimitives) {
            nonReferencePointcutTokens.add(primitive.getName());
        }
        nonReferencePointcutTokens.add("&&");
        nonReferencePointcutTokens.add("!");
        nonReferencePointcutTokens.add("||");
        nonReferencePointcutTokens.add("and");
        nonReferencePointcutTokens.add("or");
        nonReferencePointcutTokens.add("not");
    }


    /** The pointcut expression associated with the advice, as a simple String. */
    private final String pointcutExpression;

    private boolean raiseExceptions;

    /** If the advice is afterReturning, and binds the return value, this is the parameter name used. */
    private String returningName;

    /** If the advice is afterThrowing, and binds the thrown value, this is the parameter name used. */
    private String throwingName;

    private Class<?>[] argumentTypes = new Class<?>[0];

    private String[] parameterNameBindings = new String[0];

    private int numberOfRemainingUnboundArguments;


    /**
     * Create a new discoverer that attempts to discover parameter names.
     * from the given pointcut expression.
     */
    public AspectJAdviceParameterNameDiscoverer(String pointcutExpression) {
        this.pointcutExpression = pointcutExpression;
    }


    /**
     * Indicate whether {@link IllegalArgumentException} and {@link AmbiguousBindingException}
     * must be thrown as appropriate in the case of failing to deduce advice parameter names.
     *
     * @param raiseExceptions {@code true} if exceptions are to be thrown
     */
    public void setRaiseExceptions(boolean raiseExceptions) {
        this.raiseExceptions = raiseExceptions;
    }

    /**
     * If {@code afterReturning} advice binds the return value, the
     * {@code returning} variable name must be specified.
     *
     * @param returningName the name of the returning variable
     */
    public void setReturningName(String returningName) {
        this.returningName = returningName;
    }

    /**
     * If {@code afterThrowing} advice binds the thrown value, the
     * {@code throwing} variable name must be specified.
     *
     * @param throwingName the name of the throwing variable
     */
    public void setThrowingName(String throwingName) {
        this.throwingName = throwingName;
    }

    /**
     * Deduce the parameter names for an advice method.
     * <p>See the {@link AspectJAdviceParameterNameDiscoverer class-level javadoc}
     * for this class for details on the algorithm used.
     *
     * @param method the target {@link Method}
     * @return the parameter names
     */
    @Override
    public String[] getParameterNames(Method method) {
        this.argumentTypes = method.getParameterTypes();
        this.numberOfRemainingUnboundArguments = this.argumentTypes.length;
        this.parameterNameBindings = new String[this.numberOfRemainingUnboundArguments];

        int minimumNumberUnboundArgs = 0;
        if (this.returningName != null) {
            minimumNumberUnboundArgs++;
        }
        if (this.throwingName != null) {
            minimumNumberUnboundArgs++;
        }
        if (this.numberOfRemainingUnboundArguments < minimumNumberUnboundArgs) {
            throw new IllegalStateException(
                    "Not enough arguments in method to satisfy binding of returning and throwing variables");
        }

        try {
            int algorithmicStep = STEP_JOIN_POINT_BINDING;
            while ((this.numberOfRemainingUnboundArguments > 0) && algorithmicStep < STEP_FINISHED) {
                switch (algorithmicStep++) {
                    case STEP_JOIN_POINT_BINDING -> {
                        if (!maybeBindThisJoinPoint()) {
                            maybeBindThisJoinPointStaticPart();
                        }
                    }
                    case STEP_THROWING_BINDING -> maybeBindThrowingVariable();
                    case STEP_ANNOTATION_BINDING -> maybeBindAnnotationsFromPointcutExpression();
                    case STEP_RETURNING_BINDING -> maybeBindReturningVariable();
                    case STEP_PRIMITIVE_ARGS_BINDING -> maybeBindPrimitiveArgsFromPointcutExpression();
                    case STEP_THIS_TARGET_ARGS_BINDING -> maybeBindThisOrTargetOrArgsFromPointcutExpression();
                    case STEP_REFERENCE_PCUT_BINDING -> maybeBindReferencePointcutParameter();
                    default -> throw new IllegalStateException("Unknown algorithmic step: " + (algorithmicStep - 1));
                }
            }
        } catch (AmbiguousBindingException | IllegalArgumentException ex) {
            if (this.raiseExceptions) {
                throw ex;
            } else {
                return null;
            }
        }

        if (this.numberOfRemainingUnboundArguments == 0) {
            return this.parameterNameBindings;
        } else {
            if (this.raiseExceptions) {
                throw new IllegalStateException("Failed to bind all argument names: " +
                        this.numberOfRemainingUnboundArguments + " argument(s) could not be bound");
            } else {
                // convention for failing is to return null, allowing participation in a chain of responsibility
                return null;
            }
        }
    }

    /**
     * An advice method can never be a constructor in Spring.
     *
     * @return {@code null}
     * @throws UnsupportedOperationException if
     *                                       {@link #setRaiseExceptions(boolean) raiseExceptions} has been set to {@code true}
     */
    @Override
    public String[] getParameterNames(Constructor<?> ctor) {
        if (this.raiseExceptions) {
            throw new UnsupportedOperationException("An advice method can never be a constructor");
        } else {
            // we return null rather than throw an exception so that we behave well
            // in a chain-of-responsibility.
            return null;
        }
    }


    private void bindParameterName(int index, String name) {
        this.parameterNameBindings[index] = name;
        this.numberOfRemainingUnboundArguments--;
    }

    /**
     * If the first parameter is of type JoinPoint or ProceedingJoinPoint, bind "thisJoinPoint" as
     * parameter name and return true, else return false.
     */
    private boolean maybeBindThisJoinPoint() {
        if ((this.argumentTypes[0] == JoinPoint.class) || (this.argumentTypes[0] == ProceedingJoinPoint.class)) {
            bindParameterName(0, THIS_JOIN_POINT);
            return true;
        } else {
            return false;
        }
    }

    private void maybeBindThisJoinPointStaticPart() {
        if (this.argumentTypes[0] == JoinPoint.StaticPart.class) {
            bindParameterName(0, THIS_JOIN_POINT_STATIC_PART);
        }
    }

    /**
     * If a throwing name was specified and there is exactly one choice remaining
     * (argument that is a subtype of Throwable) then bind it.
     */
    private void maybeBindThrowingVariable() {
        if (this.throwingName == null) {
            return;
        }

        // So there is binding work to do...
        int throwableIndex = -1;
        for (int i = 0; i < this.argumentTypes.length; i++) {
            if (isUnbound(i) && isSubtypeOf(Throwable.class, i)) {
                if (throwableIndex == -1) {
                    throwableIndex = i;
                } else {
                    // Second candidate we've found - ambiguous binding
                    throw new AmbiguousBindingException("Binding of throwing parameter '" +
                            this.throwingName + "' is ambiguous: could be bound to argument " +
                            throwableIndex + " or " + i);
                }
            }
        }

        if (throwableIndex == -1) {
            throw new IllegalStateException("Binding of throwing parameter '" + this.throwingName +
                    "' could not be completed as no available arguments are a subtype of Throwable");
        } else {
            bindParameterName(throwableIndex, this.throwingName);
        }
    }

    /**
     * If a returning variable was specified and there is only one choice remaining, bind it.
     */
    private void maybeBindReturningVariable() {
        if (this.numberOfRemainingUnboundArguments == 0) {
            throw new IllegalStateException(
                    "Algorithm assumes that there must be at least one unbound parameter on entry to this method");
        }

        if (this.returningName != null) {
            if (this.numberOfRemainingUnboundArguments > 1) {
                throw new AmbiguousBindingException("Binding of returning parameter '" + this.returningName +
                        "' is ambiguous: there are " + this.numberOfRemainingUnboundArguments + " candidates.");
            }

            // We're all set... find the unbound parameter, and bind it.
            for (int i = 0; i < this.parameterNameBindings.length; i++) {
                if (this.parameterNameBindings[i] == null) {
                    bindParameterName(i, this.returningName);
                    break;
                }
            }
        }
    }

    /**
     * Parse the string pointcut expression looking for:
     * &#64;this, &#64;target, &#64;args, &#64;within, &#64;withincode, &#64;annotation.
     * If we find one of these pointcut expressions, try and extract a candidate variable
     * name (or variable names, in the case of args).
     * <p>Some more support from AspectJ in doing this exercise would be nice... :)
     */
    private void maybeBindAnnotationsFromPointcutExpression() {
        List<String> varNames = new ArrayList<>();
        String[] tokens = tokenizeToStringArray(this.pointcutExpression, " ");
        for (int i = 0; i < tokens.length; i++) {
            String toMatch = tokens[i];
            int firstParenIndex = toMatch.indexOf('(');
            if (firstParenIndex != -1) {
                toMatch = toMatch.substring(0, firstParenIndex);
            }
            if (singleValuedAnnotationPcds.contains(toMatch)) {
                PointcutBody body = getPointcutBody(tokens, i);
                i += body.numTokensConsumed;
                String varName = maybeExtractVariableName(body.text);
                if (varName != null) {
                    varNames.add(varName);
                }
            } else if (tokens[i].startsWith("@args(") || tokens[i].equals("@args")) {
                PointcutBody body = getPointcutBody(tokens, i);
                i += body.numTokensConsumed;
                maybeExtractVariableNamesFromArgs(body.text, varNames);
            }
        }

        // 绑定切点表达式是注解的场景
        bindAnnotationsFromVarNames(varNames);
    }

    /**
     * Match the given list of extracted variable names to argument slots.
     */
    private void bindAnnotationsFromVarNames(List<String> varNames) {
        if (!varNames.isEmpty()) {
            /*通知方法参数只能有一个注解类型，并且切点表达式中也只能允许填写一个变量名称，否则抛出异常*/
            // we have work to do...
            int numAnnotationSlots = countNumberOfUnboundAnnotationArguments();
            if (numAnnotationSlots > 1) {
                throw new AmbiguousBindingException("Found " + varNames.size() +
                        " potential annotation variable(s) and " +
                        numAnnotationSlots + " potential argument slots");
            } else if (numAnnotationSlots == 1) {
                if (varNames.size() == 1) {
                    // it's a match
                    findAndBind(Annotation.class, varNames.get(0));
                } else {
                    // multiple candidate vars, but only one slot
                    throw new IllegalArgumentException("Found " + varNames.size() +
                            " candidate annotation binding variables" +
                            " but only one potential argument binding slot");
                }
            } else {
                // no slots so presume those candidate vars were actually type names
            }
        }
    }

    /**
     * If the token starts meets Java identifier conventions, it's in.
     */
    private String maybeExtractVariableName(String candidateToken) {
        if (AspectJProxyUtils.isVariableName(candidateToken)) {
            return candidateToken;
        }
        return null;
    }

    /**
     * Given an args pointcut body (could be {@code args} or {@code at_args}),
     * add any candidate variable names to the given list.
     */
    private void maybeExtractVariableNamesFromArgs(String argsSpec, List<String> varNames) {
        if (argsSpec == null) {
            return;
        }
        String[] tokens = tokenizeToStringArray(argsSpec, ",");
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].strip();
            String varName = maybeExtractVariableName(tokens[i]);
            if (varName != null) {
                varNames.add(varName);
            }
        }
    }

    /**
     * Parse the string pointcut expression looking for this(), target() and args() expressions.
     * If we find one, try and extract a candidate variable name and bind it.
     */
    private void maybeBindThisOrTargetOrArgsFromPointcutExpression() {
        if (this.numberOfRemainingUnboundArguments > 1) {
            throw new AmbiguousBindingException("Still " + this.numberOfRemainingUnboundArguments
                    + " unbound args at this()/target()/args() binding stage, with no way to determine between them");
        }

        List<String> varNames = new ArrayList<>();
        String[] tokens = tokenizeToStringArray(this.pointcutExpression, " ");
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals("this") ||
                    tokens[i].startsWith("this(") ||
                    tokens[i].equals("target") ||
                    tokens[i].startsWith("target(")) {
                PointcutBody body = getPointcutBody(tokens, i);
                i += body.numTokensConsumed;
                String varName = maybeExtractVariableName(body.text);
                if (varName != null) {
                    varNames.add(varName);
                }
            } else if (tokens[i].equals("args") || tokens[i].startsWith("args(")) {
                PointcutBody body = getPointcutBody(tokens, i);
                i += body.numTokensConsumed;
                List<String> candidateVarNames = new ArrayList<>();
                maybeExtractVariableNamesFromArgs(body.text, candidateVarNames);
                // we may have found some var names that were bound in previous primitive args binding step,
                // filter them out...
                for (String varName : candidateVarNames) {
                    if (!alreadyBound(varName)) {
                        varNames.add(varName);
                    }
                }
            }
        }

        if (varNames.size() > 1) {
            throw new AmbiguousBindingException("Found " + varNames.size() +
                    " candidate this(), target(), or args() variables but only one unbound argument slot");
        } else if (varNames.size() == 1) {
            for (int j = 0; j < this.parameterNameBindings.length; j++) {
                if (isUnbound(j)) {
                    bindParameterName(j, varNames.get(0));
                    break;
                }
            }
        }
        // else varNames.size must be 0 and we have nothing to bind.
    }

    private void maybeBindReferencePointcutParameter() {
        if (this.numberOfRemainingUnboundArguments > 1) {
            throw new AmbiguousBindingException("Still " + this.numberOfRemainingUnboundArguments
                    + " unbound args at reference pointcut binding stage, with no way to determine between them");
        }

        List<String> varNames = new ArrayList<>();
        String[] tokens = tokenizeToStringArray(this.pointcutExpression, " ");
        for (int i = 0; i < tokens.length; i++) {
            String toMatch = tokens[i];
            if (toMatch.startsWith("!")) {
                toMatch = toMatch.substring(1);
            }
            int firstParenIndex = toMatch.indexOf('(');
            if (firstParenIndex != -1) {
                toMatch = toMatch.substring(0, firstParenIndex);
            } else {
                if (tokens.length < i + 2) {
                    // no "(" and nothing following
                    continue;
                } else {
                    String nextToken = tokens[i + 1];
                    if (nextToken.charAt(0) != '(') {
                        // next token is not "(" either, can't be a pc...
                        continue;
                    }
                }

            }

            // eat the body
            PointcutBody body = getPointcutBody(tokens, i);
            i += body.numTokensConsumed;

            if (!nonReferencePointcutTokens.contains(toMatch)) {
                // then it could be a reference pointcut
                String varName = maybeExtractVariableName(body.text);
                if (varName != null) {
                    varNames.add(varName);
                }
            }
        }

        if (varNames.size() > 1) {
            throw new AmbiguousBindingException("Found " + varNames.size() +
                    " candidate reference pointcut variables but only one unbound argument slot");
        } else if (varNames.size() == 1) {
            for (int j = 0; j < this.parameterNameBindings.length; j++) {
                if (isUnbound(j)) {
                    bindParameterName(j, varNames.get(0));
                    break;
                }
            }
        }
        // else varNames.size must be 0 and we have nothing to bind.
    }

    /**
     * We've found the start of a binding pointcut at the given index into the
     * token array. Now we need to extract the pointcut body and return it.
     */
    private PointcutBody getPointcutBody(String[] tokens, int startIndex) {
        int numTokensConsumed = 0;
        String currentToken = tokens[startIndex];
        int bodyStart = currentToken.indexOf('(');
        if (currentToken.charAt(currentToken.length() - 1) == ')') {
            // It's an all in one... get the text between the first (and the last)
            return new PointcutBody(0, currentToken.substring(bodyStart + 1, currentToken.length() - 1));
        } else {
            StringBuilder sb = new StringBuilder();
            if (bodyStart >= 0 && bodyStart != (currentToken.length() - 1)) {
                sb.append(currentToken.substring(bodyStart + 1));
                sb.append(' ');
            }
            numTokensConsumed++;
            int currentIndex = startIndex + numTokensConsumed;
            while (currentIndex < tokens.length) {
                if (tokens[currentIndex].equals("(")) {
                    currentIndex++;
                    continue;
                }

                if (tokens[currentIndex].endsWith(")")) {
                    sb.append(tokens[currentIndex], 0, tokens[currentIndex].length() - 1);
                    return new PointcutBody(numTokensConsumed, sb.toString().trim());
                }

                String toAppend = tokens[currentIndex];
                if (toAppend.startsWith("(")) {
                    toAppend = toAppend.substring(1);
                }
                sb.append(toAppend);
                sb.append(' ');
                currentIndex++;
                numTokensConsumed++;
            }

        }

        // We looked and failed...
        return new PointcutBody(numTokensConsumed, null);
    }

    /**
     * Match up args against unbound arguments of primitive types.
     */
    private void maybeBindPrimitiveArgsFromPointcutExpression() {
        int numUnboundPrimitives = countNumberOfUnboundPrimitiveArguments();
        if (numUnboundPrimitives > 1) {
            throw new AmbiguousBindingException("Found " + numUnboundPrimitives +
                    " unbound primitive arguments with no way to distinguish between them.");
        }
        if (numUnboundPrimitives == 1) {
            // Look for arg variable and bind it if we find exactly one...
            List<String> varNames = new ArrayList<>();
            String[] tokens = tokenizeToStringArray(this.pointcutExpression, " ");
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].equals("args") || tokens[i].startsWith("args(")) {
                    PointcutBody body = getPointcutBody(tokens, i);
                    i += body.numTokensConsumed;
                    maybeExtractVariableNamesFromArgs(body.text, varNames);
                }
            }
            if (varNames.size() > 1) {
                throw new AmbiguousBindingException("Found " + varNames.size() +
                        " candidate variable names but only one candidate binding slot when matching primitive args");
            } else if (varNames.size() == 1) {
                // 1 primitive arg, and one candidate...
                for (int i = 0; i < this.argumentTypes.length; i++) {
                    if (isUnbound(i) && this.argumentTypes[i].isPrimitive()) {
                        bindParameterName(i, varNames.get(0));
                        break;
                    }
                }
            }
        }
    }

    /*
     * Return true if the parameter name binding for the given parameter
     * index has not yet been assigned.
     */
    private boolean isUnbound(int i) {
        return this.parameterNameBindings[i] == null;
    }

    private boolean alreadyBound(String varName) {
        for (int i = 0; i < this.parameterNameBindings.length; i++) {
            if (!isUnbound(i) && varName.equals(this.parameterNameBindings[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return {@code true} if the given argument type is a subclass
     * of the given supertype.
     */
    private boolean isSubtypeOf(Class<?> supertype, int argumentNumber) {
        return supertype.isAssignableFrom(this.argumentTypes[argumentNumber]);
    }

    private int countNumberOfUnboundAnnotationArguments() {
        int count = 0;
        for (int i = 0; i < this.argumentTypes.length; i++) {
            if (isUnbound(i) && isSubtypeOf(Annotation.class, i)) {
                count++;
            }
        }
        return count;
    }

    private int countNumberOfUnboundPrimitiveArguments() {
        int count = 0;
        for (int i = 0; i < this.argumentTypes.length; i++) {
            if (isUnbound(i) && this.argumentTypes[i].isPrimitive()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Find the argument index with the given type, and bind the given
     * {@code varName} in that position.
     */
    private void findAndBind(Class<?> argumentType, String varName) {
        for (int i = 0; i < this.argumentTypes.length; i++) {
            if (isUnbound(i) && isSubtypeOf(argumentType, i)) {
                bindParameterName(i, varName);
                return;
            }
        }
        throw new IllegalStateException("Expected to find an unbound argument of type '" +
                argumentType.getName() + "'");
    }


    /**
     * Simple record to hold the extracted text from a pointcut body, together
     * with the number of tokens consumed in extracting it.
     */
    private record PointcutBody(int numTokensConsumed, String text) {
    }

    /**
     * Thrown in response to an ambiguous binding being detected when
     * trying to resolve a method's parameter names.
     */
    @SuppressWarnings("serial")
    public static class AmbiguousBindingException extends RuntimeException {

        /**
         * Construct a new AmbiguousBindingException with the specified message.
         *
         * @param msg the detail message
         */
        public AmbiguousBindingException(String msg) {
            super(msg);
        }
    }


    private final String[] STRING_EMPTY_ARRAY = {};

    private String[] tokenizeToStringArray(String str, String delimiters) {
        if (str == null) {
            return STRING_EMPTY_ARRAY;
        }

        StringTokenizer stringTokenizer = new StringTokenizer(str, delimiters, false);
        List<String> strList = new ArrayList<>();
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            if (token != null) {
                token = token.trim();
            }

            if (!StringUtils.isEmpty(token)) {
                strList.add(token);
            }
        }

        return strList.toArray(new String[]{});
    }

}
