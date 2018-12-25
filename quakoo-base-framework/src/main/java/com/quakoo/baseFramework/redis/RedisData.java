package com.quakoo.baseFramework.redis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.quakoo.baseFramework.redis.util.JedisXSerializeUtil;
import redis.clients.util.SafeEncoder;

/**
 * 
 * @author LiYongbiao
 *
 */
public class RedisData {
	public static class RedisKeyMemObj{
		private String key;
		private Object mem;
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}
		public Object getMem() {
			return mem;
		}
		public void setMem(Object mem) {
			this.mem = mem;
		}
		@Override
		public String toString() {
			return "RedisKeyMemObj [key=" + key + ", mem=" + mem + "]";
		}
		public RedisKeyMemObj(String key, Object mem) {
			super();
			this.key = key;
			this.mem = mem;
		}
		
		
	}
	
	
	public static class RedisKeyMemStr{
		private String key;
		private String mem;
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}
		public String getMem() {
			return mem;
		}
		public void setMem(String mem) {
			this.mem = mem;
		}
		public RedisKeyMemStr(String key, String mem) {
			super();
			this.key = key;
			this.mem = mem;
		}
		
		@Override
		public String toString() {
			return "RedisKeyMemStr [key=" + key + ", mem=" + mem + "]";
		}
		
		
		
	}
	public static class RedisKeyMemByte{
		private String key;
		private byte[] mem;
		public RedisKeyMemByte(String key, byte[] mem) {
			super();
			this.key = key;
			this.mem = mem;
		}
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}
		public byte[] getMem() {
			return mem;
		}
		public void setMem(byte[] mem) {
			this.mem = mem;
		}
	
		@Override
		public String toString() {
			return "RedisKeyMemByte [key=" + key + ", mem="
					+ Arrays.toString(mem) + "]";
		}
		
		
	}
	
	public static class RedisKeyByteMemByte{
		private byte[] key;
		private byte[] mem;
		public RedisKeyByteMemByte(byte[] key, byte[] mem) {
			super();
			this.key = key;
			this.mem = mem;
		}
		@Override
		public String toString() {
			return "RedisKeyByteMemByte [key=" + Arrays.toString(key)
					+ ", mem=" + Arrays.toString(mem) + "]";
		}
		
		public byte[] getKey() {
			return key;
		}
		public void setKey(byte[] key) {
			this.key = key;
		}
		public byte[] getMem() {
			return mem;
		}
		public void setMem(byte[] mem) {
			this.mem = mem;
		}
		
		
	}
	
	
	public static RedisKeyMemObj getRedisKeyMemObj(RedisKeyMemByte keyMemstr){
		return new RedisKeyMemObj(keyMemstr.getKey(), JedisXSerializeUtil.decode(keyMemstr.getMem()));
	}
	
	
	public static RedisKeyMemByte getRedisKeyMemByte(RedisKeyMemStr keyMemstr){
		return new RedisKeyMemByte(keyMemstr.getKey(), SafeEncoder.encode(keyMemstr.getMem()));
	}
	
	public static RedisKeyMemStr getRedisKeyMemStr(RedisKeyMemByte keyMemstr){
		return new RedisKeyMemStr(keyMemstr.getKey(), SafeEncoder.encode(keyMemstr.getMem()));
	}
	
	

	public static RedisKeyMemByte getRedisKeyMemByte(RedisKeyByteMemByte keyMemstr){
		return new RedisKeyMemByte(SafeEncoder.encode(keyMemstr.getKey()), keyMemstr.getMem());
	}
	
	public static List<RedisKeyMemByte> getRedisKeyMemByteList(List<RedisKeyMemStr> keyMemstrs){
		List<RedisKeyMemByte>  result=new ArrayList<RedisData.RedisKeyMemByte>();
		for(RedisKeyMemStr keyMemstr:keyMemstrs){
			result.add(new RedisKeyMemByte(keyMemstr.getKey(), SafeEncoder.encode(keyMemstr.getMem())));
		}
		return result;
	}
	
	
	public static List<RedisKeyMemByte> getRedisKeyMemByteListFromObj(List<RedisKeyMemObj> keyMemstrs){
		List<RedisKeyMemByte>  result=new ArrayList<RedisData.RedisKeyMemByte>();
		for(RedisKeyMemObj keyMemObj:keyMemstrs){
			result.add(new RedisKeyMemByte(keyMemObj.getKey(), JedisXSerializeUtil.encode(keyMemObj.getMem())));
		}
		return result;
	}
	
	
	
	public static List<RedisKeyByteMemByte> getRedisKeyByteMemByteList(List<RedisKeyMemByte> keyMemstrs){
		List<RedisKeyByteMemByte>  result=new ArrayList<RedisData.RedisKeyByteMemByte>();
		for(RedisKeyMemByte keyMemstr:keyMemstrs){
			result.add(new RedisKeyByteMemByte(SafeEncoder.encode(keyMemstr.getKey()), keyMemstr.getMem()));
		}
		return result;
	}
	
	
	
	
	
}

