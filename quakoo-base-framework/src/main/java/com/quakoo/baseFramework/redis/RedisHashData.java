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
public class RedisHashData {
	public static class RedisKeyHashMemObj{
		private String key;
		private Object mem;
		private Object result;
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
		public Object getResult() {
			return result;
		}
		public void setResult(Object result) {
			this.result = result;
		}
		@Override
		public String toString() {
			return "RedisKeyHashMemObj [key=" + key + ", mem=" + mem
					+ ", result=" + result + "]";
		}
		public RedisKeyHashMemObj(String key, Object mem, Object result) {
			super();
			this.key = key;
			this.mem = mem;
			this.result = result;
		}
		
		
		
		
	}
	
	
	public static class RedisKeyHashMemStr{
		private String key;
		private String mem;
		private String result;
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
		public String getResult() {
			return result;
		}
		public void setResult(String result) {
			this.result = result;
		}
		@Override
		public String toString() {
			return "RedisKeyHashMemStr [key=" + key + ", mem=" + mem
					+ ", result=" + result + "]";
		}
		public RedisKeyHashMemStr(String key, String mem, String result) {
			super();
			this.key = key;
			this.mem = mem;
			this.result = result;
		}
		
		
		
		
	}
	public static class RedisKeyHashMemByte{
		private String key;
		private byte[] mem;
		private byte[] result;
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
		public byte[] getResult() {
			return result;
		}
		public void setResult(byte[] result) {
			this.result = result;
		}
		@Override
		public String toString() {
			return "RedisKeyHashMemByte [key=" + key + ", mem="
					+ Arrays.toString(mem) + ", result="
					+ Arrays.toString(result) + "]";
		}
		public RedisKeyHashMemByte(String key, byte[] mem, byte[] result) {
			super();
			this.key = key;
			this.mem = mem;
			this.result = result;
		}
		
		
	}
	
	public static class RedisKeyHashByteMemByte{
		private byte[] key;
		private byte[] mem;
		private byte[] result;
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
		public byte[] getResult() {
			return result;
		}
		public void setResult(byte[] result) {
			this.result = result;
		}
		@Override
		public String toString() {
			return "RedisKeyHashByteMemByte [key=" + Arrays.toString(key)
					+ ", mem=" + Arrays.toString(mem) + ", result="
					+ Arrays.toString(result) + "]";
		}
		public RedisKeyHashByteMemByte(byte[] key, byte[] mem, byte[] result) {
			super();
			this.key = key;
			this.mem = mem;
			this.result = result;
		}
		
		
	}
	
	
	public static RedisKeyHashMemObj getRedisKeyHashMemObj(RedisKeyHashMemByte KeyHashMemstr){
		return new RedisKeyHashMemObj(KeyHashMemstr.getKey(), JedisXSerializeUtil.decode(KeyHashMemstr.getMem()),JedisXSerializeUtil.decode(KeyHashMemstr.getResult()));
	}
	
	
	public static RedisKeyHashMemByte getRedisKeyHashMemByte(RedisKeyHashMemStr KeyHashMemstr){
		return new RedisKeyHashMemByte(KeyHashMemstr.getKey(), SafeEncoder.encode(KeyHashMemstr.getMem()),SafeEncoder.encode(KeyHashMemstr.getResult()));
	}
	
	public static RedisKeyHashMemStr getRedisKeyHashMemStr(RedisKeyHashMemByte KeyHashMemstr){
		return new RedisKeyHashMemStr(KeyHashMemstr.getKey(), SafeEncoder.encode(KeyHashMemstr.getMem()), SafeEncoder.encode(KeyHashMemstr.getResult()));
	}
	
	

	public static RedisKeyHashMemByte getRedisKeyHashMemByte(RedisKeyHashByteMemByte KeyHashMemstr){
		return new RedisKeyHashMemByte(SafeEncoder.encode(KeyHashMemstr.getKey()), KeyHashMemstr.getMem(),KeyHashMemstr.getResult());
	}
	
	public static List<RedisKeyHashMemByte> getRedisKeyHashMemByteList(List<RedisKeyHashMemStr> KeyHashMemstrs){
		List<RedisKeyHashMemByte>  result=new ArrayList<RedisHashData.RedisKeyHashMemByte>();
		for(RedisKeyHashMemStr KeyHashMemstr:KeyHashMemstrs){
			result.add(new RedisKeyHashMemByte(KeyHashMemstr.getKey(), SafeEncoder.encode(KeyHashMemstr.getMem()),SafeEncoder.encode(KeyHashMemstr.getResult())));
		}
		return result;
	}
	
	
	public static List<RedisKeyHashMemByte> getRedisKeyHashMemByteListFromObj(List<RedisKeyHashMemObj> KeyHashMemstrs){
		List<RedisKeyHashMemByte>  result=new ArrayList<RedisHashData.RedisKeyHashMemByte>();
		for(RedisKeyHashMemObj KeyHashMemObj:KeyHashMemstrs){
			result.add(new RedisKeyHashMemByte(KeyHashMemObj.getKey(), JedisXSerializeUtil.encode(KeyHashMemObj.getMem()),JedisXSerializeUtil.encode(KeyHashMemObj.getResult())));
		}
		return result;
	}
	
	
	
	public static List<RedisKeyHashByteMemByte> getRedisKeyHashByteMemByteList(List<RedisKeyHashMemByte> KeyHashMemstrs){
		List<RedisKeyHashByteMemByte>  result=new ArrayList<RedisHashData.RedisKeyHashByteMemByte>();
		for(RedisKeyHashMemByte KeyHashMemstr:KeyHashMemstrs){
			result.add(new RedisKeyHashByteMemByte(SafeEncoder.encode(KeyHashMemstr.getKey()), KeyHashMemstr.getMem(),KeyHashMemstr.getResult()));
		}
		return result;
	}
	
	
	
	
	
}

