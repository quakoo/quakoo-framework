package com.quakoo.baseFramework.redis;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import redis.clients.jedis.JedisPoolConfig;

/**
 * 
 * @author LiYongbiao
 *
 */
public class JedisXFactory {

	public static Map<String,JedisX> map=new ConcurrentHashMap<String, JedisX>();
	
	public static JedisX getJedisX(String address, int timeout){
		JedisX jedisx=map.get(address);
		if(jedisx!=null){
			return jedisx;
		}else{
			synchronized (map) {
				JedisPoolConfig jedisPoolConfig=new JedisPoolConfig();
				//jedisPoolConfig.setMaxActive(1000);
				jedisPoolConfig.setMaxTotal(1000);
				jedisPoolConfig.setMaxIdle(100);
				//jedisPoolConfig.setMaxWait(2000);
				jedisPoolConfig.setMaxWaitMillis(2000);
				jedisPoolConfig.setTestOnBorrow(false);
				JedisBean jedisBean=new JedisBean();
				jedisBean.setMasterAddress(address);
				jedisx=new JedisX(jedisBean, jedisPoolConfig, timeout);
				map.put(address, jedisx);
				return jedisx;
			}
		}
	}
	
	
	public static JedisX getJedisX(String address,int maxActive,int maxIdle,int maxWait, int timeout){
		String key=address+maxActive+maxIdle+maxWait;
		JedisX jedisx=map.get(key);
		if(jedisx!=null){
			return jedisx;
		}else{
			synchronized (map) {
				JedisPoolConfig jedisPoolConfig=new JedisPoolConfig();
				jedisPoolConfig.setMaxTotal(1000);
				//jedisPoolConfig.setMaxActive(maxActive);
                //jedisPoolConfig.setWhenExhaustedAction(GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION);
				jedisPoolConfig.setMaxIdle(maxIdle);
                //jedisPoolConfig.setMaxWait(maxWait);
				jedisPoolConfig.setMaxWaitMillis(maxWait);
				jedisPoolConfig.setTestOnBorrow(false);
				JedisBean jedisBean=new JedisBean();
				jedisBean.setMasterAddress(address);
				jedisx=new JedisX(jedisBean, jedisPoolConfig, timeout);
				map.put(key, jedisx);
				return jedisx;
			}
		}
	}
	
}
