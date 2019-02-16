package com.quakoo.baseFramework.json;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
	public static void main(String[] dsf) throws JsonGenerationException, JsonMappingException, IOException{
	}
}
