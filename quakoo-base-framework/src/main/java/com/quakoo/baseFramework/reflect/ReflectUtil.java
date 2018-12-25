package com.quakoo.baseFramework.reflect;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.quakoo.baseFramework.aop.asm.org.objectweb.asm.*;
import com.quakoo.baseFramework.aop.asm.org.objectweb.asm.ClassReader;
import com.quakoo.baseFramework.aop.asm.org.objectweb.asm.ClassVisitor;
import com.quakoo.baseFramework.aop.asm.org.objectweb.asm.ClassWriter;
import com.quakoo.baseFramework.aop.asm.org.objectweb.asm.Label;
import com.quakoo.baseFramework.aop.asm.org.objectweb.asm.MethodVisitor;
import com.quakoo.baseFramework.aop.asm.org.objectweb.asm.Opcodes;
import com.quakoo.baseFramework.aop.asm.org.objectweb.asm.Type;

/**
 * 反射帮助类
 * 
 * @author yongbiaoli
 * 
 */
public class ReflectUtil {

	static Map<Method, String[]> methodParamMap = new ConcurrentHashMap<Method, String[]>();

	/**
	 * 把对象转换成map
	 * 
	 * @param obj
	 * @param filters
	 *            过滤字段
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> objecttoMap(Object obj, String... filters) throws Exception {
		List<String> filterList = new ArrayList<String>();
		if (filters != null) {
			for (String filter : filters) {
				filterList.add(filter);
			}
		}
		Map<String, Object> paramsMap = new HashMap<String, Object>();

		PropertyDescriptor[] pds = Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors();

		for (PropertyDescriptor pd : pds) {
			if (!"class".equals(pd.getName()) && !filterList.contains(pd.getName())) {
				if (pd.getReadMethod() != null) {
					Object value = pd.getReadMethod().invoke(obj);
					if (value != null) {
						paramsMap.put(pd.getName(), value);
					}
				}
			}
		}
		return paramsMap;
	}

	/**
	 * 把map转成对象
	 * 
	 * @param obj
	 * @param filters
	 *            过滤字段
	 * @return
	 * @throws Exception
	 */
	public static <T> T mapToObject(Map<String, Object> map, T t) throws Exception {
		PropertyDescriptor[] pds = Introspector.getBeanInfo(t.getClass()).getPropertyDescriptors();
		for (PropertyDescriptor pd : pds) {
			if (!"class".equals(pd.getName())) {
				if (pd.getReadMethod() != null) {
					if(map.get(pd.getName()) instanceof Map&&!Map.class.isAssignableFrom(pd.getPropertyType())){
						pd.getWriteMethod().invoke(t, mapToObject((Map<String, Object>)map.get(pd.getName()),pd.getPropertyType().newInstance()));
					}else{
						pd.getWriteMethod().invoke(t, map.get(pd.getName()));
					}
				}
			}
		}
		return t;
	}

