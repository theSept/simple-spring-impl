package com.liuxu.springframework.aop.aspectj.pointcut;

import com.liuxu.springframework.utils.ClassUtils;
import com.liuxu.springframework.utils.ReflectionUtils;
import org.aspectj.weaver.ReferenceType;
import org.aspectj.weaver.ReferenceTypeDelegate;
import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.ast.And;
import org.aspectj.weaver.ast.Call;
import org.aspectj.weaver.ast.FieldGetCall;
import org.aspectj.weaver.ast.HasAnnotation;
import org.aspectj.weaver.ast.ITestVisitor;
import org.aspectj.weaver.ast.Instanceof;
import org.aspectj.weaver.ast.Literal;
import org.aspectj.weaver.ast.Not;
import org.aspectj.weaver.ast.Or;
import org.aspectj.weaver.ast.Test;
import org.aspectj.weaver.internal.tools.MatchingContextBasedTest;
import org.aspectj.weaver.reflect.ReflectionBasedReferenceTypeDelegate;
import org.aspectj.weaver.reflect.ReflectionVar;
import org.aspectj.weaver.reflect.ShadowMatchImpl;
import org.aspectj.weaver.tools.ShadowMatch;

import java.lang.reflect.Field;


/**
 * This class encapsulates some AspectJ internal knowledge that should be
 * pushed back into the AspectJ project in a future release.
 *
 * <p>It relies on implementation specific knowledge in AspectJ to break
 * encapsulation and do something AspectJ was not designed to do: query
 * the types of runtime tests that will be performed. The code here should
 * migrate to {@code ShadowMatch.getVariablesInvolvedInRuntimeTest()}
 * or some similar operation.
 *
 * <p>See <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=151593">Bug 151593</a>
 *
 *
 * 该类从SpringAOP拿的，封装了AspectJ校验匹配结果的API。
 * 一个帮助类，用来分析 ShadowMatch 里的 runtime test（Residue）
 * 因为 ShadowMatch 可能包含复杂的布尔表达式，比如 args() && this() && if()，RuntimeTestWalker 就提供了便利方法去拆解这些。
 *
 * @author Adrian Colyer
 * @author Ramnivas Laddad
 * @since 2.0
 */
class RuntimeTestWalker {

	private static final Field residualTestField;

	private static final Field varTypeField;

	private static final Field myClassField;


	static {
		try {
			residualTestField = ShadowMatchImpl.class.getDeclaredField("residualTest");
			varTypeField = ReflectionVar.class.getDeclaredField("varType");
			myClassField = ReflectionBasedReferenceTypeDelegate.class.getDeclaredField("myClass");
		}
		catch (NoSuchFieldException ex) {
			throw new IllegalStateException("The version of aspectjtools.jar / aspectjweaver.jar " +
					"on the classpath is incompatible with this version of Spring: " + ex);
		}
	}


	private final Test runtimeTest;


	public RuntimeTestWalker(ShadowMatch shadowMatch) {
		try {
			ReflectionUtils.makeAccessible(residualTestField);
			this.runtimeTest = (Test) residualTestField.get(shadowMatch);
		}
		catch (IllegalAccessException ex) {
			throw new IllegalStateException(ex);
		}
	}


	/**
	 * 是否有 this() / target() 子类型相关的检测。如果有就是子类敏感，告诉你这个通知最后还是要在真实运行的时候调用 {@link #testThisInstanceOfResidue(Class)}判断。
     * <p>
     * 例如：依赖 this 的运行时类型（代理类型） - {@code @Before("this(com.example.api.OrderService)")}
     * <br/>
     * 依赖 target 的运行时类型（目标实现类型） - {@code @Before("target(com.example.impl.OrderServiceImpl)")}
     *
     * <p>
     * JDK/CGLIB的代理方式不同，所以 {@code this()} 可能会不同，需要真实调用时判断是否匹配。
	 */
	public boolean testsSubtypeSensitiveVars() {
		return (this.runtimeTest != null &&
				new SubtypeSensitiveVarTypeTestVisitor().testsSubtypeSensitiveVars(this.runtimeTest));
	}

	/**
	 * 判断某个类是否符合 this()
     * 用当前调用现场的 this 对象的实际类型去验证 this(Type) 是否成立
     * <p/>
     * 使用阶段：运行时匹配阶段（已有 thisObject 可用时）。
	 * @param thisClass 切点表达式中 this() 所依赖的类
	 */
	public boolean testThisInstanceOfResidue(Class<?> thisClass) {
		return (this.runtimeTest != null &&
				new ThisInstanceOfResidueTestVisitor(thisClass).thisInstanceOfMatches(this.runtimeTest));
	}

	/**
	 * 用目标对象的实现类型（非代理类型）去验证表达式里的 target(Type) 或相关 residue 是否可能成立。
	 * <p> 例如：{@code @Before("target(com.example.impl.OrderServiceImpl)")}，
	 * 目标类是 OrderServiceImpl：{@code testTargetInstanceOfResidue(OrderServiceImpl.class)} return {@code true}
     *
     * <p>
     * 使用阶段：静态匹配阶段的预筛选。
     * 这一步和代理无关，JDK/CGLIB 都不影响，因为 targetClass 就是真实实现类
     *
	 * @param targetClass 切点表达式中 target() 所依赖的类
	 */
	public boolean testTargetInstanceOfResidue(Class<?> targetClass) {
		return (this.runtimeTest != null &&
				new TargetInstanceOfResidueTestVisitor(targetClass).targetInstanceOfMatches(this.runtimeTest));
	}


