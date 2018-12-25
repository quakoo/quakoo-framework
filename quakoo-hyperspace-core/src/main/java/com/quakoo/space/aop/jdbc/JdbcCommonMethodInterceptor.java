package com.quakoo.space.aop.jdbc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.quakoo.space.annotation.jdbc.JdbcDaoMethod;
import com.quakoo.space.annotation.jdbc.JdbcMethodParam;
import com.quakoo.space.annotation.jdbc.JdbcSqlMethod;
import com.quakoo.space.enums.jdbc.JdbcMethodEnum;
import com.quakoo.space.enums.jdbc.JdbcMethodParamEnum;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakoo.space.JdbcBaseDao;

public class JdbcCommonMethodInterceptor implements MethodInterceptor {

	Logger logger = LoggerFactory.getLogger(JdbcCommonMethodInterceptor.class);

	private int getParamsIndex(Method method, JdbcMethodParamEnum paramEnum) {
		int result = -1;
		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < annotations.length; i++) {
			if (null != annotations[i] && annotations[i].length > 0) {
				Annotation one = annotations[i][0];
				if (one instanceof JdbcMethodParam) {
					JdbcMethodParam param = (JdbcMethodParam) one;
					if (param.paramEnum() == paramEnum) {
						result = i;
						break;
					}
				}
			}
		}
		return result;
	}

	private void validate(Method method) {
		JdbcDaoMethod jdbcDaoMethod = method.getAnnotation(JdbcDaoMethod.class);
		String sqlMethodName = method.getAnnotation(JdbcSqlMethod.class)
				.value();
		if (StringUtils.isBlank(sqlMethodName)) {
			throw new IllegalArgumentException("SqlMethodName is null");
		}
		if (null == jdbcDaoMethod) {
			throw new IllegalArgumentException("JdbcDaoMethod is null");
		}
	}

	protected long getShardingId(int shardingId_index, Object[] args) {
		long shardingId = 0;
		if (shardingId_index == -1) {
			// no shardingId
		} else if (args[shardingId_index] instanceof Integer) {
			shardingId = ((Integer) args[shardingId_index]).longValue();
		} else {
			shardingId = (Long) args[shardingId_index];
		}
		return shardingId;
	}

	private String getSql(Object object, Method method, Object[] args,
			String sqlMethodName) throws Throwable {
		Method sqlMethod = object.getClass().getDeclaredMethod(sqlMethodName,
				method.getParameterTypes());
		sqlMethod.setAccessible(true);
		String sql = sqlMethod.invoke(object, args).toString();
		return sql;
	}

	protected Object handleGetCount(Object object, Method method, Object[] args)
			throws Throwable {
		this.validate(method);
		int shardingId_index = this.getParamsIndex(method,
				JdbcMethodParamEnum.shardingId);
		long shardingId = this.getShardingId(shardingId_index, args);
		String sqlMethodName = method.getAnnotation(JdbcSqlMethod.class)
				.value();
		String sql = this.getSql(object, method, args, sqlMethodName);
		JdbcBaseDao<?> baseDao = (JdbcBaseDao<?>) object;
		//logger.debug("==============shardingId:" + shardingId + ",sql:" + sql);
		return baseDao.jdbc_getCount(shardingId, sql, null);
	}

	protected Object handleGetList(Object object, Method method, Object[] args)
			throws Throwable {
		this.validate(method);
		int shardingId_index = this.getParamsIndex(method,
				JdbcMethodParamEnum.shardingId);
		long shardingId = this.getShardingId(shardingId_index, args);
		String sqlMethodName = method.getAnnotation(JdbcSqlMethod.class)
				.value();
		String sql = this.getSql(object, method, args, sqlMethodName);
		JdbcBaseDao<?> baseDao = (JdbcBaseDao<?>) object;
		//logger.debug("==============shardingId:" + shardingId + ",sql:" + sql);
		return baseDao.jdbc_getList(shardingId, sql, null);
	}

	@Override
	public Object intercept(Object object, Method method, Object[] args,
			MethodProxy methodProxy) throws Throwable {
		JdbcDaoMethod jdbcDaoMethod = method.getAnnotation(JdbcDaoMethod.class);
		if (null != jdbcDaoMethod) {
			JdbcMethodEnum jdbcMethodEnum = jdbcDaoMethod.methodEnum();
			if (jdbcMethodEnum == JdbcMethodEnum.getList) {
				return this.handleGetList(object, method, args);
			} else if (jdbcMethodEnum == JdbcMethodEnum.getCount) {
				return this.handleGetCount(object, method, args);
			} else {
				return methodProxy.invokeSuper(object, args);
			}
		}
		return methodProxy.invokeSuper(object, args);
	}
}
