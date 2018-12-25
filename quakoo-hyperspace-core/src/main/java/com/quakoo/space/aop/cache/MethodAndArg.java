package com.quakoo.space.aop.cache;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MethodAndArg {

	private Method method;
	private List<Object> arg=new ArrayList<>();
	public Method getMethod() {
		return method;
	}
	public void setMethod(Method method) {
		this.method = method;
	}
	public List<Object> getArg() {
		return arg;
	}
	public void setArg(List<Object> arg) {
		this.arg = arg;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((arg == null) ? 0 : arg.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
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
		MethodAndArg other = (MethodAndArg) obj;
		if (arg == null) {
			if (other.arg != null)
				return false;
		} else if (!arg.equals(other.arg))
			return false;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		return true;
	}
	
	
}