	private static class TestVisitorAdapter implements ITestVisitor {

		protected static final int THIS_VAR = 0;
		protected static final int TARGET_VAR = 1;
		protected static final int AT_THIS_VAR = 3;
		protected static final int AT_TARGET_VAR = 4;
		protected static final int AT_ANNOTATION_VAR = 8;

		@Override
		public void visit(And e) {
			e.getLeft().accept(this);
			e.getRight().accept(this);
		}

		@Override
		public void visit(Or e) {
			e.getLeft().accept(this);
			e.getRight().accept(this);
		}

		@Override
		public void visit(Not e) {
			e.getBody().accept(this);
		}

		@Override
		public void visit(Instanceof i) {
		}

		@Override
		public void visit(Literal literal) {
		}

		@Override
		public void visit(Call call) {
		}

		@Override
		public void visit(FieldGetCall fieldGetCall) {
		}

		@Override
		public void visit(HasAnnotation hasAnnotation) {
		}

		@Override
		public void visit(MatchingContextBasedTest matchingContextTest) {
		}

		protected int getVarType(ReflectionVar v) {
			try {
				ReflectionUtils.makeAccessible(varTypeField);
				return (Integer) varTypeField.get(v);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}


	private abstract static class InstanceOfResidueTestVisitor extends TestVisitorAdapter {

		private final Class<?> matchClass;

		private boolean matches;

		private final int matchVarType;

		public InstanceOfResidueTestVisitor(Class<?> matchClass, boolean defaultMatches, int matchVarType) {
			this.matchClass = matchClass;
			this.matches = defaultMatches;
			this.matchVarType = matchVarType;
		}

		public boolean instanceOfMatches(Test test) {
			test.accept(this);
			return this.matches;
		}

		@Override
		public void visit(Instanceof i) {
			int varType = getVarType((ReflectionVar) i.getVar());
			if (varType != this.matchVarType) {
				return;
			}
			Class<?> typeClass = null;
			ResolvedType type = (ResolvedType) i.getType();
			if (type instanceof ReferenceType referenceType) {
				ReferenceTypeDelegate delegate = referenceType.getDelegate();
				if (delegate instanceof ReflectionBasedReferenceTypeDelegate) {
					try {
						ReflectionUtils.makeAccessible(myClassField);
						typeClass = (Class<?>) myClassField.get(delegate);
					}
					catch (IllegalAccessException ex) {
						throw new IllegalStateException(ex);
					}
				}
			}
			try {
				// Don't use ResolvedType.isAssignableFrom() as it won't be aware of (Spring) mixins
				if (typeClass == null) {
					typeClass = ClassUtils.forName(type.getName(), this.matchClass.getClassLoader());
				}
				this.matches = typeClass.isAssignableFrom(this.matchClass);
			}
			catch (ClassNotFoundException ex) {
				this.matches = false;
			}
		}
	}


	/**
	 * Check if residue of target(TYPE) kind. See SPR-3783 for more details.
	 */
	private static class TargetInstanceOfResidueTestVisitor extends InstanceOfResidueTestVisitor {

		public TargetInstanceOfResidueTestVisitor(Class<?> targetClass) {
			super(targetClass, false, TARGET_VAR);
		}

		public boolean targetInstanceOfMatches(Test test) {
			return instanceOfMatches(test);
		}
	}


	/**
	 * Check if residue of this(TYPE) kind. See SPR-2979 for more details.
	 */
	private static class ThisInstanceOfResidueTestVisitor extends InstanceOfResidueTestVisitor {

		public ThisInstanceOfResidueTestVisitor(Class<?> thisClass) {
			super(thisClass, true, THIS_VAR);
		}

		// TODO: Optimization: Process only if this() specifies a type and not an identifier.
		public boolean thisInstanceOfMatches(Test test) {
			return instanceOfMatches(test);
		}
	}


	private static class SubtypeSensitiveVarTypeTestVisitor extends TestVisitorAdapter {

		private final Object thisObj = new Object();

		private final Object targetObj = new Object();

		private final Object[] argsObjs = new Object[0];

		private boolean testsSubtypeSensitiveVars = false;

		public boolean testsSubtypeSensitiveVars(Test aTest) {
			aTest.accept(this);
			return this.testsSubtypeSensitiveVars;
		}

		@Override
		public void visit(Instanceof i) {
			ReflectionVar v = (ReflectionVar) i.getVar();
			Object varUnderTest = v.getBindingAtJoinPoint(this.thisObj, this.targetObj, this.argsObjs);
			if (varUnderTest == this.thisObj || varUnderTest == this.targetObj) {
				this.testsSubtypeSensitiveVars = true;
			}
		}

		@Override
		public void visit(HasAnnotation hasAnn) {
			// If you thought things were bad before, now we sink to new levels of horror...
			ReflectionVar v = (ReflectionVar) hasAnn.getVar();
			int varType = getVarType(v);
			if (varType == AT_THIS_VAR || varType == AT_TARGET_VAR || varType == AT_ANNOTATION_VAR) {
				this.testsSubtypeSensitiveVars = true;
			}
		}
	}

}