	/**
	 * 根据属性名字，获取类的属性
	 * 
	 * @param fieldName
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	public static Field getFieldByName(String fieldName, Class<?> clazz) throws Exception {
		Class<?> superClass = clazz;
		Field field = null;
		while (superClass != null && field == null) {
			try {
				field = superClass.getDeclaredField(fieldName);
			} catch (NoSuchFieldException e) {
			}
			superClass = superClass.getSuperclass();
		}
		return field;
	}

	/**
	 * 获取类的泛型的class
	 * 
	 * @param c
	 * @param index
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Class getGenericType(Class c, int index) {
		java.lang.reflect.Type genType = c.getGenericSuperclass();
		if (!(genType instanceof ParameterizedType)) {
			return Object.class;
		}
		java.lang.reflect.Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
		if (index >= params.length || index < 0) {
			throw new RuntimeException("Index outof bounds");
		}
		if (!(params[index] instanceof Class)) {
			return Object.class;
		}
		return (Class) params[index];
	}

	/**
	 * 根据方法名获取方法
	 * 
	 * @param methodName
	 * @param parameterTypes
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	public static Method getMethodByName(String methodName, Class parameterTypes, Class<?> clazz) throws Exception {
		Class<?> superClass = clazz;
		Method method = null;
		while (superClass != null && method == null) {
			try {
				method = superClass.getDeclaredMethod(methodName, parameterTypes);
			} catch (NoSuchMethodException e) {
			}
			superClass = superClass.getSuperclass();
		}
		return method;
	}

	/**
	 * 根据method获取他所有的参数的参数名字
	 * 
	 * @param m
	 * @return
	 */
	public static String[] getMethodParamNames(final Method m) {
		if (methodParamMap.get(m) != null) {
			return methodParamMap.get(m).clone();
		}
		final String[] paramNames = new String[m.getParameterTypes().length];
		final String n = m.getDeclaringClass().getName();
		final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		ClassReader cr = null;
		try {
			cr = new ClassReader(n);
		} catch (IOException e) {
			try {
				InputStream in = Thread.currentThread().getContextClassLoader()
						.getResourceAsStream(n.replace('.', '/') + ".class");
				cr = new ClassReader(in, true);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		cr.accept(new ClassVisitor(Opcodes.ASM4, cw) {
			@Override
			public MethodVisitor visitMethod(final int access, final String name, final String desc,
                                             final String signature, final String[] exceptions) {
				final Type[] args = Type.getArgumentTypes(desc);
				if (!name.equals(m.getName()) || !sameType(args, m.getParameterTypes())) {
					return super.visitMethod(access, name, desc, signature, exceptions);
				}
				MethodVisitor v = cv.visitMethod(access, name, desc, signature, exceptions);
				return new MethodVisitor(Opcodes.ASM4, v) {
					@Override
					public void visitLocalVariable(String name, String desc, String signature, Label start, Label end,
							int index) {
						if (Modifier.isStatic(m.getModifiers()) || index > 0) {
							for (int i = 0; i < paramNames.length; i++) {
								if (paramNames[i] == null) {
									paramNames[i] = name;
									break;
								}
							}
						}
						super.visitLocalVariable(name, desc, signature, start, end, index);
					}
				};
			}
		}, 0);
		methodParamMap.put(m, paramNames);
		return paramNames.clone();

	}

	/**
	 * 是否是同一个类型
	 * 
	 * @param types
	 * @param clazzes
	 * @return
	 */
	private static boolean sameType(Type[] types, Class<?>[] clazzes) {
		if (types.length != clazzes.length) {
			return false;
		}
		for (int i = 0; i < types.length; i++) {
			if (!Type.getType(clazzes[i]).equals(types[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * object to long
	 * 
	 * @param obj
	 * @return
	 */
	public static long getLongValueForLongOrInt(Object obj) {
		if (obj instanceof Integer) {
			return (Integer) obj;
		} else {
			return (Long) obj;
		}
	}

	/**
	 * 根据sql结果集和类型返回对应的类型
	 * 
	 * @param type
	 * @param rs
	 * @param cloum
	 * @return
	 * @throws SQLException
	 */
	public static Object getValueFormRsByType(java.lang.reflect.Type type, ResultSet rs, String cloum)
			throws SQLException {
		if (type == Long.class || type == long.class) {
			return rs.getLong(cloum);
		} else if (type == Integer.class || type == int.class) {
			return rs.getInt(cloum);
		} else if (type == Double.class || type == double.class) {
			return rs.getDouble(cloum);
		} else if (type == String.class) {
			return rs.getString(cloum);
		} else if(type== Date.class){
			return  rs.getTimestamp(cloum);
		}
		throw new RuntimeException("不支持此类型");

	}

	public static void main(String[] sdg) {
		System.out.println(Arrays.asList(getMethodParamNames(ReflectUtil.class.getMethods()[2])));
	}

}
