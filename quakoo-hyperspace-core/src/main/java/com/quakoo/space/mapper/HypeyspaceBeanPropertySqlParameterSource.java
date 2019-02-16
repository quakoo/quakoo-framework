/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.quakoo.space.mapper;

import com.quakoo.baseFramework.json.JsonUtils;
import com.quakoo.baseFramework.reflect.ReflectUtil;
import com.quakoo.baseFramework.util.StringUtil;
import com.quakoo.space.annotation.domain.HyperspaceColumn;
import com.quakoo.space.enums.JsonTypeReference;
import com.quakoo.space.model.FieldInfo;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link SqlParameterSource} implementation that obtains parameter values
 * from bean properties of a given JavaBean object. The names of the bean
 * properties have to match the parameter names.
 *
 * <p>Uses a Spring BeanWrapper for bean property access underneath.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 * @see NamedParameterJdbcTemplate
 * @see BeanWrapper
 */
public class HypeyspaceBeanPropertySqlParameterSource extends AbstractSqlParameterSource {

	private final BeanWrapper beanWrapper;

	private List<String> josnParams=new ArrayList<>();

	@Nullable
	private String[] propertyNames;


	/**
	 * Create a new BeanPropertySqlParameterSource for the given bean.
	 * @param object the bean instance to wrap
	 */
	public HypeyspaceBeanPropertySqlParameterSource(Object object) throws DataAccessException {
		this.beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(object);
		try {
			PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(object.getClass())
					.getPropertyDescriptors();
			for (PropertyDescriptor one : propertyDescriptors) {
				String fieldName = one.getName();
				if (!"class".equals(fieldName)) {
					Field field = ReflectUtil
							.getFieldByName(fieldName, object.getClass());
					if (field != null) {
						HyperspaceColumn autowareMap = field.getAnnotation(HyperspaceColumn.class);
						if (autowareMap != null && autowareMap.jsonType()!=null
								&& autowareMap.jsonType()!=Object.class) {
							josnParams.add(one.getName());
						}
						if (autowareMap != null && autowareMap.jsonTypeReference()!=null
								&& autowareMap.jsonTypeReference()!=JsonTypeReference.type_null) {
							josnParams.add(one.getName());
						}
					}
				}
			}
		}catch (Exception e){
			throw new RuntimeException(e);
		}

	}


	@Override
	public boolean hasValue(String paramName) {
		return this.beanWrapper.isReadableProperty(paramName);
	}

	@Override
	@Nullable
	public Object getValue(String paramName) throws IllegalArgumentException {
		try {
			if(josnParams.contains(paramName)){
				return JsonUtils.format(this.beanWrapper.getPropertyValue(paramName));
			}
			return this.beanWrapper.getPropertyValue(paramName);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException(ex.getMessage());
		}
	}

	/**
	 * Derives a default SQL type from the corresponding property type.
	 * @see StatementCreatorUtils#javaTypeToSqlParameterType
	 */
	@Override
	public int getSqlType(String paramName) {
		int sqlType = super.getSqlType(paramName);
		if (sqlType != TYPE_UNKNOWN) {
			return sqlType;
		}
		Class<?> propType = this.beanWrapper.getPropertyType(paramName);
		return StatementCreatorUtils.javaTypeToSqlParameterType(propType);
	}

	@Override
	@Nullable
	public String[] getParameterNames() {
		return getReadablePropertyNames();
	}

	/**
	 * Provide access to the property names of the wrapped bean.
	 * Uses support provided in the {@link PropertyAccessor} interface.
	 * @return an array containing all the known property names
	 */
	public String[] getReadablePropertyNames() {
		if (this.propertyNames == null) {
			List<String> names = new ArrayList<>();
			PropertyDescriptor[] props = this.beanWrapper.getPropertyDescriptors();
			for (PropertyDescriptor pd : props) {
				if (this.beanWrapper.isReadableProperty(pd.getName())) {
					names.add(pd.getName());
				}
			}
			this.propertyNames = StringUtils.toStringArray(names);
		}
		return this.propertyNames;
	}

}
