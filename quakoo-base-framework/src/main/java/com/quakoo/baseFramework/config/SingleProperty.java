package com.quakoo.baseFramework.config;

import java.lang.reflect.Field;

/**
 * 
 * @author LiYongbiao
 *
 */
public class SingleProperty {

	private String path;
	private boolean isJson;
	private Field field;
	private boolean isstatic;
	private Class clazz;
	private Object obj;
	
	private Class<?> typeReference;
	
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public Field getField() {
		return field;
	}
	public void setField(Field field) {
		this.field = field;
	}
	public boolean isIsstatic() {
		return isstatic;
	}
	public void setIsstatic(boolean isstatic) {
		this.isstatic = isstatic;
	}
	public Class getClazz() {
		return clazz;
	}
	public void setClazz(Class clazz) {
		this.clazz = clazz;
	}
	public Object getObj() {
		return obj;
	}
	public void setObj(Object obj) {
		this.obj = obj;
	}
	public boolean isJson() {
		return isJson;
	}
	public void setJson(boolean isJson) {
		this.isJson = isJson;
	}
	@Override
	public String toString() {
		return "SingleProperty [path=" + path + ", isJson=" + isJson
				+ ", field=" + field + ", isstatic=" + isstatic + ", clazz="
				+ clazz + ", obj=" + obj + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + (isJson ? 1231 : 1237);
		result = prime * result + (isstatic ? 1231 : 1237);
		result = prime * result + ((obj == null) ? 0 : obj.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SingleProperty other = (SingleProperty) obj;
		if (clazz == null) {
			if (other.clazz != null)
				return false;
		} else if (!clazz.equals(other.clazz))
			return false;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		if (isJson != other.isJson)
			return false;
		if (isstatic != other.isstatic)
			return false;
		if (this.obj == null) {
			if (other.obj != null)
				return false;
		} else if (!this.obj.equals(other.obj))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}
	public Class<?> getTypeReference() {
		return typeReference;
	}
	public void setTypeReference(Class<?> typeReference) {
		this.typeReference = typeReference;
	}
	
	public SingleProperty(String path, boolean isJson, Field field,
			boolean isstatic, Class clazz, Object obj, Class<?> typeReference) {
		super();
		this.path = path;
		this.isJson = isJson;
		this.field = field;
		this.isstatic = isstatic;
		this.clazz = clazz;
		this.obj = obj;
		this.typeReference = typeReference;
	}


	
	
	
	
	
}
