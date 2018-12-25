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
public class RedisSortData {
	public static class RedisKeySortMemObj{
		private String key;
		private Object mem;
		private double sort;
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
		public double getSort() {
			return sort;
		}
		public void setSort(double sort) {
			this.sort = sort;
		}
		public RedisKeySortMemObj(String key, Object mem, double sort) {
			super();
			this.key = key;
			this.mem = mem;
			this.sort = sort;
		}
		@Override
		public String toString() {
			return "RedisKeySortMemObj [key=" + key + ", mem=" + mem
					+ ", sort=" + sort + "]";
		}
		
		
		
		
		
	}
	
	
	public static class RedisKeySortMemStr{
		private String key;
		private String mem;
		private double sort;
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
		public double getSort() {
			return sort;
		}
		public void setSort(double sort) {
			this.sort = sort;
		}
		public RedisKeySortMemStr(String key, String mem, double sort) {
			super();
			this.key = key;
			this.mem = mem;
			this.sort = sort;
		}
		@Override
		public String toString() {
			return "RedisKeySortMemStr [key=" + key + ", mem=" + mem
					+ ", sort=" + sort + "]";
		}
		
		
		
		
	}
	public static class RedisKeySortMemByte{
		private String key;
		private byte[] mem;
		private double sort;
		@Override
		public String toString() {
			return "RedisKeySortMemByte [key=" + key + ", mem="
					+ Arrays.toString(mem) + ", sort=" + sort + "]";
		}
		public RedisKeySortMemByte(String key, byte[] mem, double sort) {
			super();
			this.key = key;
			this.mem = mem;
			this.sort = sort;
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
		public double getSort() {
			return sort;
		}
		public void setSort(double sort) {
			this.sort = sort;
		}
		
		
	}
	
	public static class RedisKeySortByteMemByte{
		private byte[] key;
		private byte[] mem;
		private double sort;
		@Override
		public String toString() {
			return "RedisKeyByteMemByte [key=" + Arrays.toString(key)
					+ ", mem=" + Arrays.toString(mem) + ", sort=" + sort + "]";
		}
		public RedisKeySortByteMemByte(byte[] key, byte[] mem, double sort) {
			super();
			this.key = key;
			this.mem = mem;
			this.sort = sort;
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
		public double getSort() {
			return sort;
		}
		public void setSort(double sort) {
			this.sort = sort;
		}
		
		
	}
	
	
	public static RedisKeySortMemObj getRedisKeySortMemObj(RedisKeySortMemByte KeySortMemstr){
		return new RedisKeySortMemObj(KeySortMemstr.getKey(), JedisXSerializeUtil.decode(KeySortMemstr.getMem()),KeySortMemstr.getSort());
	}
	
	
	public static RedisKeySortMemByte getRedisKeySortMemByte(RedisKeySortMemStr KeySortMemstr){
		return new RedisKeySortMemByte(KeySortMemstr.getKey(), SafeEncoder.encode(KeySortMemstr.getMem()),KeySortMemstr.getSort());
	}
	
	public static RedisKeySortMemStr getRedisKeySortMemStr(RedisKeySortMemByte KeySortMemstr){
		return new RedisKeySortMemStr(KeySortMemstr.getKey(), SafeEncoder.encode(KeySortMemstr.getMem()),KeySortMemstr.getSort());
	}
	
	

	public static RedisKeySortMemByte getRedisKeySortMemByte(RedisKeySortByteMemByte KeySortMemstr){
		return new RedisKeySortMemByte(SafeEncoder.encode(KeySortMemstr.getKey()), KeySortMemstr.getMem(),KeySortMemstr.getSort());
	}
	
	public static List<RedisKeySortMemByte> getRedisKeySortMemByteList(List<RedisKeySortMemStr> KeySortMemstrs){
		List<RedisKeySortMemByte>  result=new ArrayList<RedisSortData.RedisKeySortMemByte>();
		for(RedisKeySortMemStr KeySortMemstr:KeySortMemstrs){
			result.add(new RedisKeySortMemByte(KeySortMemstr.getKey(), SafeEncoder.encode(KeySortMemstr.getMem()),KeySortMemstr.getSort()));
		}
		return result;
	}
	
	
	public static List<RedisKeySortMemByte> getRedisKeySortMemByteListFromObj(List<RedisKeySortMemObj> KeySortMemstrs){
		List<RedisKeySortMemByte>  result=new ArrayList<RedisSortData.RedisKeySortMemByte>();
		for(RedisKeySortMemObj KeySortMemObj:KeySortMemstrs){
			result.add(new RedisKeySortMemByte(KeySortMemObj.getKey(), JedisXSerializeUtil.encode(KeySortMemObj.getMem()),KeySortMemObj.getSort()));
		}
		return result;
	}
	
	
	
	public static List<RedisKeySortByteMemByte> getRedisKeyByteMemByteList(List<RedisKeySortMemByte> KeySortMemstrs){
		List<RedisKeySortByteMemByte>  result=new ArrayList<RedisSortData.RedisKeySortByteMemByte>();
		for(RedisKeySortMemByte KeySortMemstr:KeySortMemstrs){
			result.add(new RedisKeySortByteMemByte(SafeEncoder.encode(KeySortMemstr.getKey()), KeySortMemstr.getMem(),KeySortMemstr.getSort()));
		}
		return result;
	}
	
	
	
	
	
}

