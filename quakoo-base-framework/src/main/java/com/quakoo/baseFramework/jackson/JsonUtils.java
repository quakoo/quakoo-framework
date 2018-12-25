package com.quakoo.baseFramework.jackson;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author lichao
 * @date 2015年3月20日
 *
 */
public class JsonUtils {
	private static ObjectMapper objectMapper = new ObjectMapper();
	static {
		// 不包括NULL属性
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		// 序列化日期时以timestamps输出，默认true
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
		// 序列化char[]时以数组输出，默认false，默认输出String
		objectMapper.configure(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS, true);
		// 序列化枚举是以toString()来输出，默认false，即默认以name()来输出
		objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, false);
		// 序列化Map时对key进行排序操作，默认false
		objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false);
		// 序列化空属性对象是否抛出异常
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		// 序列化BigDecimal时之间输出原始数字还是科学计数，默认false=科学计数方式来输出
		objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, false);
		// 没有任何setter或handler来处理这样的属性,是否抛JsonMappingException异常
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// 这个特性决定parser是否将允许使用非双引号属性名字
		objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		// 该特性决定parser是否允许JSON字符串包含非引号控制字符（值小于32的ASCII字符，包含制表符和换行符）。
		// 如果该属性关闭，则如果遇到这些字符，则会抛出异常。
		objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
		// 该特性决定parser将是否允许解析使用Java/C++ 样式的注释（包括'/'+'*' 和'//' 变量）。
		objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		// 该特性决定parser是否允许单引号来包住属性名称和字符串值
		objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
	}

	/**
	 * 格式化输出，性能有损耗
	 */
	public static <T> String toJsonForDisplay(T t) {
		try {
			String jsonStr = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(t);
			return jsonStr;
		} catch (JsonGenerationException e) {
			throw new RuntimeException("JsonGenerationException", e);
		} catch (JsonMappingException e) {
			throw new RuntimeException("JsonMappingException", e);
		} catch (IOException e) {
			throw new RuntimeException("IOException", e);
		}
	}

	public static <T> String toJson(T t) {
		try {
			return objectMapper.writeValueAsString(t);
		} catch (JsonGenerationException e) {
			throw new RuntimeException("JsonGenerationException", e);
		} catch (JsonMappingException e) {
			throw new RuntimeException("JsonMappingException", e);
		} catch (IOException e) {
			throw new RuntimeException("IOException", e);
		}
	}

	public static <T> T fromJson(String jsonString, Class<T> clazz) {

		T object = null;
		try {
			object = objectMapper.readValue(jsonString, clazz);
		} catch (JsonGenerationException e) {
			throw new RuntimeException("JsonGenerationException", e);
		} catch (JsonMappingException e) {
			throw new RuntimeException("JsonMappingException", e);
		} catch (IOException e) {
			throw new RuntimeException("IOException", e);
		}
		return object;
	}

	public static <T> T fromJson(String jsonString, TypeReference<T> typeReference) {

		T object = null;
		try {
			object = objectMapper.readValue(jsonString, typeReference);
		} catch (JsonGenerationException e) {
			throw new RuntimeException("JsonGenerationException", e);
		} catch (JsonMappingException e) {
			throw new RuntimeException("JsonMappingException", e);
		} catch (IOException e) {
			throw new RuntimeException("IOException", e);
		}
		return object;
	}

}
