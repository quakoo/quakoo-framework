package com.quakoo.baseFramework.convert;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 
 * @author LiYongbiao1
 *
 */
public class ConvertUtils {


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
	public static Object getValueFormRsByType(java.lang.reflect.Type type,
			ResultSet rs, String cloum) throws SQLException {
		if (type == Long.class || type == long.class) {
			return rs.getLong(cloum);
		} else if (type == Integer.class || type == int.class) {
			return rs.getInt(cloum);
		} else if (type == Double.class || type == double.class) {
			return rs.getDouble(cloum);
		} else if (type == String.class) {
			return rs.getString(cloum);
		}
		throw new RuntimeException("不支持此类型");
	}

}
