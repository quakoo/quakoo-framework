package com.quakoo.baseFramework.util;

import java.util.Date;
import java.util.Map;

import com.quakoo.baseFramework.json.JsonUtils;
import com.quakoo.baseFramework.reflect.ReflectUtil;


/**
 * 
 * @author LiYongbiao1
 *
 */
public class BaseLog {
	
	private String action;
	
	private String paramjson;
	
	public static BaseLog getBaseLog(String action,Object source,Map<String, Object> ext){
		if(null==source)
		  throw new IllegalArgumentException();
		else{
			try {
				Map<String, Object> map= ReflectUtil.objecttoMap(source);
				if(null!=ext&&!ext.isEmpty()){
					map.putAll(ext);
				}
				String json= JsonUtils.objectMapper.writeValueAsString(map);
				return new BaseLog(action, json);
			} catch (Exception e) {
				throw new IllegalArgumentException("this object's params not to the map");
			}
		}
	}

	@Override
	public String toString() {
		return "["+new Date().getTime()+"] ["+action+"] "+paramjson;
	}

	private BaseLog(){}
	   
	private BaseLog(String action ,String paramjson){
		this.action=action;
		this.paramjson=paramjson;
	}
	
}
