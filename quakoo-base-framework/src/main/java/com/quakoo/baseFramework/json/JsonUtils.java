package com.quakoo.baseFramework.json;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.quakoo.baseFramework.reflect.ReflectUtil;
import com.quakoo.baseFramework.util.LZMAUtil;
import com.quakoo.baseFramework.secure.Base64Util;
import com.quakoo.baseFramework.util.LZMAUtil;

/**
 * 
 * @author liyongbiao
 *
 */
public class JsonUtils {
	public static ObjectMapper objectMapper = new ObjectMapper();
	static {
	    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); 
	    objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
	    objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
	    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS,false);
	}





	public static <T> String zipFormat(T t) throws Exception {
			String jsonStr = objectMapper.writeValueAsString(t);
			return Base64Util.encode(LZMAUtil.zip(jsonStr.getBytes()));
	}
	
	public static <T> T zipParse(String jsonString, Class<T> clazz) throws Exception {
		T object = objectMapper.readValue(new String(LZMAUtil.unzip(Base64Util.decode(jsonString))), clazz);
		return object;
	}
	
	public static <T> T zipParse(String jsonString, TypeReference<T> typeReference) throws Exception {
		T object = objectMapper.readValue(new String(LZMAUtil.unzip(Base64Util.decode(jsonString))), typeReference);
		return object;
	}


	public static <T> String format(T t) throws JsonGenerationException, JsonMappingException, IOException {
			String jsonStr = objectMapper.writeValueAsString(t);
			return jsonStr;
	}
	
	public static <T> T parse(String jsonString, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
		T object = objectMapper.readValue(jsonString, clazz);
		return object;
	}
	public static <T> T parse(byte[] jsonStringByte, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
		T object = objectMapper.readValue(jsonStringByte, clazz);
		return object;
	}

	public static <T> T parse(String jsonString, TypeReference<T> typeReference) throws JsonParseException, JsonMappingException, IOException {
		T object = objectMapper.readValue(jsonString, typeReference);
		return object;
	}

	public static <T> T parse(String jsonString,Type type) throws JsonParseException, JsonMappingException, IOException {
		T object = objectMapper.readValue(jsonString, objectMapper.constructType(type));
		return object;
	}

	public static class User{
		@JsonIgnore
		private String name;
		private String icon;
	    @JsonFormat( pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
		private Date date=new Date();
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getIcon() {
			return icon;
		}
		public void setIcon(String icon) {
			this.icon = icon;
		}
		public Date getDate() {
			return date;
		}
		public void setDate(Date date) {
			this.date = date;
		}
		
		
	}
	public static void main(String[] dsf) throws Exception{
		JavaType javaType=null;
		PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(TestUser.class)
				.getPropertyDescriptors();
		for (PropertyDescriptor one : propertyDescriptors) {
			String fieldName = one.getName();
			if ("sdf".equals(fieldName)) {
				Field field = ReflectUtil
						.getFieldByName(fieldName, TestUser.class);
				Type fc = field.getGenericType(); // 关键的地方得到其Generic的类型
				javaType=TypeFactory.defaultInstance().constructType(fc);
			}
		}

		List<Map<Long,Integer>> sdf=new ArrayList<>();
		Map<Long,Integer> map1=new HashMap<Long,Integer>();
		map1.put(2001l,1);
		map1.put(2002l,1);
		map1.put(2003l,1);
		Map<Long,Integer> map2=new HashMap<Long,Integer>();
		map2.put(1001l,1);
		map2.put(1002l,1);
		map2.put(1003l,1);
		sdf.add(map1);
		sdf.add(map2);


		List<Map<Long,Integer>> sdfw=JsonUtils.parse(JsonUtils.format(sdf),javaType);
		System.out.println(sdfw);
	}
}
