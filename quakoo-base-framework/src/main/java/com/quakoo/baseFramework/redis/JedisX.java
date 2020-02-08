package com.quakoo.baseFramework.redis;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.redis.exception.JedisXValueNotSupportException;
import com.quakoo.baseFramework.redis.util.JedisXSerializeUtil;
import com.quakoo.baseFramework.redis.util.JedisXSerializeUtil;
import com.quakoo.baseFramework.serialize.ScloudSerializable;
import com.quakoo.baseFramework.serialize.ScloudSerializeUtil;
import com.quakoo.baseFramework.thread.NamedThreadFactory;
import com.quakoo.baseFramework.reflect.ReflectUtil;
import com.quakoo.baseFramework.redis.exception.JedisXValueNotSupportException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.*;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.util.Pool;
import redis.clients.util.SafeEncoder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

/**
 * @author yongbiaoli
 */
public class JedisX {
    protected static final Logger log = LoggerFactory.getLogger(JedisX.class);

    protected static final Logger monitorLog = LoggerFactory.getLogger("monitor");

    private final int defaultIndex = 0;

    private final JedisBean bean;

    private final JedisPoolConfig jedisPoolConfig;

    private final ShardedJedisPool shardedJedisPool;

    private int timeout = 30;

    private final ScheduledExecutorService scheduledExecutorService = Executors
            .newScheduledThreadPool(1);

    private  ExecutorService executorService =null; 

    // ============================初始化==================================

    public JedisX(JedisBean bean, JedisPoolConfig jedisPoolConfig, int timeout) {
        this.bean = bean;
        this.jedisPoolConfig = jedisPoolConfig;
        // timeout必须在sharedJedisPool之前设置
        this.timeout = timeout;
        this.shardedJedisPool = initialShardedPool();
        executorService = new ThreadPoolExecutor(bean.getThreadPoolCoreSize(), bean.getThreadPoolMaxSize(),
                20, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new NamedThreadFactory("jedisx"),
                new ThreadPoolExecutor.AbortPolicy());
        startPrintPoolInfo();
    }



    public void startPrintPoolInfo() {

        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    Field targetField = Pool.class
                            .getDeclaredField("internalPool");
                    targetField.setAccessible(true);
                    final GenericObjectPool internalPool = (GenericObjectPool) targetField
                            .get(shardedJedisPool);
                    if (internalPool != null) {
                    	monitorLog.info("ShardedJedisPool------maxActive:"
                                + internalPool.getMaxTotal() + ",active:"
                                + internalPool.getNumActive() + ",idle:"
                                + internalPool.getNumIdle());
                    }
                } catch (Exception e) {
                    log.error("print JedisX pool info error. reason:"
                            + e.getMessage());
                    // e.printStackTrace();
                }
            }

        }, 2, 1, TimeUnit.SECONDS);

    }

    public void stopPrintPoolInfo() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
    }

    public int getDBIndex(String key) {
        int retIndex = defaultIndex;
        Map<String, Integer> dbIndexMap = bean.getDbIndexMap();
        if (dbIndexMap != null) {
            String dbKey = key == null ? null : key.split("_")[0];
            Integer dbIndex = dbIndexMap.get(dbKey);
            if (dbIndex != null) {
                retIndex = dbIndex;
            }
        }
        return retIndex;
    }

    private ShardedJedisPool initialShardedPool() {
        String conn = bean.getMasterAddress();
        String password = bean.getPassword();
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        String addrs[] = conn.split(",");
        int length = addrs.length;
        for (int i = 0; i < length; i++) {
            String addr = addrs[i];
            String host = addr.split(":")[0];
            int port = Integer.parseInt(addr.split(":")[1]);
            JedisShardInfo info = new JedisShardInfo(host, port, timeout,
                    "redis-shard-" + addr);
            if (StringUtils.isNotEmpty(password))
                info.setPassword(password);
            shards.add(info);
        }
        return new ShardedJedisPool(jedisPoolConfig, shards);
    }

    // ============================初始化结束==================================
    public List<String> getAllRedisInfo() {
        ShardedJedis shardedJedis = null;
        List<String> allInfos = new ArrayList<>();
        try {
            shardedJedis = shardedJedisPool.getResource();
            Collection<Jedis> jedises = shardedJedis.getAllShards();
            for (Jedis jedis : jedises) {
                String info = jedis.info();
                allInfos.add(info);
            }
            return allInfos;
        } catch (Exception e) {
            if (shardedJedis != null) {
                shardedJedisPool.returnBrokenResource(shardedJedis);
                shardedJedis = null;
                shardedJedis = null;
            }
            log.error(e.getMessage(), e);
        } finally {
            onFinally(shardedJedis);
        }
        return allInfos;
    }
    
    private Map<String, List<RedisAddSetParam>> devideKeysAddSet(
    		List<RedisAddSetParam> params) {
    	Map<JedisShardInfo, List<RedisAddSetParam>> map = Maps.newHashMap();
    	Map<String, List<RedisAddSetParam>> result = Maps.newHashMap();
    	ShardedJedis shardJedis = null;
    	try {
    		 shardJedis = shardedJedisPool.getResource();
    		 for (RedisAddSetParam param : params) {
    			 JedisShardInfo jedisShardInfo = shardJedis.getShardInfo(param.getKey());
    			 List<RedisAddSetParam> keysList = map.get(jedisShardInfo);
    			 if (keysList == null) {
                     keysList = new ArrayList<RedisAddSetParam>();
                     map.put(jedisShardInfo, keysList);
                 }
    			 keysList.add(param);
    		 }
		} catch (Exception e) {
			if (shardJedis != null) {
	               shardedJedisPool.returnBrokenResource(shardJedis);
	               shardJedis = null;
	               shardJedis = null;
	           }
	           log.error(e.getMessage(), e);
		} finally {
			 onFinally(shardJedis);
		}
    	for (List<RedisAddSetParam> list : map.values()) {
            result.put(list.iterator().next().getKey(), list);
        }
        return result;
    }
    
    private Map<String, List<String>> devideKeysSet(List<String> params) {
    	Map<JedisShardInfo, List<String>> map = Maps.newHashMap();
    	Map<String, List<String>> result = Maps.newHashMap();
    	ShardedJedis shardJedis = null;
    	try {
      		 shardJedis = shardedJedisPool.getResource();
      		 for (String param : params) {
      			 JedisShardInfo jedisShardInfo = shardJedis.getShardInfo(param);
      			 List<String> keysList = map.get(jedisShardInfo);
      			 if (keysList == null) {
                       keysList = new ArrayList<String>();
                       map.put(jedisShardInfo, keysList);
                   }
      			 keysList.add(param);
      		 }
   		} catch (Exception e) {
   			if (shardJedis != null) {
                  shardedJedisPool.returnBrokenResource(shardJedis);
                  shardJedis = null;
                  shardJedis = null;
              }
              log.error(e.getMessage(), e);
   		} finally {
              onFinally(shardJedis);
        }
      	    for (List<String> list : map.values()) {
               result.put(list.iterator().next(), list);
           }
           return result;
    }
    
    private Map<String, List<RedisSortedSetParam>> devideKeysSortedSet(
    		List<RedisSortedSetParam> params) {
    	Map<JedisShardInfo, List<RedisSortedSetParam>> map = Maps.newHashMap();
    	Map<String, List<RedisSortedSetParam>> result = Maps.newHashMap();
    	ShardedJedis shardJedis = null;
    	try {
   		 shardJedis = shardedJedisPool.getResource();
   		 for (RedisSortedSetParam param : params) {
   			 JedisShardInfo jedisShardInfo = shardJedis.getShardInfo(param.getKey());
   			 List<RedisSortedSetParam> keysList = map.get(jedisShardInfo);
   			 if (keysList == null) {
                    keysList = new ArrayList<RedisSortedSetParam>();
                    map.put(jedisShardInfo, keysList);
                }
   			 keysList.add(param);
   		 }
		} catch (Exception e) {
			if (shardJedis != null) {
               shardedJedisPool.returnBrokenResource(shardJedis);
               shardJedis = null;
               shardJedis = null;
           }
           log.error(e.getMessage(), e);
		} finally {
           onFinally(shardJedis);
        }
   	    for (List<RedisSortedSetParam> list : map.values()) {
            result.put(list.iterator().next().getKey(), list);
        }
        return result;
    }
    
    private Map<String, List<RedisDecrParam>> devideKeysDecr(List<RedisDecrParam> params) {
    	Map<JedisShardInfo, List<RedisDecrParam>> map = Maps.newHashMap();
    	Map<String, List<RedisDecrParam>> result = Maps.newHashMap();
    	ShardedJedis shardJedis = null;
    	try {
    		 shardJedis = shardedJedisPool.getResource();
    		 for (RedisDecrParam param : params) {
    			 JedisShardInfo jedisShardInfo = shardJedis.getShardInfo(param.getKey());
    			 List<RedisDecrParam> keysList = map.get(jedisShardInfo);
    			 if (keysList == null) {
                     keysList = new ArrayList<RedisDecrParam>();
                     map.put(jedisShardInfo, keysList);
                 }
    			 keysList.add(param);
    		 }
		} catch (Exception e) {
			if (shardJedis != null) {
                shardedJedisPool.returnBrokenResource(shardJedis);
                shardJedis = null;
                shardJedis = null;
            }
            log.error(e.getMessage(), e);
		} finally {
            onFinally(shardJedis);
        }
    	for (List<RedisDecrParam> list : map.values()) {
             result.put(list.iterator().next().getKey(), list);
        }
    	return result;
    }

    private Map<String, List<RedisIncrParam>> devideKeysIncr(List<RedisIncrParam> params) {
    	Map<JedisShardInfo, List<RedisIncrParam>> map = Maps.newHashMap();
    	Map<String, List<RedisIncrParam>> result = Maps.newHashMap();
    	ShardedJedis shardJedis = null;
    	try {
    		 shardJedis = shardedJedisPool.getResource();
    		 for (RedisIncrParam param : params) {
    			 JedisShardInfo jedisShardInfo = shardJedis.getShardInfo(param.getKey());
    			 List<RedisIncrParam> keysList = map.get(jedisShardInfo);
    			 if (keysList == null) {
                     keysList = new ArrayList<RedisIncrParam>();
                     map.put(jedisShardInfo, keysList);
                 }
    			 keysList.add(param);
    		 }
		} catch (Exception e) {
			if (shardJedis != null) {
                shardedJedisPool.returnBrokenResource(shardJedis);
                shardJedis = null;
                shardJedis = null;
            }
            log.error(e.getMessage(), e);
		} finally {
            onFinally(shardJedis);
        }
    	for (List<RedisIncrParam> list : map.values()) {
             result.put(list.iterator().next().getKey(), list);
        }
    	return result;
    }
    
    /**
     * keys
     */
    private Map<String, List<String>> devideKeys(Collection<String> keys) {
        Map<JedisShardInfo, List<String>> map = new HashMap<JedisShardInfo, List<String>>();
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        ShardedJedis shardJedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            for (String key : keys) {
                JedisShardInfo jedisShardInfo = shardJedis.getShardInfo(key);
                List<String> keysList = map.get(jedisShardInfo);

                if (keysList == null) {
                    keysList = new ArrayList<String>();
                    map.put(jedisShardInfo, keysList);
                }
                keysList.add(key);
            }
        } catch (Exception e) {
            if (shardJedis != null) {
                shardedJedisPool.returnBrokenResource(shardJedis);
                shardJedis = null;
                shardJedis = null;
            }
            log.error(e.getMessage(), e);
        } finally {
            onFinally(shardJedis);
        }
        for (List<String> list : map.values()) {
            result.put(list.iterator().next(), list);
        }
        return result;
    }

    /******************************* below:expire ******************************/
    /**
     * 对key设置过期时间
     * <p/>
     * key expireSeconds Integer reply, specifically: 1: the timeout was set. 0:
     * the timeout was not set since the key already has an associated timeout
     * (this may happen only in Redis versions < 2.1.3, Redis >= 2.1.3 will
     * happily update the timeout), or the key does not exist.
     */

    public Long expire(String key, int expireSeconds) {

        return expire(key, SafeEncoder.encode(key), expireSeconds);
    }

    private void onException(ShardedJedis shardJedis, Jedis jedis, Exception e) {
        if (shardJedis != null) {
            shardedJedisPool.returnBrokenResource(shardJedis);
        }
        log.error(e.getMessage() + jedis.getClient().getHost() + ":"
                + jedis.getClient().getPort(), e);
    }

    private void onFinally(ShardedJedis shardJedis) {
        if (shardJedis != null) {
            shardedJedisPool.returnResource(shardJedis);
        }
    }

    private Long expire(String key, byte[] bytekey, int expireSeconds) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                Long ret = jedis.expire(bytekey, expireSeconds);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /****************************** above:expire *************************/

    /***************************** below:set ********************************************/
    /**
     * 缓存一个字符串。与getString(key)、multiGetString(List\<keys\>)配合使用,
     * 缓存成功后无法通过getObject(key)取出对象。
     * 若后续需要对此String进行append、incr、decr操作，则不能用setObject替换
     * <p/>
     * key expireSecond 过期时间，单位为秒（0和负数表示不设置过期） value "OK" or "failed"
     */

    public String setString(final String key, int expireSecond, String value) {
        return setByteArr(key, expireSecond, SafeEncoder.encode(value));
    }

    /**
     * 缓存一个对象。与getObject(key)、multiGetObject(List\<keys\>)配合使用
     * <p/>
     * key expireSecond 过期时间，单位为秒（0和负数表示不设置过期） value 可序列化对象（实现Serializable接口）
     * "OK" or "failed"
     */

    public String setObject(final String key, int expireSecond, Object value) {
        long start = System.currentTimeMillis();
        try {
            return setByteArr(key, expireSecond, serialize(value));
        } finally {
            log.info("set key:{},time:{}", key, System.currentTimeMillis() - start);
        }
    }

    /**
     * 缓存一个数据块。与getByteArr(key)、multiGetByteArr(List\<keys\>)配合使用
     * <p/>
     * key expireSecond 过期时间，单位为秒（0和负数表示不设置过期） value "OK" or "failed"
     */

    public String setByteArr(final String key, int expireSecond, byte[] value) {
        valueTypeAssert(value);
        return set(key, SafeEncoder.encode(key), expireSecond, value);
    }
    
    

    private String set(String key, final byte[] bytekey, int expireSecond,
                       byte[] value) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                String ret = null;
                if (expireSecond > 0) {
                    ret = jedis.setex(bytekey, expireSecond, value);
                } else {
                    ret = jedis.set(bytekey, value);
                }
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }

        return "failed";
    }

    /******************************** above:set ********************************************/

    /***************************** below:multiset *****************************************/
    /**
     * 缓存多个字符串。与getString(key)。multiGetString(List\<keys\>)配合使用。
     * 若后续需要对此String进行append、incr、decr操作，则不能用setObject替换
     * <p/>
     * keyValue expireSecond 过期时间，单位为秒（0和负数表示不设置过期） "OK" or "failed"
     */

    public String multiSetString(final Map<String, String> keyValue,
                                 int expireSecond) {
        Map<String, byte[]> keyValuebMap = new HashMap<String, byte[]>();
        for (Map.Entry<String,String> entry : keyValue.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                keyValuebMap.put(entry.getKey(), SafeEncoder.encode(value));
            }
        }
        return multiSet(keyValuebMap, expireSecond);
    }

    /**
     * 缓存多个对象。与getObject(key)、multiGetObject(List\<keys\>)配合使用
     * <p/>
     * keyValue key与可序列化对象（实现Serializable接口）的映射Map expireSecond
     * 过期时间，单位为秒（0和负数表示不设置过期） "OK" or "failed"
     */

    public String multiSetObject(Map<String, Object> keyValue, int expireSecond) {
        long start = System.currentTimeMillis();
        try {
            Map<String, byte[]> keyValuebMap = new HashMap<String, byte[]>();
            for (Map.Entry<String,Object> entry : keyValue.entrySet()) {
                Object value = entry.getValue();
                if (value != null) {
                    keyValuebMap.put(entry.getKey(), serialize(value));
                }
            }
            return multiSet(keyValuebMap, expireSecond);
        } finally {
            log.info("mset key:{},time:{}", keyValue.size(), System.currentTimeMillis() - start);
        }
    }

    /**
     * 缓存多个数据块。与getByteArr(key)、multiGetByteArr(List\<keys\>)配合使用
     * <p/>
     * keyValue key与可序列化对象（实现Serializable接口）的映射Map expireSecond
     * 过期时间，单位为秒（0和负数表示不设置过期） "OK" or "failed"
     */

    public String multiSetByteArr(Map<String, byte[]> keyValue, int expireSecond) {
        Map<String, byte[]> keyValuebMap = new HashMap<String, byte[]>();
        for (Map.Entry<String,byte[]> entry : keyValue.entrySet()) {
            byte[] value = entry.getValue();
            if (value != null) {
                keyValuebMap.put(entry.getKey(), value);
            }
        }
        return multiSet(keyValuebMap, expireSecond);
    }

    private String multiSet(Map<String, byte[]> keyValuebMap, int expireSecond) {
        String ret = null;
        Map<String, List<String>> nodeToKeys = devideKeys(keyValuebMap.keySet());
        for (Map.Entry<String,List<String>> entry : nodeToKeys.entrySet()) {
            List<String> thisKeys = entry.getValue();
            if (thisKeys != null && thisKeys.size() > 0) {
                ret = multiSet(entry.getKey(), expireSecond,
                        getKeyValueBArrArr(thisKeys, keyValuebMap));
            }
        }
        return ret;
    }

    private String multiSet(String key, int expireSecond,
                            final byte[]... keyValues) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                String ret = null;
                ret = jedis.mset(keyValues);
                if (expireSecond > 0) { // it's
                    for (int i = 0; i < keyValues.length; i += 2) {
                        jedis.expire(keyValues[i], expireSecond);
                    }
                }
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return "failed";
    }

    /******************************** above:multiset *****************************************/

    /**
     * *****************************below:setIfNotExist*************************
     * **********
     */
    /**
     * 类似于setString，不同的是只有在此key不存在时才缓存成功
     * <p/>
     * key expireSecond value 1表示缓存成功，0表示key已存在缓存不成功，-1表示服务器异常
     */

    public long setStringIfNotExist(final String key, int expireSecond,
                                    String value) {
        return setByteArrIfNotExist(key, expireSecond,
                SafeEncoder.encode(value));
    }

    /**
     * 类似于setObject，不同的是只有在此key不存在时才缓存成功
     * <p/>
     * key expireSecond value 1表示缓存成功，0表示key已存在缓存不成功，-1表示服务器异常
     */

    public long setObjectIfNotExist(final String key, int expireSecond,
                                    Object value) {
        return setByteArrIfNotExist(key, expireSecond, serialize(value));
    }

    /**
     * 类似于setByteArr，不同的是只有在此key不存在时才缓存成功
     * <p/>
     * key expireSecond value 1表示缓存成功，0表示key已存在缓存不成功，-1表示服务器异常
     */

    public long setByteArrIfNotExist(final String key, int expireSecond,
                                     byte[] value) {
        valueTypeAssert(value);
        return setIfNotExist(key, SafeEncoder.encode(key), expireSecond, value);

    }

    private long setIfNotExist(String key, final byte[] bytekey,
                               int expireSecond, byte[] value) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.setnx(bytekey, value);
                if (expireSecond > 0) {
                    jedis.expire(bytekey, expireSecond);
                }
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return -1L;
    }

    /******************************** above:setIfNotExist *********************************/

    /***************************** below:multisetInNotExit *****************************/
    /**
     * 类似于multiSetString，不同的是只有在此key不存在时才缓存成功
     * <p/>
     * keyValue expireSecond 返回非负数值，表示本次缓存成功k-v数;返回负数值，表示服务端异常
     */

    public long multiSetStringIfNotExist(final Map<String, String> keyValue,
                                         int expireSecond) {
        Map<String, byte[]> keyValuebMap = new HashMap<String, byte[]>();
        for (Map.Entry<String,String> entry : keyValue.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                keyValuebMap.put(entry.getKey(), SafeEncoder.encode(value));
            }
        }
        return multiSetIfNotExist(keyValuebMap, expireSecond);
    }

    /**
     * 类似于multiSetObject，不同的是只有在此key不存在时才缓存成功
     * <p/>
     * keyValue expireSecond 返回非负数值，表示本次缓存成功k-v数;返回负数值，表示服务端异常
     */

    public long multiSetObjectIfNotExist(Map<String, Object> keyValue,
                                         int expireSecond) {
        Map<String, byte[]> keyValuebMap = new HashMap<String, byte[]>();
        for (Map.Entry<String,Object> entry : keyValue.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                keyValuebMap.put(entry.getKey(), serialize(value));
            }
        }
        return multiSetIfNotExist(keyValuebMap, expireSecond);
    }

    /**
     * 类似于multiSetByteArr，不同的是只有在此key不存在时才缓存成功
     * <p/>
     * keyValue expireSecond 返回非负数值，表示本次缓存成功k-v数;返回负数值，表示服务端异常
     */

    public long multiSetByteArrIfNotExist(Map<String, byte[]> keyValue,
                                          int expireSecond) {
        Map<String, byte[]> keyValuebMap = new HashMap<String, byte[]>();
        for (Map.Entry<String,byte[]> entry : keyValue.entrySet()) {
            byte[] value = entry.getValue();
            if (value != null) {
                keyValuebMap.put(entry.getKey(), value);
            }
        }
        return multiSetIfNotExist(keyValuebMap, expireSecond);
    }

    private long multiSetIfNotExist(Map<String, byte[]> keyValuebMap,
                                    int expireSecond) {
        long ret = 0;
        Map<String, List<String>> nodeToKeys = devideKeys(keyValuebMap.keySet());
        for (Map.Entry<String,List<String>> entry : nodeToKeys.entrySet()) {
            List<String> thisKeys =entry.getValue();
            if (thisKeys != null && thisKeys.size() > 0) {
                ret += multiSetIfNotExist(entry.getKey(), expireSecond,
                        getKeyValueBArrArr(thisKeys, keyValuebMap));
            }
        }
        return ret;
    }

    private long multiSetIfNotExist(String key, int expireSecond,
                                    final byte[]... keyValues) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                long ret = jedis.msetnx(keyValues);
                if (expireSecond > 0) { // it's
                    for (int i = 0; i < keyValues.length; i += 2) {
                        jedis.expire(keyValues[i], expireSecond);
                    }
                }
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return -1L;
    }

    /**
     * ***************************** above:multisetIfNotExit
     * *******************************
     */

    public String getRedisInfo(String key) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                return jedis.getClient().getHost() + ":"
                        + jedis.getClient().getPort();
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /***************************** below:get *******************************************/
    /**
     * 取出缓存的字符串，与setString()配合使用。.
     * 需要注意的时通过setObject(key,str)之类的接口缓存的字符串对象，无法通过此接口取出正确数据
     * <p/>
     * key
     */

    public String getString(String key) {
        byte[] ret = get(key, SafeEncoder.encode(key));
        if (ret != null) {
            return SafeEncoder.encode(ret);
        }
        return null;

    }
    /***************************** below:keys *******************************************/
    /**
     * 模糊匹配取出缓存的字符串，与setString()配合使用。.
     * 需要注意的时通过setObject(key,str)之类的接口缓存的字符串对象，无法通过此接口取出正确数据
     * <p/>
     * key
     */

    public Set<String> keys(String key) {
    	Set<String> set = Sets.newHashSet();
    	Set<byte[]> ret = keys(key, SafeEncoder.encode(key));
    	for(byte[] b : ret){
    		if(b != null)
    			set.add(SafeEncoder.encode(b));
    	}
    	return set;
    }
    /**
     * 取出缓存的字符串，与setObject()配合使用。.
     * 需要注意的时通过setString(key,str)之类的接口缓存的字符串对象，无法通过此接口取出正确数据
     * <p/>
     * key
     */

    public Object getObject(String key, Class<? extends ScloudSerializable> clazz) {
        long start = System.currentTimeMillis();
        try {
            byte[] ret = get(key, SafeEncoder.encode(key));
            if (ret != null) {
                return deserialize(ret, clazz);
            }
            return null;
        } finally {
            log.info("getObject key:{},time:{}", new Object[]{key, System.currentTimeMillis() - start});
        }
    }

    public Object getObject(final String key, long time, final Class<? extends ScloudSerializable> clazz) {
        FutureTask<Object> future = new FutureTask<Object>(
                new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return getObject(key, clazz);
                    }
                });
        executorService.execute(future);
        try {
            return future.get(time, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Jedis jedis = null;
            ShardedJedis shardJedis = null;
            try {
                shardJedis = shardedJedisPool.getResource();
                jedis = shardJedis.getShard(key);
                log.error(key + ":" + jedis.getClient().getHost() + ":"
                        + jedis.getClient().getPort(), e);
            } catch (Exception ex) {
            } finally {
                if (shardJedis != null) {
                    shardedJedisPool.returnResource(shardJedis);
                }
            }

        }
        return null;
    }
    
    public long ttl(String key) {
    	long ret = ttl(key, SafeEncoder.encode(key));
        return ret;
    }
    
    private long ttl(String key, byte[] bytekey) {
    	ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                long ret = jedis.ttl(bytekey);
                return ret;
            }
        } catch (Exception e) {
            if (shardJedis != null) {
                shardedJedisPool.returnBrokenResource(shardJedis);
                shardJedis = null;
            }
            if (jedis != null) {
                log.error("JdedisX.get() " + key + " " + e.getMessage()
                        + jedis.getClient().getHost() + ":"
                        + jedis.getClient().getPort(), e);
            } else {
                log.error("JdedisX.get() " + e.getMessage());
            }
        } finally {
            onFinally(shardJedis);
        }
        return -3l;
    }

    /**
     * 取出缓存的字符串，与setByteArr()配合使用。.
     * <p/>
     * key
     */

    public byte[] getByteArr(String key) {
        byte[] ret = get(key, SafeEncoder.encode(key));
        return ret;
    }
    private Set<byte[]> keys(String key, byte[] bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
            	Set<byte[]> ret = jedis.keys(bytekey);
            	return ret;
            }
        }catch (Exception e) {
            if (shardJedis != null) {
                shardedJedisPool.returnBrokenResource(shardJedis);
                shardJedis = null;
            }
            if (jedis != null) {
                log.error("JdedisX.get() " + key + " " + e.getMessage()
                        + jedis.getClient().getHost() + ":"
                        + jedis.getClient().getPort(), e);

            } else {
                log.error("JdedisX.get() " + e.getMessage());
            }
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }
    private byte[] get(String key, byte[] bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                byte[] ret = jedis.get(bytekey);
                return ret;
            }
        } catch (Exception e) {
            if (shardJedis != null) {
                shardedJedisPool.returnBrokenResource(shardJedis);
                shardJedis = null;
            }
            if (jedis != null) {
                log.error("JdedisX.get() " + key + " " + e.getMessage()
                        + jedis.getClient().getHost() + ":"
                        + jedis.getClient().getPort(), e);

            } else {
                log.error("JdedisX.get() " + e.getMessage());
            }
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /******************************** above:get ********************************************/

    /**
     * *****************************below:multiget******************************
     * **********
     */
    /**
     * 一次取出多个缓存的String对象
     * <p/>
     * keys
     */

    public Map<String, String> multiGetString(List<String> keys) {
        Map<String, String> ret = new HashMap<String, String>();
        Map<String, byte[]> temp = multiGetByteArr(keys);
        for (Map.Entry<String,byte[]> entry : temp.entrySet()) {
            byte[] value = entry.getValue();
            if (value != null) {
                ret.put(entry.getKey(), SafeEncoder.encode(value));
            } else {
                ret.put(entry.getKey(), null);
            }
        }
        return ret;
    }

    /**
     * 一次取出多个缓存的Object
     * <p/>
     * keys
     */

    public Map<String, Object> multiGetObject(List<String> keys, Class<? extends ScloudSerializable> clazz) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> ret = new HashMap<String, Object>();
            Map<String, byte[]> temp = multiGetByteArr(keys);
            for (Map.Entry<String,byte[]> entry : temp.entrySet()) {
                byte[] value = entry.getValue();
                if (value != null) {
                    ret.put(entry.getKey(), deserialize(value, clazz));
                } else {
                    ret.put(entry.getKey(), null);
                }
            }
            return ret;
        } finally {
            log.info("getObject keys'size:{},keys:{},time:{}", new Object[]{keys.size(), keys.toString(), System.currentTimeMillis() - start});
        }
    }

    public Map<String, Object> multiGetObject(final List<String> keys, long time, final Class<? extends ScloudSerializable> clazz) {
        FutureTask<Map<String, Object>> future = new FutureTask<Map<String, Object>>(
                new Callable<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> call() throws Exception {
                        return multiGetObject(keys, clazz);
                    }
                });
        executorService.execute(future);
        try {
            return future.get(time, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("multiGetObject error", e);
        }
        return new HashMap<String, Object>();
    }

    /**
     * 一次取出多个缓存的数据块
     *
     * keys
     *
     */

    // public Map<String, byte[]> multiGetByteArr(List<String> keys) {
    // Map<String, byte[]> ret = new HashMap<String, byte[]>();
    // Map<String, List<String>> nodeToKeys = devideKeys(keys);
    // for (final String key : nodeToKeys.keySet()) {
    // List<String> thisKeys = nodeToKeys.get(key);
    // if (thisKeys != null && thisKeys.size() > 0) {
    // Map<String, byte[]> thisret = multiGet(key, getBArrArr(thisKeys));
    // if (thisret != null) {
    // ret.putAll(thisret);
    // }
    // }
    // }
    //
    // return ret;
    // }

    /**
     * 一次取出多个缓存的数据块，多线程取
     * <p/>
     * keys
     */

    public Map<String, byte[]> multiGetByteArr(List<String> keys) {
        final Map<String, byte[]> ret = new ConcurrentHashMap<String, byte[]>();
        final Map<String, List<String>> nodeToKeys = devideKeys(keys);
        final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet()
                .size());

        for (final String key : nodeToKeys.keySet()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<String> thisKeys = nodeToKeys.get(key);
                        if (thisKeys != null && thisKeys.size() > 0) {
                            Map<String, byte[]> thisret = multiGet(key,
                                    getBArrArr(thisKeys));
                            if (thisret != null) {
                                ret.putAll(thisret);
                            }
                        }
                    } finally {
                        cdl.countDown();
                    }
                }
            });

        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return ret;
    }

    private Map<String, byte[]> multiGet(String key, byte[]... keys) {
        Map<String, byte[]> map = new HashMap<String, byte[]>();
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                List<byte[]> ret = jedis.mget(keys);
                for (int i = ret.size() - 1; i >= 0; i--) {
                    if (ret.get(i) != null)
                        map.put(SafeEncoder.encode(keys[i]), ret.get(i));
                }
                return map;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return map;
    }

    /******************************** above:multiget *****************************************/

    /***************************** below:exists *****************************************/
    /**
     * 查询某key是否存在。无论是通过setString,还是setObject，setByteArr等任何接口缓存了数据，只要数据仍在有效期内，
     * 有将返回true
     * <p/>
     * key
     */

    public Boolean exists(String key) {
        boolean result = exists(key, SafeEncoder.encode(key));
        return result;
    }

    private Boolean exists(String key, byte[] bytekey) {

        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Boolean ret = jedis.exists(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return false;
    }

    /******************************** above:exists ********************************************/

    /**
     * *****************************below:delete,multidelte*********************
     * *************
     */
    /**
     * 删除某个缓存k-v对
     * <p/>
     * key 0表示key不存在或未删除成功，1表示key存在并删除成功
     */

    public long delete(String key) {
        long start = System.currentTimeMillis();
        try {
            return delete(key, SafeEncoder.encode(key));
        } finally {
            log.info("delte key:{},time:{}", key, System.currentTimeMillis() - start);
        }
    }

    public long deleteWithSharding(String shardingKey, String key) {

        return delete(shardingKey, key, SafeEncoder.encode(key));

    }

    /**
     * 一次删除多个k-v对
     * <p/>
     * keys 成功删除的k-v对的数目
     */

    public long multiDelete(List<String> keys) {
        Map<String, List<String>> nodeToKeys = devideKeys(keys);
        long ret = 0;
        for (Map.Entry<String,List<String>> entry : nodeToKeys.entrySet()) {
            List<String> thisKeys = entry.getValue();
            if (thisKeys != null && thisKeys.size() > 0) {
                ret += delete(entry.getKey(), getBArrArr(thisKeys));
            }
        }
        return ret;
    }

    private long delete(String shardingKey, String key, byte[]... bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                long ret = jedis.del(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0;
    }

    private long delete(String key, byte[]... bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                long ret = jedis.del(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0;
    }

    /******************************** above:delete,multidelete *****************************/

    /******************************* below:inc ******************************************/
    /**
     * 对key做自增1。<br/>
     * 如果key本不存在，则在执行此操作前设置默认值为0<br/>
     * 如果key存在，且key是十进制数值的字符串表示(e.g: "-123")，则在些数据基础上自增(e.g:return -122L)<br/>
     * 如果key存在，且key不是十进制数值的字符串表示，则返回null
     * <p/>
     * key 自增后的Long值。如果操作失败返回null
     */

    public Long incr(String key) {
        return incrBy(key, 1);
    }

    /**
     * 类似于incr(key).此方法可设置自增步长
     * <p/>
     * key step 自增后的Long值.如果操作失败返回null
     */

    public Long incrBy(String key, long step) {

        return incrBy(key, SafeEncoder.encode(key), step);

    }

    private Long incrBy(String key, byte[] bytekey, long step) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.incrBy(bytekey, step);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /**
     * 取得记录incr或desr的当前值
     * <p/>
     * key null表示不存在, 数值表示此key的当前值
     */

    public Long getNumberRecordIncrOrDesr(String key) {
        String ret = getString(key);
        if (ret != null) {
            try {
                long num = Long.parseLong(ret);
                return num;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /******************************** above:inc ********************************************/

    /******************************* below:desr ********************************************/
    /**
     * 对key做自减1。<br/>
     * 如果key本不存在，则在执行此操作前设置默认值为0 如果key存在，且key是十进制数值的字符串表示(e.g:
     * "-123")，则在些数据基础上自减(e.g:return -124L)<br/>
     * 如果key存在，且key不是十进制数值的字符串表示，则返回null
     * <p/>
     * key 自减后的Long值，可为负数。如果操作失败返回null
     */

    public Long decr(String key) {
        return decrBy(key, 1);
    }

    /**
     * 类似于decr(key).此方法可设置自减步长
     * <p/>
     * key step 自减后的Long值.如果操作失败返回null
     */

    public Long decrBy(String key, long step) {

        return decrBy(key, SafeEncoder.encode(key), step);

    }

    private Long decrBy(String key, byte[] bytekey, long step) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.decrBy(bytekey, step);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /******************************** above:desc ********************************************/

    /******************************* below:append ***************************************/
    /**
     * 对原value后进行数据追加
     * <p/>
     * key appendStr 追加后的value总字节数
     */

    public Long append(String key, String appendStr) {
        return append(key, SafeEncoder.encode(appendStr));
    }

    /**
     * 对原value后进行数据追加
     * <p/>
     * key appendBytes 追加后的value总字节数
     */

    public Long append(String key, byte[] appendBytes) {
        valueTypeAssert(appendBytes);

        return append(key, SafeEncoder.encode(key), appendBytes);

    }

    private Long append(String key, byte[] bytekey, byte[] appendBytes) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.append(bytekey, appendBytes);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /******************************** above:append ***************************************/

    /******************************** below:getSet ***************************************/
    /**
     * set a new str-value, and return the old str-value
     * <p/>
     * key expireSecond value
     */

    public String getSetString(String key, int expireSecond, String value) {
        valueTypeAssert(value);

        byte[] ret = getSet(key, SafeEncoder.encode(key), expireSecond,
                SafeEncoder.encode(value));
        if (ret != null) {
            return SafeEncoder.encode(ret);
        }
        return null;
    }

    /**
     * set a new Object-value, and return a old Object-value
     * <p/>
     * key expireSecond value
     */

    public Object getSetObject(String key, int expireSecond, Object value, final Class<? extends ScloudSerializable> clazz) {
        valueTypeAssert(value);

        byte[] ret = getSet(key, SafeEncoder.encode(key), expireSecond,
                serialize(value));
        if (ret != null) {
            return deserialize(ret, clazz);
        }

        return null;
    }

    /**
     * set a new [B-value, and return a old [B-value
     * <p/>
     * key expireSecond value
     */

    public byte[] getSetByteArr(String key, int expireSecond, byte[] value) {
        valueTypeAssert(value);
        return getSet(key, SafeEncoder.encode(key), expireSecond, value);

    }

    private byte[] getSet(String key, byte[] bytekey, int expireSecond,
                          byte[] value) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                byte[] ret = jedis.getSet(bytekey, value);
                if (expireSecond > 0) {
                    jedis.expire(bytekey, expireSecond);
                }
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /******************************** above:getSet ***************************************/

    /******************************* below:hash hSet ************************************/
    /**
     * 增加或更新hashmap的某一数据项（String类型）。<br/>
     * 不可针对某数据项单独设置过期时间，可对整个hashmap使用expire(key,time)设置过期时间
     * <p/>
     * key field value 0 成功更新数据项，1成功新数据项，-1操作失败
     */

    public long hSetString(String key, String field, String value) {
        valueTypeAssert(value);

        return hSet(key, SafeEncoder.encode(key), SafeEncoder.encode(field),
                SafeEncoder.encode(value));

    }

    /**
     * 增加或更新hashmap的某一数据项（Object）。<br/>
     * 不可针对某数据项单独设置过期时间，可对整个hashmap使用expire(key,time)设置过期时间
     * <p/>
     * key field value 0 成功更新数据项，1成功新数据项，-1操作失败
     */

    public long hSetObject(String key, String field, Object value) {
        valueTypeAssert(value);

        return hSet(key, SafeEncoder.encode(key), SafeEncoder.encode(field),
                serialize(value));

    }

    /**
     * 增加或更新hashmap的某一数据项（byte[]数据块）。<br/>
     * 不可针对某数据项单独设置过期时间，可对整个hashmap使用expire(key,time)设置过期时间
     * <p/>
     * key field value 0 成功更新数据项，1成功新数据项，-1操作失败
     */

    public long hSetByteArr(String key, String field, byte[] value) {
        valueTypeAssert(value);

        return hSet(key, SafeEncoder.encode(key), SafeEncoder.encode(field),
                value);

    }

    private long hSet(String key, byte[] bytekey, byte[] field, byte[] value) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.hset(bytekey, field, value);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return -1L;
    }

    /******************************** above:hash hSet ***************************************/

    /******************************* below:hash hMultiSet ***************************************/
    /**
     * 同时更新某hashmap下的若干个数据项（String）。<br/>
     * 不可针对某数据项单独设置过期时间，可对整个hashmap使用expire(key,time)设置过期时间
     * <p/>
     * key fieldValues "OK" 更新成功，“failed”失败
     */

    public String hMultiSetString(String key, Map<String, String> fieldValues) {
        Map<byte[], byte[]> fieldbValuebMap = new HashMap<byte[], byte[]>();
        for (Map.Entry<String,String> entry : fieldValues.entrySet()) {
            String value =entry.getValue();
            if (value != null) {
                fieldbValuebMap.put(SafeEncoder.encode(entry.getKey()),
                        SafeEncoder.encode(value));
            }
        }
        return hMultiSet(key, fieldbValuebMap);
    }

    /**
     * 同时更新某hashmap下的若干个数据项（Object）。<br/>
     * 不可针对某数据项单独设置过期时间，可对整个hashmap使用expire(key,time)设置过期时间
     * <p/>
     * key fieldValues "OK" 更新成功，“failed”失败
     */

    public String hMultiSetObject(String key, Map<String, Object> fieldValues) {
        Map<byte[], byte[]> fieldbValuebMap = new HashMap<byte[], byte[]>();
        for (Map.Entry<String,Object> entry : fieldValues.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                fieldbValuebMap
                        .put(SafeEncoder.encode(entry.getKey()), serialize(value));
            }
        }
        return hMultiSet(key, fieldbValuebMap);
    }

    /**
     * 同时更新某hasmap下的若干个数据项（byte[]数据块）。<br/>
     * 不可针对某数据项单独设置过期时间，可对整个hashmap使用expire(key,time)设置过期时间
     * <p/>
     * key fieldValues "OK" 更新成功，“failed”失败
     */

    public String hMultiSetByteArr(String key, Map<String, byte[]> fieldValues) {
        Map<byte[], byte[]> fieldbValuebMap = new HashMap<byte[], byte[]>();
        for (Map.Entry<String,byte[]> entry : fieldValues.entrySet()) {
            byte[] value = entry.getValue();
            if (value != null) {
                fieldbValuebMap.put(SafeEncoder.encode(entry.getKey()), value);
            }
        }
        return hMultiSet(key, fieldbValuebMap);
    }

    private String hMultiSet(String key, Map<byte[], byte[]> fieldbValuebMap) {

        return hMultiSet(key, SafeEncoder.encode(key), fieldbValuebMap);

    }

    private String hMultiSet(String key, byte[] bytekey,
                             Map<byte[], byte[]> hash) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                String ret = jedis.hmset(bytekey, hash);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return "failed";
    }

    /******************************** above:hash hMultiSet ***************************************/

    /******************************* below:hash hGet ***************************************/
    /**
     * 以字符串方式取出某hashmap下的某数据项
     * <p/>
     * key field
     */

    public String hGetString(String key, String field) {
        byte[] ret = hGetByteArr(key, field);
        if (ret != null) {
            return SafeEncoder.encode(ret);
        }
        return null;
    }

    /**
     * 以Object方式取出某hashmap下的某数据项
     * <p/>
     * key field
     */

    public Object hGetObject(String key, String field, final Class<? extends ScloudSerializable> clazz) {
        byte[] ret = hGetByteArr(key, field);
        if (ret != null) {
            return deserialize(ret, clazz);
        }
        return null;
    }

    /**
     * 以数据块的方式 取出某hashmap下的某数据项
     * <p/>
     * key field
     */

    public byte[] hGetByteArr(String key, String field) {

        return hGet(key, SafeEncoder.encode(key), SafeEncoder.encode(field));

    }

    private byte[] hGet(String key, byte[] bytekey, byte[] field) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                byte[] ret = jedis.hget(bytekey, field);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /******************************** above:hash hGet ***************************************/

    /******************************* below:hash hGetAll ***************************************/
    /**
     * 以字符串的方式 取出某hashmap下的所有数据项
     * <p/>
     * key
     */

    public Map<String, String> hGetAllString(String key) {
        Map<String, byte[]> temp = hGetAllByteArr(key);
        if (temp != null) {
            Map<String, String> ret = new HashMap<String, String>();
            for (Map.Entry<String,byte[]> entry : temp.entrySet()) {
                byte[] value = entry.getValue();
                if (value != null) {
                    ret.put(entry.getKey(), SafeEncoder.encode(entry.getValue()));
                }
            }
            return ret;
        }
        return null;
    }

    /**
     * 以Object的方式 取出某hashmap下的所有数据项
     * <p/>
     * key
     */

    public Map<String, Object> hGetAllObject(String key, final Class<? extends ScloudSerializable> clazz) {
        Map<String, byte[]> temp = hGetAllByteArr(key);
        if (temp != null) {
            Map<String, Object> ret = new HashMap<String, Object>();
            for (Map.Entry<String,byte[]> entry: temp.entrySet()) {
                byte[] value = entry.getValue();
                if (value != null) {
                    ret.put(entry.getKey(), deserialize(entry.getValue(), clazz));
                }
            }
            return ret;
        }
        return null;
    }

    /**
     * 以数据块的方式 取出某hashmap下的所有数据项
     * <p/>
     * key
     */

    public Map<String, byte[]> hGetAllByteArr(String key) {

        return hGetAll(key, SafeEncoder.encode(key));

    }

    private Map<String, byte[]> hGetAll(String key, byte[] bytekey) {
        Map<String, byte[]> map = new HashMap<String, byte[]>();
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Map<byte[], byte[]> ret = jedis.hgetAll(bytekey);
                for (Map.Entry<byte[], byte[]> item : ret.entrySet()) {
                    map.put(SafeEncoder.encode(item.getKey()), item.getValue());
                }
                return map;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /******************************** above:hash hGetAll ***************************************/

    /******************************* below:hash hMultiGet **************************************/
    /**
     * 以字符串的方式 取出某hashmap下的多个数据项
     * <p/>
     * key fields
     */

    public Map<String, String> hMultiGetString(String key, List<String> fields) {
        Map<String, byte[]> temp = hMultiGetByteArr(key, fields);
        if (temp != null) {
            Map<String, String> ret = new HashMap<String, String>();
            for (Map.Entry<String,byte[]> entry : temp.entrySet()) {
                byte[] value = entry.getValue();
                if (value != null) {
                    ret.put(entry.getKey(), SafeEncoder.encode(entry.getValue()));
                }
            }
            return ret;
        }
        return null;
    }

    /**
     * 以Object的方式 取出某hashmap下的多个数据项
     * <p/>
     * key fields
     */

    public Map<String, Object> hMultiGetObject(String key, List<String> fields, final Class<? extends ScloudSerializable> clazz) {
        Map<String, byte[]> temp = hMultiGetByteArr(key, fields);
        if (temp != null) {
            Map<String, Object> ret = new HashMap<String, Object>();
            for (Map.Entry<String,byte[]> entry : temp.entrySet()) {
                byte[] value = entry.getValue();
                if (value != null) {
                    ret.put(entry.getKey(), deserialize(entry.getValue(), clazz));
                }
            }
            return ret;
        }
        return null;
    }

    /**
     * 以byte[]数据块的方式 取出某hashmap下的多个数据项
     * <p/>
     * key fields
     */

    public Map<String, byte[]> hMultiGetByteArr(String key, List<String> fields) {

        return hMultiGet(key, SafeEncoder.encode(key), getBArrArr(fields));

    }

    private Map<String, byte[]> hMultiGet(String key, byte[] bytekey,
                                          byte[]... fields) {
        Map<String, byte[]> map = new HashMap<String, byte[]>();
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                List<byte[]> ret = jedis.hmget(bytekey, fields);
                for (int i = ret.size() - 1; i >= 0; i--) {
                    map.put(SafeEncoder.encode(fields[i]), ret.get(i));
                }
                return map;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return map;
    }

    /******************************** above:hash hMultiGet ***************************************/

    /******************************* below:hash hSetIfNotExist ***************************************/
    /**
     * 类似于hSetString，但只有在field不存在时才设置
     * <p/>
     * key field value -1操作失败；0 field已存在，本次设置不成功; 1 field不存在，设置成功
     */

    public long hSetStringIfNotExist(String key, String field, String value) {
        return hSetByteArrIfNotExist(key, field, SafeEncoder.encode(value));
    }

    /**
     * 类似于hSetObject，但只有在field不存在时才设置
     * <p/>
     * key field value -1操作失败；0 field已存在，本次设置不成功; 1 field不存在，设置成功
     */

    public long hSetObjectIfNotExist(String key, String field, Object value) {
        return hSetByteArrIfNotExist(key, field, serialize(value));
    }

    /**
     * 类似于hSetByteArr，但只有在field不存在时才设置
     * <p/>
     * key field value -1操作失败；0 field已存在，本次设置不成功; 1 field不存在，设置成功
     */

    public long hSetByteArrIfNotExist(String key, String field, byte[] value) {
        valueTypeAssert(value);

        return hSetIfNotExist(key, SafeEncoder.encode(key),
                SafeEncoder.encode(field), value);

    }

    private long hSetIfNotExist(String key, byte[] bytekey, byte[] field,
                                byte[] value) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.hsetnx(bytekey, field, value);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return -1L;
    }

    /******************************** above:hash setInNotExist ***************************************/

    /******************************* below:hash hLen,hKeys,hDel,hIncrBy,hExists ************************/
    /**
     * 删除某hashmap的某项
     * <p/>
     * key field 1 删除成功；0 field 不存在，删除不成功;-1服务端异常
     */
    
    public long hMultiDelete(String key, List<String> fields){
    	return hMultiDelete(key, SafeEncoder.encode(key), getBArrArr(fields));
    }

    private long hMultiDelete(String key, byte[] bytekey, byte[][] fields) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                long ret = jedis.hdel(bytekey, fields);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return -1;
    }

    public long hDelete(String key, String field) {
        return hDelete(key, SafeEncoder.encode(key), SafeEncoder.encode(field));
    }

    private long hDelete(String key, byte[] bytekey, byte[] field) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                long ret = jedis.hdel(bytekey, field);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return -1;
    }

    /**
     * 获取某hashmap中的items数
     * <p/>
     * key
     */

    public long hLen(String key) {

        return hLen(key, SafeEncoder.encode(key));

    }

    private long hLen(String key, byte[] bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                long ret = jedis.hlen(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0;
    }

    public Set<String> hKeys(String key) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Set<String> ret = jedis.hkeys(key);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /**
     * 对hashmap下的某field进行自增操作。自增规则可见incr()接口
     * <p/>
     * key field step 自增步长，可为负数 自增后的新值。如果原值为十进制的字符串表示，则返回null
     */

    public Long hIncrBy(String key, String field, long step) {

        return hIncrBy(key, SafeEncoder.encode(key), SafeEncoder.encode(field),
                step);

    }

    private Long hIncrBy(String key, byte[] bytekey, byte[] field, long step) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.hincrBy(bytekey, field, step);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /**
     * 判断某hashmap下是否包含某field
     * <p/>
     * key field
     */

    public Boolean hExists(String key, String field) {

        return hExists(key, SafeEncoder.encode(key), SafeEncoder.encode(field));

    }

    private Boolean hExists(String key, byte[] bytekey, byte[] field) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Boolean ret = jedis.hexists(bytekey, field);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return false;
    }

    /******************************** above:hash hLen,hKeys,hDel,hIncrBy,hExists ************************/

    /****************** below:list lpush,rpush,lpushIfListExist,rpushIfListExist ***********************/
    /**
     * 在list左端新增String类型的item
     * <p/>
     * key item 操作完成后的list长度
     */

    public Long lpushString(String key, String item) {
        return lpushByteArr(key, SafeEncoder.encode(item));
    }

    /**
     * 在list左端新增Object类型的item
     * <p/>
     * key item 操作完成后的list长度
     */

    public Long lpushObject(String key, Object item) {
        return lpushByteArr(key, serialize(item));
    }

    /**
     * 在list左端新增byte[]类型的item
     * <p/>
     * key item 操作完成后的list长度
     */

    public Long lpushByteArr(String key, byte[] item) {
        valueTypeAssert(item);

        return lpush(key, SafeEncoder.encode(key), item);

    }

    private Long lpush(String key, byte[] bytekey, byte[] value) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.lpush(bytekey, value);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    public Map<String, Long> piprpushString(String key, Set<String> items) {
        List<String> itemList = Lists.newArrayList(items);
        List<byte[]> bytes = Lists.newArrayList();
        for(String item : itemList) {
            bytes.add(SafeEncoder.encode(item));
        }
        List<Object> resList = piprpushByteArr(key, bytes);
        Map<String, Long> res = Maps.newLinkedHashMap();
        for(int i = 0; i < itemList.size(); i++) {
            String item = itemList.get(i);
            long oneRes = 0;
            if(resList.get(i) != null) oneRes = (long)resList.get(i);
            res.put(item, oneRes);
        }
        return res;
    }

    /**
     * 在list右端新增String类型的item
     * <p/>
     * key item 操作完成后的list长度
     */
    public Long rpushString(String key, String item) {
        return rpushByteArr(key, SafeEncoder.encode(item));
    }

    public Map<Object, Long> piprpushObject(String key, List<Object> items) {
        List<byte[]> bytes = Lists.newArrayList();
        for(Object item : items) {
            bytes.add(serialize(item));
        }
        List<Object> resList = piprpushByteArr(key, bytes);
        Map<Object, Long> res = Maps.newLinkedHashMap();
        for(int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            long oneRes = 0;
            if(resList.get(i) != null) oneRes = (long)resList.get(i);
            res.put(item, oneRes);
        }
        return res;
    }

    /**
     * 在list右端新增Object类型的item
     * <p/>
     * key item 操作完成后的list长度
     */
    public Long rpushObject(String key, Object item) {
        return rpushByteArr(key, serialize(item));
    }

    public List<Object> piprpushByteArr(String key, List<byte[]> items) {
        valueTypeAssert(items);
        return piprpush(key, SafeEncoder.encode(key), items);

    }

    /**
     * 在list右端新增byte[]类型的item
     * <p/>
     * key item 操作完成后的list长度
     */
    public Long rpushByteArr(String key, byte[] item) {
        valueTypeAssert(item);
        return rpush(key, SafeEncoder.encode(key), item);

    }

    private List<Object> piprpush(String key, byte[] bytekey, List<byte[]> values) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        Pipeline pipeline = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                pipeline = jedis.pipelined();
                for(byte[] value : values) {
                    pipeline.rpush(bytekey, value);
                }
                List<Object> res = pipeline.syncAndReturnAll();
                return res;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            try {
                pipeline.close();
            } catch (Exception e) {}
            onFinally(shardJedis);
        }
        return Lists.newArrayList();
    }

    private Long rpush(String key, byte[] bytekey, byte[] value) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                Long ret = jedis.rpush(bytekey, value);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    public Long lpushStringIfListExist(String key, String item) {
        return lpushByteArrIfListExist(key, SafeEncoder.encode(item));
    }

    public Long lpushObjectIfListExist(String key, Object item) {
        return lpushByteArrIfListExist(key, serialize(item));
    }

    public Long lpushByteArrIfListExist(String key, byte[] item) {
        valueTypeAssert(item);

        return lpushIfListExist(key, SafeEncoder.encode(key), item);

    }

    private Long lpushIfListExist(String key, byte[] bytekey, byte[] value) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.lpushx(bytekey, value);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    public Long rpushStringIfListExist(String key, String item) {
        return rpushByteArrIfListExist(key, SafeEncoder.encode(item));
    }

    public Long rpushObjectIfListExist(String key, Object item) {
        return rpushByteArrIfListExist(key, serialize(item));
    }

    public Long rpushByteArrIfListExist(String key, byte[] item) {
        valueTypeAssert(item);

        return rpushIfListExist(key, SafeEncoder.encode(key), item);

    }

    private Long rpushIfListExist(String key, byte[] bytekey, byte[] value) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.rpushx(bytekey, value);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /******************** above:list lpush,rpush,lpushIfListExist,rpushIfListExist ***********************/

    /******************************* below:list lpop,rpop ***************************************/
    /**
     * 在list的最左端以String的方式，取出并删除一个item
     * <p/>
     * key
     */

    public String lpopString(String key) {
        byte[] ret = lpopByteArr(key);
        if (ret != null) {
            return SafeEncoder.encode(ret);
        }
        return null;
    }

    /**
     * 在list的最左端以Object的方式，取出并删除一个item
     * <p/>
     * key
     */

    public Object lpopObject(String key, final Class<? extends ScloudSerializable> clazz) {
        byte[] ret = lpopByteArr(key);
        if (ret != null) {
            return deserialize(ret, clazz);
        }
        return null;
    }

    /**
     * 在list的最左端以byte[]的方式，取出并删除一个item
     * <p/>
     * key
     */

    public byte[] lpopByteArr(String key) {

        return lpop(key, SafeEncoder.encode(key));

    }

    private byte[] lpop(String key, byte[] bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                byte[] ret = jedis.lpop(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /**
     * 在list的最右端以String的方式，取出并删除一个item
     * <p/>
     * key
     */

    public String rpopString(String key) {
        byte[] ret = rpopByteArr(key);
        if (ret != null) {
            return SafeEncoder.encode(ret);
        }
        return null;
    }

    /**
     * 在list的最右端以Object的方式，取出并删除一个item
     * <p/>
     * key
     */

    public Object rpopObject(String key, final Class<? extends ScloudSerializable> clazz) {
        byte[] ret = rpopByteArr(key);
        if (ret != null) {
            return deserialize(ret, clazz);
        }
        return null;
    }

    /**
     * 在list的最右端以byte[]的方式，取出并删除一个item
     * <p/>
     * key
     */

    public byte[] rpopByteArr(String key) {

        return rpop(key, SafeEncoder.encode(key));

    }

    private byte[] rpop(String key, byte[] bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                byte[] ret = jedis.rpop(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /******************************** above:list lpop,rpop ***************************************/

    /******************************* below:list lindex,lrange *************************************/
    /**
     * 以String的方式取出list的index（从左到右从0开始计数）位上item
     * <p/>
     * key
     */

    public String lindexString(String key, int index) {
        byte[] ret = lindexByteArr(key, index);
        if (ret != null) {
            return SafeEncoder.encode(ret);
        }
        return null;
    }

    /**
     * 以Object的方式取出list的index（从左到右从0开始计数）位上item
     * <p/>
     * key
     */

    public Object lindexObject(String key, int index, final Class<? extends ScloudSerializable> clazz) {
        byte[] ret = lindexByteArr(key, index);
        if (ret != null) {
            return deserialize(ret, clazz);
        }
        return null;
    }

    /**
     * 以byte[]的方式取出list的index（从左到右从0开始计数）位上item
     * <p/>
     * key
     */

    public byte[] lindexByteArr(String key, int index) {

        return lindex(key, SafeEncoder.encode(key), index);

    }

    private byte[] lindex(String key, byte[] bytekey, int index) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                byte[] ret = jedis.lindex(bytekey, index);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /**
     * 以String的方式取出list的某位置区间上的items。
     * <p/>
     * key start end
     */

    public List<String> lrangeString(String key, int start, int end) {
        List<byte[]> ret = lrangeByteArr(key, start, end);
        if (ret != null) {
            List<String> trueRet = new ArrayList<String>();
            for (byte[] item : ret) {
                if (item != null) {
                    trueRet.add(SafeEncoder.encode(item));
                }
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 以Object的方式取出list的某位置区间上的items
     * <p/>
     * key start end
     */

    public List<Object> lrangeObject(String key, int start, int end, final Class<? extends ScloudSerializable> clazz) {
        List<byte[]> ret = lrangeByteArr(key, start, end);
        if (ret != null) {
            List<Object> trueRet = new ArrayList<Object>();
            for (byte[] item : ret) {
                if (item != null) {
                    trueRet.add(deserialize(item, clazz));
                }
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 以byte[]的方式取出list的某位置区间上的items
     * <p/>
     * key start end
     */

    public List<byte[]> lrangeByteArr(String key, int start, int end) {

        return lrange(key, SafeEncoder.encode(key), start, end);

    }

    private List<byte[]> lrange(String key, byte[] bytekey, int start, int end) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                List<byte[]> ret = jedis.lrange(bytekey, start, end);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /******************************** above:list lindex,lrange ***************************************/

    /******************************* below:list lset,ltrim ***************************************/
    /**
     * 设置list的index位置上的值（String类型）
     * <p/>
     * key index value 如果index超出list长度返回failed;否则设置成功返回OK
     */

    public String lsetString(String key, int index, String value) {
        return lsetByteArr(key, index, SafeEncoder.encode(value));
    }

    /**
     * 设置list的index位置上的值（Object类型）
     * <p/>
     * key index value 如果index超出list长度返回failed;否则设置成功返回OK
     */

    public String lsetObject(String key, int index, Object value) {
        return lsetByteArr(key, index, serialize(value));
    }

    /**
     * 设置list的index位置上的值（byte[]类型）
     * <p/>
     * key index value 如果index超出list长度返回failed;否则设置成功返回OK
     */

    public String lsetByteArr(String key, int index, byte[] value) {
        valueTypeAssert(value);

        return lset(key, SafeEncoder.encode(key), index, value);

    }

    private String lset(String key, byte[] bytekey, int index, byte[] value) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                String ret = jedis.lset(bytekey, index, value);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return "failed";
    }

    /**
     * 对list在服务端进行截取，范围之外部分将被服务端永久丢弃
     * <p/>
     * key start end 截取成功返回OK；如果指定范围超出list的实际范围，返回failed
     */

    public String ltrim(String key, int start, int end) {

        return ltrim(key, SafeEncoder.encode(key), start, end);

    }

    private String ltrim(String key, byte[] bytekey, int start, int end) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                String ret = jedis.ltrim(bytekey, start, end);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return "failed";
    }

    /******************************** above:list lset,ltrim ***************************************/

    /******************************* below:list len ***************************************/
    /**
     * 获取某list的长度
     * <p/>
     * key
     */

    public Long llen(String key) {

        return llen(key, SafeEncoder.encode(key));

    }

    private Long llen(String key, byte[] bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.llen(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /******************************** above:list len ***************************************/

    /******************************* below:set sadd,srem,spop,smember **********************/
    /**
     * 往集合中插入member
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was added 0
     * if the element was already a member of the set
     */

    public Long saddString(String key, String member) {
        return saddByteArr(key, SafeEncoder.encode(member));
    }

    /**
     * 往集合中插入member(复杂对象是否适应于此方法有待测试验证)
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was added 0
     * if the element was already a member of the set
     */

    public Long saddObject(String key, Object member) {
        return saddByteArr(key, serialize(member));
    }

    /**
     * 往集合中插入member
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was added 0
     * if the element was already a member of the set
     */

    public Long saddByteArr(String key, byte[] member) {
        valueTypeAssert(member);

        return sadd(key, SafeEncoder.encode(key), member);

    }

    private Long sadd(String key, byte[] bytekey, byte[] member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.sadd(bytekey, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /******************************* below:set sadd,srem,spop,smember **********************/
    /**
     * 往集合中插入member
     * <p/>
     * key Integer reply, specifically: 1 if the new element was added 0 if the
     * element was already a member of the set
     */

    public Long smultiAddString(String key, String... members) {
        return smultiAddByteArr(key, SafeEncoder.encodeMany(members));
    }

    /**
     * 往集合中插入member(复杂对象是否适应于此方法有待测试验证)
     * <p/>
     * key Integer reply, specifically: 1 if the new element was added 0 if the
     * element was already a member of the set
     */

    public Long smultiAddObject(String key, Object... members) {
        return smultiAddByteArr(key, serialize(members));
    }

    /**
     * 往集合中插入member
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was added 0
     * if the element was already a member of the set
     */
    public Long smultiAddByteArr(String key, byte[][] members) {
        valueTypeAssert(members);
        return smultiAdd(key, SafeEncoder.encode(key), members);
    }

    private Long smultiAdd(String key, byte[] bytekey, byte[][] member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                Long ret = jedis.sadd(bytekey, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /**
     * 删除集合中的member成员
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was removed
     * 0 if the new element was not a member of the set
     */

    public Long sremString(String key, String member) {
        return sremByteArr(key, SafeEncoder.encode(member));
    }

    /**
     * 删除集合中的member成员（复杂对象是否适合此方法有待测试）
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was removed
     * 0 if the new element was not a member of the set
     */

    public Long sremObject(String key, Object member) {
        return sremByteArr(key, serialize(member));
    }

    /**
     * 删除集合中的member成员
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was removed
     * 0 if the new element was not a member of the set
     */

    public Long sremByteArr(String key, byte[] member) {
        valueTypeAssert(member);

        return srem(key, SafeEncoder.encode(key), member);

    }

    private Long srem(String key, byte[] bytekey, byte[] member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.srem(bytekey, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /**
     * Remove and return a random member from a set
     * <p/>
     * key
     */

    public String spopString(String key) {
        byte[] ret = spopByteArr(key);
        if (ret != null) {
            return SafeEncoder.encode(ret);
        }
        return null;
    }

    /**
     * Remove and return a random member from a set
     * <p/>
     * key
     */

    public Object spopObject(String key, final Class<? extends ScloudSerializable> clazz) {
        byte[] ret = spopByteArr(key);
        if (ret != null) {
            return deserialize(ret, clazz);
        }
        return null;
    }

    /**
     * Remove and return a random member from a set
     * <p/>
     * key
     */

    public byte[] spopByteArr(String key) {

        return spop(key, SafeEncoder.encode(key));

    }

    private byte[] spop(String key, byte[] bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                byte[] ret = jedis.spop(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /**
     * Get all the members in a set
     * <p/>
     * key
     */

    public Set<String> smemberString(String key) {
        Set<byte[]> ret = smemberByteArr(key);
        if (ret != null) {
            Set<String> trueRet = new HashSet<String>();
            for (byte[] member : ret) {
                if (member != null) {
                    trueRet.add(SafeEncoder.encode(member));
                }
            }
            return trueRet;
        }
        return null;
    }

    /**
     * Get all the members in a set
     * <p/>
     * key
     */

    public Set<Object> smemberObject(String key, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = smemberByteArr(key);
        if (ret != null) {
            Set<Object> trueRet = new HashSet<Object>();
            for (byte[] member : ret) {
                if (member != null) {
                    trueRet.add(deserialize(member, clazz));
                }
            }
            return trueRet;
        }
        return null;
    }

    /**
     * Get all the members in a set
     * <p/>
     * key
     */

    public Set<byte[]> smemberByteArr(String key) {

        return smember(key, SafeEncoder.encode(key));

    }

    private Set<byte[]> smember(String key, byte[] bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Set<byte[]> ret = jedis.smembers(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /******************************** above:set sadd,srem,spop,smember *************************/

    /******************************* below:set scard,sismember *******************************/
    /**
     * 判断某个member是否在此集合中
     * <p/>
     * key member
     */

    public Boolean sismemberString(String key, String member) {
        return sismemberByteArr(key, SafeEncoder.encode(member));
    }

    /**
     * 判断某个member是否在此集合中
     * <p/>
     * key member
     */

    public Boolean sismemberObject(String key, Object member) {
        return sismemberByteArr(key, serialize(member));
    }

    /**
     * 判断某个member是否在此集合中
     * <p/>
     * key member
     */

    public Boolean sismemberByteArr(String key, byte[] member) {
        if (member == null) {
            return false;
        }

        return sismember(key, SafeEncoder.encode(key), member);

    }

    private Boolean sismember(String key, byte[] bytekey, byte[] member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Boolean ret = jedis.sismember(bytekey, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return false;
    }

    /**
     * Get the number of members in a set
     * <p/>
     * key
     */

    public Long scard(String key) {

        return scard(key, SafeEncoder.encode(key));

    }

    private Long scard(String key, byte[] bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.scard(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /******************************** above:set scard,sismember *******************************/

    /****************************** below: set sinter，sinterstore *************************/

    /****************************** above: set sinter，sinterstore *************************/

    /****************************** below:sorted set ZADD、zaddMulti *************************/
    /**
     * Add the specified member having the specifeid score to the sorted set
     * stored at key. If member is already a member of the sorted set the score
     * is updated, and the element reinserted in the right position to ensure
     * sorting. If key does not exist a new sorted set with the specified member
     * as sole member is created. If the key exists but does not hold a sorted
     * set value an error is returned.
     * <p/>
     * The score value can be the string representation of a double precision
     * floating point number.
     * <p/>
     * Time complexity O(log(N)) with N being the number of elements in the
     * sorted set
     * <p/>
     * key score member Integer reply, specifically: 1 if the new element was
     * added; 0 if the element was already a member of the sorted set and the
     * score was updated ; -1 the error happened in server
     */

    public Long zaddStringWithSharding(String shardingKey, String key,
                                       double score, String member) {
        return zaddByteArrWithSharding(shardingKey, key, score,
                SafeEncoder.encode(member));
    }

    public Long zaddObjectWithSharding(String shardingKey, String key,
                                       double score, Object member) {
        return zaddByteArrWithSharding(shardingKey, key, score,
                serialize(member));
    }

    public Long zaddByteArrWithSharding(String shardingKey, String key,
                                        double score, byte[] member) {
        valueTypeAssert(member);

        return zaddWithSharding(shardingKey, key, SafeEncoder.encode(key),
                score, member);

    }

    private Long zaddWithSharding(String shardingKey, String key,
                                  byte[] bytekey, double score, byte[] member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                Long ret = jedis.zadd(bytekey, score, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /**
     * 往sorted set中插入多个元素
     * <p/>
     * key scoreMembers 成功插入元素的个数
     */

    public Long zaddMultiStringWithSharding(String shardingKey, String key,
                                            Map<String, Double> memberScores) {
    	Map<byte[], Double> bMemberScores = new HashMap<byte[], Double>();
        for (Map.Entry<String, Double> item : memberScores.entrySet()) {
            bMemberScores.put(SafeEncoder.encode(item.getKey()), item.getValue());
        }
        return zaddMultiByteArrWithSharding(shardingKey, key, bMemberScores);
//        Map<Double, byte[]> scoreBMembers = new HashMap<Double, byte[]>();
//        for (Map.Entry<Double, String> item : scoreMembers.entrySet()) {
//            if (item.getValue() != null) {
//                scoreBMembers.put(item.getKey(),
//                        SafeEncoder.encode(item.getValue()));
//            }
//        }
//        return zaddMultiByteArrWithSharding(shardingKey, key, scoreBMembers);
    }

    /**
     * 往sorted set中插入多个元素
     * <p/>
     * key scoreMembers 成功插入元素的个数
     */

    public Long zaddMultiObjectWithSharding(String shardingKey, String key,
                                            Map<Object, Double> memberScores) {
    	Map<byte[], Double> bMemberScores = new HashMap<byte[], Double>();
        for (Map.Entry<Object, Double> item : memberScores.entrySet()) {
        	bMemberScores.put(serialize(item.getKey()), item.getValue());
        }
        return zaddMultiByteArrWithSharding(shardingKey, key, bMemberScores);
    	
//        Map<Double, byte[]> scoreBMembers = new HashMap<Double, byte[]>();
//        for (Map.Entry<Double, Object> item : scoreMembers.entrySet()) {
//            if (item.getValue() != null) {
//                scoreBMembers.put(item.getKey(), serialize(item.getValue()));
//            }
//        }
//        return zaddMultiByteArrWithSharding(shardingKey, key, scoreBMembers);
    }

    /**
     * 往sorted set中插入多个元素
     * <p/>
     * key scoreMembers 成功插入元素的个数
     */
    
    public Long zaddMultiByteArrWithSharding(String shardingKey, String key,
			Map<byte[], Double> scoreMembers) {
		if (scoreMembers == null || scoreMembers.size() == 0) {
			return 0L;
		}

		return zaddWithSharding(shardingKey, key, SafeEncoder.encode(key),
				scoreMembers);

	}
    
//	public Long zaddMultiByteArrWithSharding(String shardingKey, String key,
//			Map<Double, byte[]> scoreMembers) {
//		if (scoreMembers == null || scoreMembers.size() == 0) {
//			return 0L;
//		}
//
//		return zaddWithSharding(shardingKey, key, SafeEncoder.encode(key),
//				scoreMembers);
//
//	}

//    public Long zaddMultiByteArrWithSharding(String shardingKey, String key,
//                                             Map<Double, byte[]> scoreMembers) {
//        if (scoreMembers == null || scoreMembers.size() == 0) {
//            return 0L;
//        }
//
//        return zaddWithSharding(shardingKey, key, SafeEncoder.encode(key),
//                scoreMembers);
//
//    }
    
	private Long zaddWithSharding(String shardingKey, String key,
			byte[] bytekey, Map<byte[], Double> scoreMembers) {
		ShardedJedis shardJedis = null;
		Jedis jedis = null;
		try {

			shardJedis = shardedJedisPool.getResource();
			jedis = shardJedis.getShard(shardingKey);

			if (jedis != null) {
				Long ret = jedis.zadd(bytekey, scoreMembers);
				return ret;
			}
		} catch (Exception e) {
			onException(shardJedis, jedis, e);
			shardJedis = null;
		} finally {
			onFinally(shardJedis);
		}
		return 0L;
	}

//    private Long zaddWithSharding(String shardingKey, String key,
//                                  byte[] bytekey, Map<Double, byte[]> scoreMembers) {
//        ShardedJedis shardJedis = null;
//        Jedis jedis = null;
//        try {
//
//            shardJedis = shardedJedisPool.getResource();
//            jedis = shardJedis.getShard(shardingKey);
//
//            if (jedis != null) {
//                Long ret = jedis.zadd(bytekey, scoreMembers);
//                return ret;
//            }
//        } catch (Exception e) {
//            onException(shardJedis, jedis, e);
//            shardJedis = null;
//        } finally {
//            onFinally(shardJedis);
//        }
//        return 0L;
//    }

    /****************************** above:sorted set ZADD、zaddMulti *************************/

    /****************************** above:sorted set zremrangeByRank、 ZREM、zremMulti *************************/
    /**
     * 从有序集合中删除指定位置范围元素
     * <p/>
     * key start end 所删除的元素个数
     */

    public Long zremrangeByRankWithSharding(String shardingKey, String key,
                                            int start, int end) {

        return zremrangeByRankWithSharding(shardingKey, key,
                SafeEncoder.encode(key), start, end);

    }

    private Long zremrangeByRankWithSharding(String shardingKey, String key,
                                             byte[] bytekey, int start, int end) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                Long ret = jedis.zremrangeByRank(bytekey, start, end);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /**
     * 从有序集合中删除指定元素
     * <p/>
     * key member 0，未找到该元素删除不成功； 1，找到并删除成功
     */

    public Long zremStringWithSharding(String shardingKey, String key,
                                       String member) {
        return zremByteArrWithSharding(shardingKey, key,
                SafeEncoder.encode(member));
    }

    /**
     * 从有序集合中删除指定元素
     * <p/>
     * key member 0，未找到该元素删除不成功； 1，找到并删除成功
     */

    public Long zremObjectWithSharding(String shardingKey, String key,
                                       Object member) {
        return zremByteArrWithSharding(shardingKey, key, serialize(member));
    }

    /**
     * 从有序集合中删除指定元素
     * <p/>
     * key member 0，未找到该元素删除不成功； 1，找到并删除成功
     */

    public Long zremByteArrWithSharding(String shardingKey, String key,
                                        byte[] member) {
        if (member == null) {
            return 0L;
        }

        return zremWithSharding(shardingKey, key, SafeEncoder.encode(key),
                member);

    }

    /**
     * 从有序集合中删除多个指定元素
     * <p/>
     * key memberList 成功删除的元素个数
     */

    public Long zremMultiStringWithSharding(String shardingKey, String key,
                                            List<String> memberList) {
        List<byte[]> bArrList = new ArrayList<byte[]>();
        for (String member : memberList) {
            if (member != null) {
                bArrList.add(SafeEncoder.encode(member));
            }
        }
        return zremMultiByteArrWithSharding(shardingKey, key, bArrList);
    }

    /**
     * 从有序集合中删除多个指定元素
     * <p/>
     * key memberList 成功删除的元素个数
     */

    public Long zremMultiObjectWithSharding(String shardingKey, String key,
                                            List<Object> memberList) {
        List<byte[]> bArrList = new ArrayList<byte[]>();
        for (Object member : memberList) {
            if (member != null) {
                bArrList.add(serialize(member));
            }
        }
        return zremMultiByteArrWithSharding(shardingKey, key, bArrList);
    }

    /**
     * 从有序集合中删除多个指定元素
     * <p/>
     * key memberList 成功删除的元素个数
     */

    public Long zremMultiByteArrWithSharding(String shardingKey, String key,
                                             List<byte[]> memberList) {
        byte[][] memberB = new byte[memberList.size()][];
        for (int i = memberList.size() - 1; i >= 0; i--) {
            memberB[i] = memberList.get(i);
        }

        return zremWithSharding(shardingKey, key, SafeEncoder.encode(key),
                memberB);

    }

    private Long zremWithSharding(String shardingKey, String key,
                                  byte[] bytekey, byte[]... member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                Long ret = jedis.zrem(bytekey, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /****************************** above:sorted set zremrangeByRank、ZREM、zremMulti *************************/

    /************************** below:sorted set ZCARD、ZCOUNT *********************/
    /**
     * 获取sorted set 中元素个数
     * <p/>
     * key
     */

    public Long zcardWithSharding(String shardingKey, String key) {

        return zcardWithSharding(shardingKey, key, SafeEncoder.encode(key));

    }

    /**
     * 获取sorted set 中指定范围内的元素个数
     * <p/>
     * key minScore maxScore
     */

    public Long zcountWithSharding(String shardingKey, String key,
                                   double minScore, double maxScore) {

        return zcountWithSharding(shardingKey, key, SafeEncoder.encode(key),
                minScore, maxScore);

    }

    private Long zcardWithSharding(String shardingKey, String key,
                                   byte[] bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                Long ret = jedis.zcard(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    private Long zcountWithSharding(String shardingKey, String key,
                                    byte[] bytekey, double minScore, double maxScore) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                Long ret = jedis.zcount(bytekey, minScore, maxScore);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /**************************** above:sorted set ZCARD、ZCOUNT *********************/

    /****************************** below:sorted set ZRANK,ZREVRANK、ZSCORE *************************/
    /**
     * 返回元素在有序集合（从小到大）中的序号（从0开始）
     * <p/>
     * key member 如果元素存在时返回序号，元素不存在时返回null
     */

    public Long zrankStringWithSharding(String shardingKey, String key,
                                        String member) {
        return zrankByteArrWithSharding(shardingKey, key,
                SafeEncoder.encode(member));
    }

    /**
     * 返回元素在有序集合（从小到大）中的序号（从0开始）
     * <p/>
     * key member 如果元素存在时返回序号，元素不存在时返回null
     */

    public Long zrankObjectWithSharding(String shardingKey, String key,
                                        Object member) {
        return zrankByteArrWithSharding(shardingKey, key, serialize(member));
    }

    /**
     * 返回元素在有序集合（从小到大）中的序号（从0开始）
     * <p/>
     * key member 如果元素存在时返回序号，元素不存在时返回null
     */

    public Long zrankByteArrWithSharding(String shardingKey, String key,
                                         byte[] member) {
        valueTypeAssert(member);

        return zrankWithSharding(shardingKey, key, SafeEncoder.encode(key),
                member, true);

    }

    /**
     * 返回元素在有序集合（从大到小）中的序号（从0开始）
     * <p/>
     * key member 如果元素存在时返回序号，元素不存在时返回null
     */

    public Long zrevrankStringWithSharding(String shardingKey, String key,
                                           String member) {
        return zrevrankByteArrWithSharding(shardingKey, key,
                SafeEncoder.encode(member));
    }

    /**
     * 返回元素在有序集合（从大到小）中的序号（从0开始）
     * <p/>
     * key member 如果元素存在时返回序号，元素不存在时返回null
     */

    public Long zrevrankObjectWithSharding(String shardingKey, String key,
                                           Object member) {
        return zrevrankByteArrWithSharding(shardingKey, key, serialize(member));
    }

    /**
     * 返回元素在有序集合（从大到小）中的序号（从0开始）
     * <p/>
     * key member 如果元素存在时返回序号，元素不存在时返回null
     */

    public Long zrevrankByteArrWithSharding(String shardingKey, String key,
                                            byte[] member) {
        valueTypeAssert(member);

        return zrankWithSharding(shardingKey, key, SafeEncoder.encode(key),
                member, false);

    }

    private Long zrankWithSharding(String shardingKey, String key,
                                   byte[] bytekey, byte[] member, boolean isasc) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                if (isasc) {
                    Long ret = jedis.zrank(bytekey, member);
                    return ret;
                } else {
                    Long ret = jedis.zrevrank(bytekey, member);
                    return ret;
                }
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /**
     * 返回元素在有序集合中的排序因子
     * <p/>
     * key member 如果元素存在时返回排序因子score，元素不存在时返回null
     */

    public Double zscoreStringWithSharding(String shardingKey, String key,
                                           String member) {
        return zscoreByteArrWithSharding(shardingKey, key,
                SafeEncoder.encode(member));
    }

    /**
     * 返回元素在有序集合中的排序因子
     * <p/>
     * key member 如果元素存在时返回排序因子score，元素不存在时返回null
     */

    public Double zscoreObjectWithSharding(String shardingKey, String key,
                                           Object member) {
        return zscoreByteArrWithSharding(shardingKey, key, serialize(member));
    }

    /**
     * 返回元素在有序集合中的排序因子
     * <p/>
     * key member 如果元素存在时返回排序因子score，元素不存在时返回null
     */

    public Double zscoreByteArrWithSharding(String shardingKey, String key,
                                            byte[] member) {
        valueTypeAssert(member);

        return zscoreWithSharding(shardingKey, key, SafeEncoder.encode(key),
                member);

    }

    private Double zscoreWithSharding(String shardingKey, String key,
                                      byte[] bytekey, byte[] member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                Double ret = jedis.zscore(bytekey, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /****************************** above:sorted set ZRANK,ZREVRANK、ZSCORE *************************/

    /*************************** below:sorted set ZRANGE、ZREVRANGE *******************/
    /**
     * 获取指定位置范围内的升序集合
     * <p/>
     * key start end LinkedHashSet结果集
     */

    public Set<String> zrangeStringWithSharding(String shardingKey, String key,
                                                int start, int end) {
        Set<byte[]> ret = zrangeByteArrWithSharding(shardingKey, key, start,
                end);
        if (ret != null) {
            Set<String> trueRet = new LinkedHashSet<String>(); // used
            // LinkedHashSet
            // to ensure it
            // is in order
            for (byte[] item : ret) {
                trueRet.add(SafeEncoder.encode(item));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定位置范围内的升序集合
     * <p/>
     * key start end LinkedHashSet结果集
     */

    public Set<Object> zrangeObjectWithSharding(String shardingKey, String key,
                                                int start, int end, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = zrangeByteArrWithSharding(shardingKey, key, start,
                end);
        if (ret != null) {
            Set<Object> trueRet = new LinkedHashSet<Object>();
            for (byte[] item : ret) {
                trueRet.add(deserialize(item, clazz));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定位置范围内的升序集合
     * <p/>
     * key start end LinkedHashSet结果集
     */

    public Set<byte[]> zrangeByteArrWithSharding(String shardingKey,
                                                 String key, int start, int end) {

        return zrangeWithSharding(shardingKey, key, SafeEncoder.encode(key),
                start, end, true);

    }

    /**
     * 获取指定位置范围内的降序集合
     * <p/>
     * key start end LinkedHashSet结果集
     */

    public Set<String> zrevrangeStringWithSharding(String shardingKey,
                                                   String key, int start, int end) {
        Set<byte[]> ret = zrevrangeByteArrWithSharding(shardingKey, key, start,
                end);
        if (ret != null) {
            Set<String> trueRet = new LinkedHashSet<String>();
            for (byte[] item : ret) {
                trueRet.add(SafeEncoder.encode(item));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定位置范围内的降序集合
     * <p/>
     * key start end LinkedHashSet结果集
     */

    public Set<Object> zrevrangeObjectWithSharding(String shardingKey,
                                                   String key, int start, int end, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = zrevrangeByteArrWithSharding(shardingKey, key, start,
                end);
        if (ret != null) {
            Set<Object> trueRet = new LinkedHashSet<Object>();
            for (byte[] item : ret) {
                trueRet.add(deserialize(item, clazz));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定位置范围内的降序集合
     * <p/>
     * key start end LinkedHashSet结果集
     */

    public Set<byte[]> zrevrangeByteArrWithSharding(String shardingKey,
                                                    String key, int start, int end) {

        return zrangeWithSharding(shardingKey, key, SafeEncoder.encode(key),
                start, end, false);

    }

    private Set<byte[]> zrangeWithSharding(String shardingKey, String key,
                                           byte[] bytekey, int start, int end, boolean isasc) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                if (isasc) {
                    Set<byte[]> ret = jedis.zrange(bytekey, start, end);
                    return ret;
                } else {
                    Set<byte[]> ret = jedis.zrevrange(bytekey, start, end);
                    return ret;
                }
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /*************************** above:sorted set ZRANGE、ZREVRANGE *******************/

    /****************************** below:sorted set zrangeByScore、zrevrangeByScore *************************/
    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore LinkedHashSet 升序结果集
     */

    public Set<String> zrangeByScoreStringWithSharding(String shardingKey,
                                                       String key, double minScore, double maxScore) {
        Set<byte[]> ret = zrangeByScoreByteArrWithSharding(shardingKey, key,
                minScore, maxScore);
        if (ret != null) {
            Set<String> trueRet = new LinkedHashSet<String>();
            for (byte[] item : ret) {
                trueRet.add(SafeEncoder.encode(item));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore LinkedHashSet升序结果集
     */

    public Set<Object> zrangeByScoreObjectWithSharding(String shardingKey,
                                                       String key, double minScore, double maxScore, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = zrangeByScoreByteArrWithSharding(shardingKey, key,
                minScore, maxScore);
        if (ret != null) {
            Set<Object> trueRet = new LinkedHashSet<Object>();
            for (byte[] item : ret) {
                trueRet.add(deserialize(item, clazz));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore LinkedHashSet升序结果集
     */

    public Set<byte[]> zrangeByScoreByteArrWithSharding(String shardingKey,
                                                        String key, double minScore, double maxScore) {

        return zrangeByScoreWithSharding(shardingKey, key,
                SafeEncoder.encode(key), minScore, maxScore, true);

    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore LinkedHashSet 降序结果集
     */

    public Set<String> zrevrangeByScoreStringWithSharding(String shardingKey,
                                                          String key, double maxScore, double minScore) {
        Set<byte[]> ret = zrevrangeByScoreByteArrWithSharding(shardingKey, key,
                maxScore, minScore);
        if (ret != null) {
            Set<String> trueRet = new LinkedHashSet<String>();
            for (byte[] item : ret) {
                trueRet.add(SafeEncoder.encode(item));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore LinkedHashSet降序结果集
     */

    public Set<Object> zrevrangeByScoreObjectWithSharding(String shardingKey,
                                                          String key, double maxScore, double minScore, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = zrevrangeByScoreByteArrWithSharding(shardingKey, key,
                maxScore, minScore);
        if (ret != null) {
            Set<Object> trueRet = new LinkedHashSet<Object>();
            for (byte[] item : ret) {
                trueRet.add(deserialize(item, clazz));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore LinkedHashSet降序结果集
     */

    public Set<byte[]> zrevrangeByScoreByteArrWithSharding(String shardingKey,
                                                           String key, double maxScore, double minScore) {

        return zrangeByScoreWithSharding(shardingKey, key,
                SafeEncoder.encode(key), minScore, maxScore, false);

    }

    private Set<byte[]> zrangeByScoreWithSharding(String shardingKey,
                                                  String key, byte[] bytekey, double minScore, double maxScore,
                                                  boolean isasc) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                if (isasc) {
                    Set<byte[]> ret = jedis.zrangeByScore(bytekey, minScore,
                            maxScore);
                    return ret;
                } else {
                    Set<byte[]> ret = jedis.zrevrangeByScore(bytekey, maxScore,
                            minScore);
                    return ret;
                }
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore offset count LinkedHashSet 升序结果集
     */

    public Set<String> zrangeByScoreStringWithSharding(String shardingKey,
                                                       String key, double minScore, double maxScore, int offset, int count) {
        Set<byte[]> ret = zrangeByScoreByteArrWithSharding(shardingKey, key,
                minScore, maxScore, offset, count);
        if (ret != null) {
            Set<String> trueRet = new LinkedHashSet<String>();
            for (byte[] item : ret) {
                trueRet.add(SafeEncoder.encode(item));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore offset count LinkedHashSet升序结果集
     */

    public Set<Object> zrangeByScoreObjectWithSharding(String shardingKey,
                                                       String key, double minScore, double maxScore, int offset, int count, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = zrangeByScoreByteArrWithSharding(shardingKey, key,
                minScore, maxScore, offset, count);
        if (ret != null) {
            Set<Object> trueRet = new LinkedHashSet<Object>();
            for (byte[] item : ret) {
                trueRet.add(deserialize(item, clazz));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore offset count LinkedHashSet升序结果集
     */

    public Set<byte[]> zrangeByScoreByteArrWithSharding(String shardingKey,
                                                        String key, double minScore, double maxScore, int offset, int count) {

        return zrangeByScoreWithSharding(shardingKey, key,
                SafeEncoder.encode(key), minScore, maxScore, offset, count,
                true);

    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore offset count LinkedHashSet 降序结果集
     */

    public Set<String> zrevrangeByScoreStringWithSharding(String shardingKey,
                                                          String key, double maxScore, double minScore, int offset, int count) {
        Set<byte[]> ret = zrevrangeByScoreByteArrWithSharding(shardingKey, key,
                maxScore, minScore, offset, count);
        if (ret != null) {
            Set<String> trueRet = new LinkedHashSet<String>();
            for (byte[] item : ret) {
                trueRet.add(SafeEncoder.encode(item));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore offset count LinkedHashSet降序结果集
     */

    public Set<Object> zrevrangeByScoreObjectWithSharding(String shardingKey,
                                                          String key, double maxScore, double minScore, int offset, int count, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = zrevrangeByScoreByteArrWithSharding(shardingKey, key,
                maxScore, minScore, offset, count);
        if (ret != null) {
            Set<Object> trueRet = new LinkedHashSet<Object>();
            for (byte[] item : ret) {
                trueRet.add(deserialize(item, clazz));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore offset count LinkedHashSet降序结果集
     */

    public Set<byte[]> zrevrangeByScoreByteArrWithSharding(String shardingKey,
                                                           String key, double maxScore, double minScore, int offset, int count) {

        return zrangeByScoreWithSharding(shardingKey, key,
                SafeEncoder.encode(key), minScore, maxScore, offset, count,
                false);

    }

    private Set<byte[]> zrangeByScoreWithSharding(String shardingKey,
                                                  String key, byte[] bytekey, double minScore, double maxScore,
                                                  int offset, int count, boolean isasc) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                if (isasc) {
                    Set<byte[]> ret = jedis.zrangeByScore(bytekey, minScore,
                            maxScore, offset, count);
                    return ret;
                } else {
                    Set<byte[]> ret = jedis.zrevrangeByScore(bytekey, maxScore,
                            minScore, offset, count);
                    return ret;
                }
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /**
     * 计算给定的一个或多个有序集的交集，并将该交集(结果集)储存到 newkey 。 默认情况下，结果集中某个成员的 score 值是所有给定集下该成员
     * score 值之和.
     * <p/>
     * shardingKey 排序的key，确保所有的集合在一台redis上面 newkey 新创建的key keys 需要操作的集合的key
     */
    public void zinterstoreWithSharding(String shardingKey, String newkey,
                                        String... keys) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            int dbIndex = this.getDBIndex(newkey);
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                byte[][] bytekes = new byte[keys.length][];
                for (int i = 0; i < keys.length; i++) {
                    String key = keys[i];
                    bytekes[i] = SafeEncoder.encode(key);
                }
                jedis.zinterstore(SafeEncoder.encode(newkey), bytekes);
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
    }

    /**
     * 计算给定的一个或多个有序集的并集，其中给定 key 的数量必须以 numkeys 参数指定，并将该并集(结果集)储存到 destination 。
     * 默认情况下，结果集中某个成员的 score 值是所有给定集下该成员 score 值之 和 。
     * <p/>
     * shardingKey 排序的key，确保所有的集合在一台redis上面 newkey 新创建的key keys 需要操作的集合的key
     */
    public void zunionstoreWithSharding(String shardingKey, String newkey,
                                        String... keys) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            int dbIndex = this.getDBIndex(newkey);
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {

                byte[][] bytekes = new byte[keys.length][];
                for (int i = 0; i < keys.length; i++) {
                    String key = keys[i];
                    bytekes[i] = SafeEncoder.encode(key);
                }
                jedis.zunionstore(SafeEncoder.encode(newkey), bytekes);
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
    }

    /**
     * 合集倒序
     */
    public Set<String> zunionstoreRevrangeByScoreString(List<String> keys,
                                                        double maxScore, double minScore, int offset, int count) {
        Set<byte[]> ret = zunionstoreRevrangeByScoreByteArr(keys, maxScore,
                minScore, offset, count);
        if (ret != null) {
            Set<String> trueRet = new LinkedHashSet<String>();
            for (byte[] item : ret) {
                trueRet.add(SafeEncoder.encode(item));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 合集倒序
     */
    public Set<Object> zunionstoreRevrangeByScoreObject(List<String> keys,
                                                        double maxScore, double minScore, int offset, int count, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = zunionstoreRevrangeByScoreByteArr(keys, maxScore,
                minScore, offset, count);
        if (ret != null) {
            Set<Object> trueRet = new LinkedHashSet<Object>();
            for (byte[] item : ret) {
                trueRet.add(deserialize(item, clazz));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 合集倒序
     */
    public Set<byte[]> zunionstoreRevrangeByScoreByteArr(List<String> keys,
                                                         double maxScore, double minScore, int offset, int count) {

        return zunionstoreRangeByScore(keys, minScore, maxScore, offset, count,
                false);

    }

    /**
     * 合集正序
     */
    public Set<String> zunionstoreRangeByScoreString(List<String> keys,
                                                     double minScore, double maxScore, int offset, int count) {
        Set<byte[]> ret = zunionstoreRangeByScoreByteArr(keys, minScore,
                maxScore, offset, count);
        if (ret != null) {
            Set<String> trueRet = new LinkedHashSet<String>();
            for (byte[] item : ret) {
                trueRet.add(SafeEncoder.encode(item));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 合集正序
     */
    public Set<Object> zunionstoreRangeByScoreObject(List<String> keys,
                                                     double minScore, double maxScore, int offset, int count, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = zunionstoreRangeByScoreByteArr(keys, minScore,
                maxScore, offset, count);
        if (ret != null) {
            Set<Object> trueRet = new LinkedHashSet<Object>();
            for (byte[] item : ret) {
                trueRet.add(deserialize(item, clazz));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 合集正序
     */
    public Set<byte[]> zunionstoreRangeByScoreByteArr(List<String> keys,
                                                      double minScore, double maxScore, int offset, int count) {

        return zunionstoreRangeByScore(keys, minScore, maxScore, offset, count,
                true);

    }

    private Set<byte[]> zunionstoreRangeByScore(List<String> keys,
                                                final double minScore, final double maxScore, final int offset,
                                                final int count, final boolean isasc) {
        if (keys == null || keys.size() == 0) {
            return null;
        }
        Set<byte[]> ret = new LinkedHashSet<byte[]>();
        final Map<String, List<String>> nodeToKeys = devideKeys(keys);
        final TreeSet<Tuple> treeSets = new TreeSet<Tuple>();

        final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet()
                .size());
        for (final String key : nodeToKeys.keySet()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<String> thisKeys = nodeToKeys.get(key);
                        if (thisKeys != null && thisKeys.size() > 0) {
                            Set<Tuple> tuples = zunionstoreTuple(thisKeys,
                                    minScore, maxScore, offset, count, isasc);
                            if (tuples != null) {
                                synchronized (treeSets) {
                                    treeSets.addAll(tuples);
                                }
                            }

                        }
                    } finally {
                        cdl.countDown();
                    }
                }
            });
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int size = treeSets.size();
        if (treeSets.size() > 0) {
            if (isasc) {
                for (int i = 0; i < size && i <= count; i++) {
                    ret.add(treeSets.pollFirst().getBinaryElement());
                }
            } else {
                for (int i = 0; i < size && i <= count; i++) {
                    ret.add(treeSets.pollLast().getBinaryElement());
                }
            }
        }

        return ret;
    }

    private Set<Tuple> zunionstoreTuple(List<String> keys, double minScore,
                                        double maxScore, int offset, int count, boolean isasc) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            int dbIndex = this.getDBIndex(keys.iterator().next());
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(keys.iterator().next());

            if (jedis != null) {
                byte[][] bytekes = new byte[keys.size()][];
                for (int i = 0; i < keys.size(); i++) {
                    String key = keys.get(i);
                    bytekes[i] = SafeEncoder.encode(key);
                }
                String newKey = UUID.randomUUID().toString();
                byte[] bytekey = SafeEncoder.encode(newKey);
                long start2 = System.currentTimeMillis();
                jedis.zunionstore(bytekey, bytekes);

                if (isasc) {
                    Set<Tuple> ret = jedis.zrangeByScoreWithScores(bytekey,
                            minScore, maxScore, offset, count);
                    jedis.del(bytekey);
                    return ret;
                } else {
                    Set<Tuple> ret = jedis.zrevrangeByScoreWithScores(bytekey,
                            maxScore, minScore, offset, count);
                    jedis.del(bytekey);
                    return ret;
                }
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /****************************** above:sorted set zrangeByScore、zrevrangeByScore *************************/

    /****************************** below:sorted set *************************/
    /****************************** above:sorted set *************************/

    /****************************** below:sorted set *************************/
    /****************************** above:sorted set *************************/

    /****************************** below:sorted set *************************/
    /****************************** above:sorted set *************************/

    /**
     * 对对象进行序列化
     * <p/>
     * o 必须实现Serializable接口
     */

    public byte[] serialize(Object o) {
        if (o instanceof ScloudSerializable) {
            return ScloudSerializeUtil.encode((ScloudSerializable) o);
        }
        return JedisXSerializeUtil.encode(o);
    }

    public byte[][] serialize(Object... os) {
        byte[][] result = new byte[os.length][];
        for (int i = 0; i < os.length; i++) {
            if (os[i] instanceof ScloudSerializable) {
                result[i] = ScloudSerializeUtil.encode((ScloudSerializable) os[i]);
            } else {
                result[i] = JedisXSerializeUtil.encode(os[i]);
            }
        }
        return result;
    }


    public Object deserialize(byte[] bytes, Class<? extends ScloudSerializable> clazz) {
        if (clazz != null) {
            return ScloudSerializeUtil.decode(bytes, clazz);
        } else {
            return JedisXSerializeUtil.decode(bytes);
        }
    }


    private byte[][] getBArrArr(List<String> thisKeys) {
        byte[][] bkeys = new byte[thisKeys.size()][];
        for (int i = thisKeys.size() - 1; i >= 0; i--) {
            bkeys[i] = SafeEncoder.encode(thisKeys.get(i));
        }
        return bkeys;
    }

    private byte[][] getKeyValueBArrArr(List<String> thisKeys,
                                        Map<String, byte[]> keyValueMap) {
        byte[][] bKeyValues = new byte[thisKeys.size() * 2][];
        for (int i = 0; i < thisKeys.size(); i++) {
            String key = thisKeys.get(i);
            bKeyValues[i + i] = SafeEncoder.encode(key);
            bKeyValues[i + i + 1] = keyValueMap.get(key);
        }
        return bKeyValues;
    }

    private void valueTypeAssert(Object value) {
        if (value == null) {
            throw new JedisXValueNotSupportException(
                    "nut support the Object-type of Null");
        }

    }

    /******************************** above:attr ********************************************/

    /****************************** below:sorted set ZADD、zaddMulti *************************/
    /**
     * Add the specified member having the specifeid score to the sorted set
     * stored at key. If member is already a member of the sorted set the score
     * is updated, and the element reinserted in the right position to ensure
     * sorting. If key does not exist a new sorted set with the specified member
     * as sole member is created. If the key exists but does not hold a sorted
     * set value an error is returned.
     * <p/>
     * The score value can be the string representation of a double precision
     * floating point number.
     * <p/>
     * Time complexity O(log(N)) with N being the number of elements in the
     * sorted set
     * <p/>
     * key score member Integer reply, specifically: 1 if the new element was
     * added; 0 if the element was already a member of the sorted set and the
     * score was updated ; -1 the error happened in server
     */

    public Long zaddString(String key, double score, String member) {
        return zaddByteArr(key, score, SafeEncoder.encode(member));
    }

    public Long zaddObject(String key, double score, Object member) {
        return zaddByteArr(key, score, serialize(member));
    }

    public Long zaddByteArr(String key, double score, byte[] member) {
        valueTypeAssert(member);

        return zadd(key, SafeEncoder.encode(key), score, member);

    }

    private Long zadd(String key, byte[] bytekey, double score, byte[] member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.zadd(bytekey, score, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /**
     * 往sorted set中插入多个元素
     * <p/>
     * key scoreMembers 成功插入元素的个数
     */

    public Long zaddMultiString(String key, Map<String, Double> memberScores) {
    	Map<byte[], Double> bMemberScores = new HashMap<byte[], Double>();
        for (Map.Entry<String, Double> item : memberScores.entrySet()) {
        	bMemberScores.put(SafeEncoder.encode(item.getKey()), item.getValue());
        }
        return zaddMultiByteArr(key, bMemberScores);
//    	Map<Double, byte[]> scoreBMembers = new HashMap<Double, byte[]>();
//        for (Map.Entry<Double, String> item : scoreMembers.entrySet()) {
//            if (item.getValue() != null) {
//                scoreBMembers.put(item.getKey(),
//                        SafeEncoder.encode(item.getValue()));
//            }
//        }
//        return zaddMultiByteArr(key, scoreBMembers);
    }

    /**
     * 往sorted set中插入多个元素
     * <p/>
     * key scoreMembers 成功插入元素的个数
     */
    public Long zaddMultiObject(String key, Map<Object, Double> memberScores) {
    	Map<byte[], Double> bMemberScores = new HashMap<byte[], Double>();
        for (Map.Entry<Object, Double> item : memberScores.entrySet()) {
            	bMemberScores.put(serialize(item.getKey()), item.getValue());
        }
        return zaddMultiByteArr(key, bMemberScores);
//        Map<Double, byte[]> scoreBMembers = new HashMap<Double, byte[]>();
//        for (Map.Entry<Double, Object> item : scoreMembers.entrySet()) {
//            if (item.getValue() != null) {
//                scoreBMembers.put(item.getKey(), serialize(item.getValue()));
//            }
//        }
//        return zaddMultiByteArr(key, scoreBMembers);
    }
    
    public Long saddMultiObject(String key, List<Object> members) {
    	byte[][] bMembers = new byte[members.size()][];
    	for(int i = 0; i < members.size(); i++) {
    		bMembers[i] = serialize(members.get(i));
    	}
    	return saddMultiByteArr(key, bMembers);
    }

    
    public Long saddMultiByteArr(String key, byte[][] members) {
        if (members == null || members.length == 0) {
            return 0L;
        }
        return sadd(key, SafeEncoder.encode(key), members);
    }
    
    /**
     * 往sorted set中插入多个元素
     * <p/>
     * key scoreMembers 成功插入元素的个数
     */
    public Long zaddMultiByteArr(String key, Map<byte[], Double> scoreMembers) {
        if (scoreMembers == null || scoreMembers.size() == 0) {
            return 0L;
        }

        return zadd(key, SafeEncoder.encode(key), scoreMembers);

    }
    
//    public Long zaddMultiByteArr(String key, Map<Double, byte[]> scoreMembers) {
//        if (scoreMembers == null || scoreMembers.size() == 0) {
//            return 0L;
//        }
//
//        return zadd(key, SafeEncoder.encode(key), scoreMembers);
//
//    }
    
    private long sadd(String key, byte[] bytekey, byte[][] members) {
    	ShardedJedis shardJedis = null;
		Jedis jedis = null;
		try {
			shardJedis = shardedJedisPool.getResource();
			jedis = shardJedis.getShard(key);
			if (jedis != null) {
				Long ret = jedis.sadd(bytekey, members);
				return ret;
			}
		} catch (Exception e) {
			onException(shardJedis, jedis, e);
			shardJedis = null;
		} finally {
			onFinally(shardJedis);
		}
		return 0L;
    }
    
	private Long zadd(String key, byte[] bytekey,
			Map<byte[], Double> scoreMembers) {
		ShardedJedis shardJedis = null;
		Jedis jedis = null;
		try {

			shardJedis = shardedJedisPool.getResource();
			jedis = shardJedis.getShard(key);

			if (jedis != null) {
				Long ret = jedis.zadd(bytekey, scoreMembers);
				return ret;
			}
		} catch (Exception e) {
			onException(shardJedis, jedis, e);
			shardJedis = null;
		} finally {
			onFinally(shardJedis);
		}
		return 0L;
	}

//    private Long zadd(String key, byte[] bytekey,
//                      Map<Double, byte[]> scoreMembers) {
//        ShardedJedis shardJedis = null;
//        Jedis jedis = null;
//        try {
//
//            shardJedis = shardedJedisPool.getResource();
//            jedis = shardJedis.getShard(key);
//
//            if (jedis != null) {
//                Long ret = jedis.zadd(bytekey, scoreMembers);
//                return ret;
//            }
//        } catch (Exception e) {
//            onException(shardJedis, jedis, e);
//            shardJedis = null;
//        } finally {
//            onFinally(shardJedis);
//        }
//        return 0L;
//    }

    /****************************** above:sorted set ZADD、zaddMulti *************************/

    /****************************** above:sorted set zremrangeByRank、 ZREM、zremMulti *************************/
    /**
     * 从有序集合中删除指定位置范围元素
     * <p/>
     * key start end 所删除的元素个数
     */
    
    

    public Long zremrangeByRank(String key, int start, int end) {
        return zremrangeByRank(key, SafeEncoder.encode(key), start, end);
    }
    
    public Long zremrangeByScore(String key, double minScore, double maxScore) {
        return zremrangeByScore(key, SafeEncoder.encode(key), minScore, maxScore);
    }
    
    private Long zremrangeByScore(String key, byte[] bytekey, double minScore,
    		double maxScore){
    	ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                Long ret = jedis.zremrangeByScore(bytekey, minScore, maxScore);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    private Long zremrangeByRank(String key, byte[] bytekey, int start, int end) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                Long ret = jedis.zremrangeByRank(bytekey, start, end);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /**
     * 从有序集合中删除指定元素
     * <p/>
     * key member 0，未找到该元素删除不成功； 1，找到并删除成功
     */

    public Long zremString(String key, String member) {
        return zremByteArr(key, SafeEncoder.encode(member));
    }

    /**
     * 从有序集合中删除指定元素
     * <p/>
     * key member 0，未找到该元素删除不成功； 1，找到并删除成功
     */

    public Long zremObject(String key, Object member) {
        return zremByteArr(key, serialize(member));
    }

    /**
     * 从有序集合中删除指定元素
     * <p/>
     * key member 0，未找到该元素删除不成功； 1，找到并删除成功
     */

    public Long zremByteArr(String key, byte[] member) {
        if (member == null) {
            return 0L;
        }

        return zrem(key, SafeEncoder.encode(key), member);

    }

    /**
     * 从有序集合中删除多个指定元素
     * <p/>
     * key memberList 成功删除的元素个数
     */

    public Long zremMultiString(String key, List<String> memberList) {
        List<byte[]> bArrList = new ArrayList<byte[]>();
        for (String member : memberList) {
            if (member != null) {
                bArrList.add(SafeEncoder.encode(member));
            }
        }
        return zremMultiByteArr(key, bArrList);
    }

    /**
     * 从有序集合中删除多个指定元素
     * <p/>
     * key memberList 成功删除的元素个数
     */

    public Long zremMultiObject(String key, List<Object> memberList) {
        List<byte[]> bArrList = new ArrayList<byte[]>();
        for (Object member : memberList) {
            if (member != null) {
                bArrList.add(serialize(member));
            }
        }
        return zremMultiByteArr(key, bArrList);
    }

    /**
     * 从有序集合中删除多个指定元素
     * <p/>
     * key memberList 成功删除的元素个数
     */

    public Long zremMultiByteArr(String key, List<byte[]> memberList) {
        byte[][] memberB = new byte[memberList.size()][];
        for (int i = memberList.size() - 1; i >= 0; i--) {
            memberB[i] = memberList.get(i);
        }

        return zrem(key, SafeEncoder.encode(key), memberB);

    }

    private Long zrem(String key, byte[] bytekey, byte[]... member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.zrem(bytekey, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /****************************** above:sorted set zremrangeByRank、ZREM、zremMulti *************************/

    /************************** below:sorted set ZCARD、ZCOUNT *********************/
    /**
     * 获取sorted set 中元素个数
     * <p/>
     * key
     */

    public Long zcard(String key) {

        return zcard(key, SafeEncoder.encode(key));

    }

    /**
     * 获取sorted set 中指定范围内的元素个数
     * <p/>
     * key minScore maxScore
     */

    public Long zcount(String key, double minScore, double maxScore) {

        return zcount(key, SafeEncoder.encode(key), minScore, maxScore);

    }

    private Long zcard(String key, byte[] bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.zcard(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    private Long zcount(String key, byte[] bytekey, double minScore,
                        double maxScore) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Long ret = jedis.zcount(bytekey, minScore, maxScore);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /**************************** above:sorted set ZCARD、ZCOUNT *********************/

    /****************************** below:sorted set ZRANK,ZREVRANK、ZSCORE *************************/
    /**
     * 返回元素在有序集合（从小到大）中的序号（从0开始）
     * <p/>
     * key member 如果元素存在时返回序号，元素不存在时返回null
     */

    public Long zrankString(String key, String member) {
        return zrankByteArr(key, SafeEncoder.encode(member));
    }

    /**
     * 返回元素在有序集合（从小到大）中的序号（从0开始）
     * <p/>
     * key member 如果元素存在时返回序号，元素不存在时返回null
     */

    public Long zrankObject(String key, Object member) {
        return zrankByteArr(key, serialize(member));
    }

    /**
     * 返回元素在有序集合（从小到大）中的序号（从0开始）
     * <p/>
     * key member 如果元素存在时返回序号，元素不存在时返回null
     */

    public Long zrankByteArr(String key, byte[] member) {
        valueTypeAssert(member);

        return zrank(key, SafeEncoder.encode(key), member, true);

    }

    /**
     * 返回元素在有序集合（从大到小）中的序号（从0开始）
     * <p/>
     * key member 如果元素存在时返回序号，元素不存在时返回null
     */

    public Long zrevrankString(String key, String member) {
        return zrevrankByteArr(key, SafeEncoder.encode(member));
    }

    /**
     * 返回元素在有序集合（从大到小）中的序号（从0开始）
     * <p/>
     * key member 如果元素存在时返回序号，元素不存在时返回null
     */

    public Long zrevrankObject(String key, Object member) {
        return zrevrankByteArr(key, serialize(member));
    }

    /**
     * 返回元素在有序集合（从大到小）中的序号（从0开始）
     * <p/>
     * key member 如果元素存在时返回序号，元素不存在时返回null
     */

    public Long zrevrankByteArr(String key, byte[] member) {
        valueTypeAssert(member);

        return zrank(key, SafeEncoder.encode(key), member, false);

    }

    private Long zrank(String key, byte[] bytekey, byte[] member, boolean isasc) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                if (isasc) {
                    Long ret = jedis.zrank(bytekey, member);
                    return ret;
                } else {
                    Long ret = jedis.zrevrank(bytekey, member);
                    return ret;
                }
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /**
     * 返回元素在有序集合中的排序因子
     * <p/>
     * key member 如果元素存在时返回排序因子score，元素不存在时返回null
     */

    public Double zscoreString(String key, String member) {
        return zscoreByteArr(key, SafeEncoder.encode(member));
    }

    /**
     * 返回元素在有序集合中的排序因子
     * <p/>
     * key member 如果元素存在时返回排序因子score，元素不存在时返回null
     */

    public Double zscoreObject(String key, Object member) {
        return zscoreByteArr(key, serialize(member));
    }

    /**
     * 返回元素在有序集合中的排序因子
     * <p/>
     * key member 如果元素存在时返回排序因子score，元素不存在时返回null
     */

    public Double zscoreByteArr(String key, byte[] member) {
        valueTypeAssert(member);

        return zscore(key, SafeEncoder.encode(key), member);

    }

    private Double zscore(String key, byte[] bytekey, byte[] member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Double ret = jedis.zscore(bytekey, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /****************************** above:sorted set ZRANK,ZREVRANK、ZSCORE *************************/

    /*************************** below:sorted set ZRANGE、ZREVRANGE *******************/
    /**
     * 获取指定位置范围内的升序集合
     * <p/>
     * key start end LinkedHashSet结果集
     */

    public Set<String> zrangeString(String key, int start, int end) {
        Set<byte[]> ret = zrangeByteArr(key, start, end);
        if (ret != null) {
            Set<String> trueRet = new LinkedHashSet<String>(); // used
            // LinkedHashSet
            // to ensure it
            // is in order
            for (byte[] item : ret) {
                trueRet.add(SafeEncoder.encode(item));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定位置范围内的升序集合
     * <p/>
     * key start end LinkedHashSet结果集
     */

    public Set<Object> zrangeObject(String key, int start, int end, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = zrangeByteArr(key, start, end);
        if (ret != null) {
            Set<Object> trueRet = new LinkedHashSet<Object>();
            for (byte[] item : ret) {
                trueRet.add(deserialize(item, clazz));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定位置范围内的升序集合
     * <p/>
     * key start end LinkedHashSet结果集
     */

    public Set<byte[]> zrangeByteArr(String key, int start, int end) {

        return zrange(key, SafeEncoder.encode(key), start, end, true);

    }

    /**
     * 获取指定位置范围内的降序集合
     * <p/>
     * key start end LinkedHashSet结果集
     */

    public Set<String> zrevrangeString(String key, int start, int end) {
        Set<byte[]> ret = zrevrangeByteArr(key, start, end);
        if (ret != null) {
            Set<String> trueRet = new LinkedHashSet<String>();
            for (byte[] item : ret) {
                trueRet.add(SafeEncoder.encode(item));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定位置范围内的降序集合
     * <p/>
     * key start end LinkedHashSet结果集
     */

    public Set<Object> zrevrangeObject(String key, int start, int end, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = zrevrangeByteArr(key, start, end);
        if (ret != null) {
            Set<Object> trueRet = new LinkedHashSet<Object>();
            for (byte[] item : ret) {
                trueRet.add(deserialize(item, clazz));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定位置范围内的降序集合
     * <p/>
     * key start end LinkedHashSet结果集
     */

    public Set<byte[]> zrevrangeByteArr(String key, int start, int end) {

        return zrange(key, SafeEncoder.encode(key), start, end, false);

    }

    private Set<byte[]> zrange(String key, byte[] bytekey, int start, int end,
                               boolean isasc) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                if (isasc) {
                    Set<byte[]> ret = jedis.zrange(bytekey, start, end);
                    return ret;
                } else {
                    Set<byte[]> ret = jedis.zrevrange(bytekey, start, end);
                    return ret;
                }
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /*************************** above:sorted set ZRANGE、ZREVRANGE *******************/

    /****************************** below:sorted set zrangeByScore、zrevrangeByScore *************************/
    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore LinkedHashSet 升序结果集
     */

    public Set<String> zrangeByScoreString(String key, double minScore,
                                           double maxScore) {
        Set<byte[]> ret = zrangeByScoreByteArr(key, minScore, maxScore);
        if (ret != null) {
            Set<String> trueRet = new LinkedHashSet<String>();
            for (byte[] item : ret) {
                trueRet.add(SafeEncoder.encode(item));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore LinkedHashSet升序结果集
     */

    public Set<Object> zrangeByScoreObject(String key, double minScore,
                                           double maxScore, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = zrangeByScoreByteArr(key, minScore, maxScore);
        if (ret != null) {
            Set<Object> trueRet = new LinkedHashSet<Object>();
            for (byte[] item : ret) {
                trueRet.add(deserialize(item, clazz));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore LinkedHashSet升序结果集
     */

    public Set<byte[]> zrangeByScoreByteArr(String key, double minScore,
                                            double maxScore) {

        return zrangeByScore(key, SafeEncoder.encode(key), minScore, maxScore,
                true);

    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore LinkedHashSet 降序结果集
     */

    public Set<String> zrevrangeByScoreString(String key, double maxScore,
                                              double minScore) {
        Set<byte[]> ret = zrevrangeByScoreByteArr(key, maxScore, minScore);
        if (ret != null) {
            Set<String> trueRet = new LinkedHashSet<String>();
            for (byte[] item : ret) {
                trueRet.add(SafeEncoder.encode(item));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore LinkedHashSet降序结果集
     */

    public Set<Object> zrevrangeByScoreObject(String key, double maxScore,
                                              double minScore, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = zrevrangeByScoreByteArr(key, maxScore, minScore);
        if (ret != null) {
            Set<Object> trueRet = new LinkedHashSet<Object>();
            for (byte[] item : ret) {
                trueRet.add(deserialize(item, clazz));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore LinkedHashSet降序结果集
     */

    public Set<byte[]> zrevrangeByScoreByteArr(String key, double maxScore,
                                               double minScore) {

        return zrangeByScore(key, SafeEncoder.encode(key), minScore, maxScore,
                false);

    }

    private Set<byte[]> zrangeByScore(String key, byte[] bytekey,
                                      double minScore, double maxScore, boolean isasc) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                if (isasc) {
                    Set<byte[]> ret = jedis.zrangeByScore(bytekey, minScore,
                            maxScore);
                    return ret;
                } else {
                    Set<byte[]> ret = jedis.zrevrangeByScore(bytekey, maxScore,
                            minScore);
                    return ret;
                }
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore offset count LinkedHashSet 升序结果集
     */

    public Set<String> zrangeByScoreString(String key, double minScore,
                                           double maxScore, int offset, int count) {
        Set<byte[]> ret = zrangeByScoreByteArr(key, minScore, maxScore, offset,
                count);
        if (ret != null) {
            Set<String> trueRet = new LinkedHashSet<String>();
            for (byte[] item : ret) {
                trueRet.add(SafeEncoder.encode(item));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore offset count LinkedHashSet升序结果集
     */

    public Set<Object> zrangeByScoreObject(String key, double minScore,
                                           double maxScore, int offset, int count, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = zrangeByScoreByteArr(key, minScore, maxScore, offset,
                count);
        if (ret != null) {
            Set<Object> trueRet = new LinkedHashSet<Object>();
            for (byte[] item : ret) {
                trueRet.add(deserialize(item, clazz));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore offset count LinkedHashSet升序结果集
     */

    public Set<byte[]> zrangeByScoreByteArr(String key, double minScore,
                                            double maxScore, int offset, int count) {

        return zrangeByScore(key, SafeEncoder.encode(key), minScore, maxScore,
                offset, count, true);

    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore offset count LinkedHashSet 降序结果集
     */

    public Set<String> zrevrangeByScoreString(String key, double maxScore,
                                              double minScore, int offset, int count) {
        Set<byte[]> ret = zrevrangeByScoreByteArr(key, maxScore, minScore,
                offset, count);
        if (ret != null) {
            Set<String> trueRet = new LinkedHashSet<String>();
            for (byte[] item : ret) {
                trueRet.add(SafeEncoder.encode(item));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore offset count LinkedHashSet降序结果集
     */

    public Set<Object> zrevrangeByScoreObject(String key, double maxScore,
                                              double minScore, int offset, int count, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = zrevrangeByScoreByteArr(key, maxScore, minScore,
                offset, count);
        if (ret != null) {
            Set<Object> trueRet = new LinkedHashSet<Object>();
            for (byte[] item : ret) {
                trueRet.add(deserialize(item, clazz));
            }
            return trueRet;
        }
        return null;
    }

    /**
     * 获取指定排序因子score范围内的元素
     * <p/>
     * key minScore maxScore offset count LinkedHashSet降序结果集
     */

    public Set<byte[]> zrevrangeByScoreByteArr(String key, double maxScore,
                                               double minScore, int offset, int count) {

        return zrangeByScore(key, SafeEncoder.encode(key), minScore, maxScore,
                offset, count, false);

    }

    private Set<byte[]> zrangeByScore(String key, byte[] bytekey,
                                      double minScore, double maxScore, int offset, int count,
                                      boolean isasc) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                if (isasc) {
                    Set<byte[]> ret = jedis.zrangeByScore(bytekey, minScore,
                            maxScore, offset, count);
                    return ret;
                } else {
                    Set<byte[]> ret = jedis.zrevrangeByScore(bytekey, maxScore,
                            minScore, offset, count);
                    return ret;
                }
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /****************************** above:sorted set zrangeByScore、zrevrangeByScore *************************/

    /******************************* below:set sadd,srem,spop,smember **********************/
    /**
     * 往集合中插入member
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was added 0
     * if the element was already a member of the set
     */

    public Long saddStringWithSharding(String shardingKey, String key,
                                       String member) {
        return saddByteArrWithSharding(shardingKey, key,
                SafeEncoder.encode(member));
    }

    /**
     * 往集合中插入member(复杂对象是否适应于此方法有待测试验证)
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was added 0
     * if the element was already a member of the set
     */

    public Long saddObjectWithSharding(String shardingKey, String key,
                                       Object member) {
        return saddByteArrWithSharding(shardingKey, key, serialize(member));
    }

    /**
     * 往集合中插入member
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was added 0
     * if the element was already a member of the set
     */

    public Long saddByteArrWithSharding(String shardingKey, String key,
                                        byte[] member) {
        valueTypeAssert(member);

        return saddWithSharding(shardingKey, key, SafeEncoder.encode(key),
                member);

    }

    private Long saddWithSharding(String shardingKey, String key,
                                  byte[] bytekey, byte[] member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                Long ret = jedis.sadd(bytekey, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /******************************* below:set sadd,srem,spop,smember **********************/
    /**
     * 往集合中插入member
     * <p/>
     * key Integer reply, specifically: 1 if the new element was added 0 if the
     * element was already a member of the set
     */

    public Long smultiAddStringWithSharding(String shardingKey, String key,
                                            String... members) {
        return smultiAddByteArrWithSharding(shardingKey, key,
                SafeEncoder.encodeMany(members));
    }

    /**
     * 往集合中插入member(复杂对象是否适应于此方法有待测试验证)
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was added 0
     * if the element was already a member of the set
     */

    public Long smultiAddObjectWithSharding(String shardingKey, String key,
                                            Object... members) {
        return smultiAddByteArrWithSharding(shardingKey, key,
                serialize(members));
    }

    /**
     * 往集合中插入member
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was added 0
     * if the element was already a member of the set
     */

    public Long smultiAddByteArrWithSharding(String shardingKey, String key,
                                             byte[][] members) {
        valueTypeAssert(members);

        return smultiAddWithSharding(shardingKey, key, SafeEncoder.encode(key),
                members);

    }

    private Long smultiAddWithSharding(String shardingKey, String key,
                                       byte[] bytekey, byte[][] member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                Long ret = jedis.sadd(bytekey, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /**
     * 删除集合中的member成员
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was removed
     * 0 if the new element was not a member of the set
     */

    public Long sremStringWithSharding(String shardingKey, String key,
                                       String member) {
        return sremByteArrWithSharding(shardingKey, key,
                SafeEncoder.encode(member));
    }

    /**
     * 删除集合中的member成员（复杂对象是否适合此方法有待测试）
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was removed
     * 0 if the new element was not a member of the set
     */

    public Long sremObjectWithSharding(String shardingKey, String key,
                                       Object member) {
        return sremByteArrWithSharding(shardingKey, key, serialize(member));
    }

    /**
     * 删除集合中的member成员
     * <p/>
     * key member Integer reply, specifically: 1 if the new element was removed
     * 0 if the new element was not a member of the set
     */

    public Long sremByteArrWithSharding(String shardingKey, String key,
                                        byte[] member) {
        valueTypeAssert(member);

        return sremWithSharding(shardingKey, key, SafeEncoder.encode(key),
                member);

    }

    public Long smultiRemByteArrWithSharding(String shardingKey, String key,
                                             byte[][] members) {
        valueTypeAssert(members);
        return smultiRemWithSharding(shardingKey, key, SafeEncoder.encode(key),
                members);
    }

    public Long smultiRemObjectWithSharding(String shardingKey, String key,
                                            Object... members) {
        return smultiRemByteArrWithSharding(shardingKey, key,
                serialize(members));
    }

    public Long smultiRemStringWithSharding(String shardingKey, String key,
                                            String... members) {
        return smultiRemByteArrWithSharding(shardingKey, key,
                SafeEncoder.encodeMany(members));
    }

    private Long smultiRemWithSharding(String shardingKey, String key,
                                       byte[] bytekey, byte[][] member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                Long ret = jedis.srem(bytekey, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    private Long sremWithSharding(String shardingKey, String key,
                                  byte[] bytekey, byte[] member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                Long ret = jedis.srem(bytekey, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /**
     * Remove and return a random member from a set
     * <p/>
     * key
     */

    public String spopStringWithSharding(String shardingKey, String key) {
        byte[] ret = spopByteArrWithSharding(shardingKey, key);
        if (ret != null) {
            return SafeEncoder.encode(ret);
        }
        return null;
    }

    /**
     * Remove and return a random member from a set
     * <p/>
     * key
     */

    public Object spopObjectWithSharding(String shardingKey, String key, final Class<? extends ScloudSerializable> clazz) {
        byte[] ret = spopByteArrWithSharding(shardingKey, key);
        if (ret != null) {
            return deserialize(ret, clazz);
        }
        return null;
    }

    /**
     * Remove and return a random member from a set
     * <p/>
     * key
     */

    public byte[] spopByteArrWithSharding(String shardingKey, String key) {

        return spopWithSharding(shardingKey, key, SafeEncoder.encode(key));

    }

    private byte[] spopWithSharding(String shardingKey, String key,
                                    byte[] bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                byte[] ret = jedis.spop(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /**
     * Get all the members in a set
     * <p/>
     * key
     */

    public Set<String> smemberStringWithSharding(String shardingKey, String key) {
        Set<byte[]> ret = smemberByteArrWithSharding(shardingKey, key);
        if (ret != null) {
            Set<String> trueRet = new HashSet<String>();
            for (byte[] member : ret) {
                if (member != null) {
                    trueRet.add(SafeEncoder.encode(member));
                }
            }
            return trueRet;
        }
        return null;
    }

    /**
     * Get all the members in a set
     * <p/>
     * key
     */

    public Set<Object> smemberObjectWithSharding(String shardingKey, String key, final Class<? extends ScloudSerializable> clazz) {
        Set<byte[]> ret = smemberByteArrWithSharding(shardingKey, key);
        if (ret != null) {
            Set<Object> trueRet = new HashSet<Object>();
            for (byte[] member : ret) {
                if (member != null) {
                    trueRet.add(deserialize(member, clazz));
                }
            }
            return trueRet;
        }
        return null;
    }

    /**
     * Get all the members in a set
     * <p/>
     * key
     */

    public Set<byte[]> smemberByteArrWithSharding(String shardingKey, String key) {

        return smemberWithSharding(shardingKey, key, SafeEncoder.encode(key));

    }

    private Set<byte[]> smemberWithSharding(String shardingKey, String key,
                                            byte[] bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                Set<byte[]> ret = jedis.smembers(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    /******************************** above:set sadd,srem,spop,smember *************************/

    /******************************* below:set scard,sismember *******************************/
    /**
     * 判断某个member是否在此集合中
     * <p/>
     * key member
     */

    public Boolean sismemberStringWithSharding(String shardingKey, String key,
                                               String member) {
        return sismemberByteArrWithSharding(shardingKey, key,
                SafeEncoder.encode(member));
    }

    /**
     * 判断某个member是否在此集合中
     * <p/>
     * key member
     */

    public Boolean sismemberObjectWithSharding(String shardingKey, String key,
                                               Object member) {
        return sismemberByteArrWithSharding(shardingKey, key, serialize(member));
    }

    /**
     * 判断某个member是否在此集合中
     * <p/>
     * key member
     */

    public Boolean sismemberByteArrWithSharding(String shardingKey, String key,
                                                byte[] member) {
        if (member == null) {
            return false;
        }

        return sismemberWithSharding(shardingKey, key, SafeEncoder.encode(key),
                member);

    }

    private Boolean sismemberWithSharding(String shardingKey, String key,
                                          byte[] bytekey, byte[] member) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                Boolean ret = jedis.sismember(bytekey, member);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return false;
    }

    /**
     * Get the number of members in a set
     * <p/>
     * key
     */

    public Long scardWithSharding(String shardingKey, String key) {

        return scardWithSharding(shardingKey, key, SafeEncoder.encode(key));

    }

    private Long scardWithSharding(String shardingKey, String key,
                                   byte[] bytekey) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                Long ret = jedis.scard(bytekey);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return 0L;
    }

    /**
     * 计算给定的一个或多个有序集的交集，并将该交集(结果集)储存到 newkey 。 默认情况下，结果集中某个成员的 score 值是所有给定集下该成员
     * score 值之和.
     * <p/>
     * shardingKey 排序的key，确保所有的集合在一台redis上面 newkey 新创建的key keys 需要操作的集合的key
     */
    public void sinterstoreWithSharding(String shardingKey, String newkey,
                                        String... keys) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(shardingKey);

            if (jedis != null) {
                byte[][] bytekes = new byte[keys.length][];
                for (int i = 0; i < keys.length; i++) {
                    String key = keys[i];
                    bytekes[i] = SafeEncoder.encode(key);
                }
                jedis.sinterstore(SafeEncoder.encode(newkey), bytekes);
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }

    }

    /******************************** above:set scard,sismember *******************************/

    /****************************** below: set sinter，sinterstore *************************/

    /**
     * *************************** above: set sinter，sinterstore
     * ************************
     */
    
    
    
    public Map<String, Set<String>> pipSPop(List<String> keys) {
    	final Map<String, Set<String>> res = Maps.newHashMap();
    	final Map<String, List<String>> nodeToKeys = devideKeys(keys);
    	final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet().size());
    	for (final String key : nodeToKeys.keySet()) {
    		 executorService.execute(new Runnable() {
				@Override
				public void run() {
					try {
						List<String> keys = nodeToKeys.get(key);
						Map<String, Set<String>> thisret = pipSPop(key, keys);
						if (thisret != null) {
							res.putAll(thisret);
	                    }
					} finally {
						cdl.countDown();
					}
				}
			});
    	}
    	try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return res;
    }
    
    
    private Map<String, Set<String>> pipSPop(String key, List<String> keys) {
    	Map<String, Set<String>> res = Maps.newHashMap();
    	ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
        	 shardJedis = shardedJedisPool.getResource();
             jedis = shardJedis.getShard(key);
             if (jedis != null) {
            	 Pipeline pipeline = jedis.pipelined();
            	 for(String one : keys){
            		 pipeline.spop(SafeEncoder.encode(one));
            	 }
            	 List<Object> ret = pipeline.syncAndReturnAll();
            	 for (int i = 0; i < keys.size(); i++) {
                     if (ret.get(i) != null) {
                     	String one = keys.get(i);
                     	byte[] value = (byte[]) ret.get(i);
                     	Set<String> set = res.get(one);
                     	if(null == set){
                     		set = Sets.newHashSet();
                     		res.put(one, set);
                     	}
                     	set.add(SafeEncoder.encode(value));
                     }
                 }
            	 return res;
             }
        } catch(Exception e) {
        	onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
        	onFinally(shardJedis);
        }
        return res;
    }
    
    public Map<String, Long> pipZcard(List<String> keys){
    	final Map<String, Long> ret = new ConcurrentHashMap<String, Long>();
        final Map<String, List<String>> nodeToKeys = devideKeys(keys);
        final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet()
                .size());
        for (final String key : nodeToKeys.keySet()) {
   		 executorService.execute(new Runnable() {
				@Override
				public void run() {
					try {
						List<String> keys = nodeToKeys.get(key);
						Map<String, Long> thisret = pipZcard(key, keys);
						if (thisret != null) {
							ret.putAll(thisret);
	                    }
					} finally {
						cdl.countDown();
					}
				}
			});
   	     }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
    }
    
    private Map<String, Long> pipZcard(String key, List<String> keys) {
        Map<String, Long> map = new HashMap<String, Long>();
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for (String subKey : keys) {
                    pipeline.zcard(SafeEncoder.encode(subKey));
                }
                List<Object> ret = pipeline.syncAndReturnAll();
                for (int i = ret.size() - 1; i >= 0; i--) {
                    if (ret.get(i) != null)
                        map.put(keys.get(i), (Long) ret.get(i));
                }
                return map;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return map;
    }
    
    private Map<String, Set<Object>> pipSmembersObject(String key, List<String> params) {
    	Map<String, Set<Object>> res = Maps.newHashMap();
    	Map<String, Set<byte[]>> map = this.pipSmembersByte(key, params);
    	for(Entry<String, Set<byte[]>> entry : map.entrySet()) {
    		Set<Object> objSet = Sets.newLinkedHashSet();
    		Set<byte[]> set = entry.getValue();
    		for(byte[] one : set) {
    			objSet.add(JedisXSerializeUtil.decode(one));
    		}
    		res.put(entry.getKey(), objSet);
    	}
    	return res;
    }
    
    private Map<RedisSortedSetParam, Set<Object>> pipZrangeByScoreObject(String key,
    		List<RedisSortedSetParam> params) {
    	Map<RedisSortedSetParam, Set<Object>> res = Maps.newHashMap();
    	Map<RedisSortedSetParam, Set<byte[]>> map = this.pipZrangeByScoreByte(key, params);
    	for(Entry<RedisSortedSetParam, Set<byte[]>> entry : map.entrySet()) {
    		Set<Object> objSet = Sets.newLinkedHashSet();
    		Set<byte[]> set = entry.getValue();
    		for(byte[] one : set) {
    			objSet.add(JedisXSerializeUtil.decode(one));
    		}
    		res.put(entry.getKey(), objSet);
    	}
    	return res;
    } 
    
    private Map<RedisSortedSetParam, Set<String>> pipZrangeByScoreString(String key, 
    		List<RedisSortedSetParam> params) {
    	Map<RedisSortedSetParam, Set<String>> res = Maps.newHashMap();
    	Map<RedisSortedSetParam, Set<byte[]>> map = this.pipZrangeByScoreByte(key, params);
    	for(Entry<RedisSortedSetParam, Set<byte[]>> entry : map.entrySet()) {
    		Set<String> strSet = Sets.newLinkedHashSet();
    		Set<byte[]> set = entry.getValue();
    		for(byte[] one : set) {
    			strSet.add(SafeEncoder.encode(one));
    		}
    		res.put(entry.getKey(), strSet);
    	}
    	return res;
    }
    
    private Map<RedisAddSetParam, Long> pipSaddByte(String key, List<RedisAddSetParam> params) {
    	Map<RedisAddSetParam, Long> map = Maps.newHashMap();
    	 ShardedJedis shardJedis = null;
         Jedis jedis = null;
         try {
        	 shardJedis = shardedJedisPool.getResource();
        	 jedis = shardJedis.getShard(key);
        	 if (jedis != null) {
        		 Pipeline pipeline = jedis.pipelined();
        		 for (RedisAddSetParam param : params) {
        			 byte[] keyBytes = SafeEncoder.encode(param.getKey());
        			 List<Object> members = param.getMembers();
        			 byte[][] bMembers = new byte[members.size()][];
        		     for(int i = 0; i < members.size(); i++) {
        		    	bMembers[i] = serialize(members.get(i));
        		     }
        		     pipeline.sadd(keyBytes, bMembers);
        		 }
        		 List<Object> ret = pipeline.syncAndReturnAll();
        		 for (int i = 0; i < params.size(); i++) {
                     if (ret.get(i) != null) {
                    	RedisAddSetParam param = params.get(i);
                     	Long value = (Long) ret.get(i);
                     	map.put(param, value);
                     }
                 }
                 return map;
        	 }
		} catch (Exception e) {
			onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return map;
    }
    
    private Map<String, Set<byte[]>> pipSmembersByte(String key, List<String> params) {
    	Map<String, Set<byte[]>> map = Maps.newHashMap();
    	ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
        	shardJedis = shardedJedisPool.getResource();
        	jedis = shardJedis.getShard(key);
        	if (jedis != null) {
        		Pipeline pipeline = jedis.pipelined();
        		for (String param : params) {
        			pipeline.smembers(SafeEncoder.encode(param));
        		}
        		List<Object> ret = pipeline.syncAndReturnAll();
        		for (int i = 0; i < params.size(); i++) {
                    if (ret.get(i) != null) {
                    	String param = params.get(i);
                    	Set<byte[]> value = (Set<byte[]>) ret.get(i);
                    	map.put(param, value);
                    }
                }
        		return map;
        	}
		} catch (Exception e) {
			onException(shardJedis, jedis, e);
            shardJedis = null;
		} finally {
            onFinally(shardJedis);
        }
        return map;
    }
    
	@SuppressWarnings("unchecked")
	private Map<RedisSortedSetParam, Set<byte[]>> pipZrangeByScoreByte(String key, 
    		List<RedisSortedSetParam> params) {
    	Map<RedisSortedSetParam, Set<byte[]>> map = Maps.newHashMap();
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            
            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for (RedisSortedSetParam param : params) {
                	if (param.isIsasc()) {
                		pipeline.zrangeByScore(SafeEncoder.encode(param.getKey()),
                				param.getMinScore(), param.getMaxScore(),
                				param.getOffset(), param.getCount());
    				} else {
    					pipeline.zrevrangeByScore(SafeEncoder.encode(param.getKey()),
    							param.getMaxScore(), param.getMinScore(),
    							param.getOffset(), param.getCount());
    				}
                }
                List<Object> ret = pipeline.syncAndReturnAll();
                for (int i = 0; i < params.size(); i++) {
                    if (ret.get(i) != null) {
                    	RedisSortedSetParam param = params.get(i);
                    	Set<byte[]> value = (Set<byte[]>) ret.get(i);
                    	map.put(param, value);
                    }
                }
                return map;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return map;
    }
    
    public Map<RedisSortedSetZremrangeParam, Long> pipZremrangeByRank(List<RedisSortedSetZremrangeParam> params){
    	final Map<RedisSortedSetZremrangeParam, Long> ret = Maps.newHashMap();
    	List<String> keys = Lists.newArrayList();
    	final Map<String, RedisSortedSetZremrangeParam> map_param = Maps.newHashMap();
		for(RedisSortedSetZremrangeParam param : params){
			map_param.put(param.getKey(), param);
			keys.add(param.getKey());
		}
		final Map<String, List<String>> nodeToKeys = devideKeys(keys);
		final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet().size());
		for (final String key : nodeToKeys.keySet()) {
			 executorService.execute(new Runnable() {
				@Override
				public void run() {
					try {
						List<String> keys = nodeToKeys.get(key);
						List<RedisSortedSetZremrangeParam> params = Lists.newArrayList();
						for(String one : keys){
							params.add(map_param.get(one));
						}
						Map<RedisSortedSetZremrangeParam, Long> thisret = 
								pipZremrangeByRank(key, params);
	                    if (thisret != null) {
	                        ret.putAll(thisret);
	                    }
					} finally {
						cdl.countDown();
					}
				}
			 });
		}
		try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
    }
    
    private Map<RedisSortedSetZremrangeParam, Long> pipZremrangeByRank(String key, 
    		List<RedisSortedSetZremrangeParam> params) {
    	Map<RedisSortedSetZremrangeParam, Long> map = Maps.newHashMap();
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for (RedisSortedSetZremrangeParam param : params) {
                	pipeline.zremrangeByRank(SafeEncoder.encode(param.getKey()),
                			param.getStart(), param.getEnd());
                }
                List<Object> ret = pipeline.syncAndReturnAll();
                for (int i = 0; i < params.size(); i++) {
                    if (ret.get(i) != null) {
                    	RedisSortedSetZremrangeParam param = params.get(i);
                    	Long value = (Long) ret.get(i);
                    	map.put(param, value);
                    }
                }
                return map;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return map;
    }
    
    public Map<RedisDecrParam, Long> pipDecr(List<RedisDecrParam> params){
    	final Map<RedisDecrParam, Long> ret = Maps.newLinkedHashMap();
		final Map<String, List<RedisDecrParam>> nodeToKeys = devideKeysDecr(params);
		final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet().size());
		for (final String key : nodeToKeys.keySet()) {
			 executorService.execute(new Runnable() {
				@Override
				public void run() {
					try {
						List<RedisDecrParam> params = nodeToKeys.get(key);
						Map<RedisDecrParam, Long> thisret = pipDecr(key, params);
	                    if (thisret != null) {
	                        ret.putAll(thisret);
	                    }
					} finally {
						cdl.countDown();
					}
				}
			 });
		}
		try {
           cdl.await();
       } catch (InterruptedException e) {
           e.printStackTrace();
       }
       return ret;
	}
    
    public Map<RedisIncrParam, Long> pipIncr(List<RedisIncrParam> params){
    	final Map<RedisIncrParam, Long> ret = Maps.newLinkedHashMap();
		final Map<String, List<RedisIncrParam>> nodeToKeys = devideKeysIncr(params);
		final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet().size());
		for (final String key : nodeToKeys.keySet()) {
			 executorService.execute(new Runnable() {
				@Override
				public void run() {
					try {
						List<RedisIncrParam> params = nodeToKeys.get(key);
						Map<RedisIncrParam, Long> thisret = pipIncr(key, params);
	                    if (thisret != null) {
	                        ret.putAll(thisret);
	                    }
					} finally {
						cdl.countDown();
					}
				}
			 });
		}
		try {
           cdl.await();
       } catch (InterruptedException e) {
           e.printStackTrace();
       }
       return ret;
	}
    
    private Map<RedisDecrParam, Long> pipDecr(String key, List<RedisDecrParam> params) {
    	Map<RedisDecrParam, Long> map = Maps.newLinkedHashMap();
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for (RedisDecrParam param : params) {
                	pipeline.decr(SafeEncoder.encode(param.getKey()));
                }
                List<Object> ret = pipeline.syncAndReturnAll();
                for (int i = 0; i < params.size(); i++) {
                    if (ret.get(i) != null) {
                    	RedisDecrParam param = params.get(i);
                    	Long value = (Long) ret.get(i);
                    	map.put(param, value);
                    }
                }
                return map;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
       
        return map;
    }
	
	private Map<RedisIncrParam, Long> pipIncr(String key, List<RedisIncrParam> params) {
		Map<RedisIncrParam, Long> map = Maps.newLinkedHashMap();
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for (RedisIncrParam param : params) {
                	pipeline.incr(SafeEncoder.encode(param.getKey()));
                }
                List<Object> ret = pipeline.syncAndReturnAll();
                for (int i = 0; i < params.size(); i++) {
                    if (ret.get(i) != null) {
                    	RedisIncrParam param = params.get(i);
                    	Long value = (Long) ret.get(i);
                    	map.put(param, value);
                    }
                }
                return map;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
       
        return map;
    }
	
	
	
	public Map<RedisAddSetParam, Long> pipSaddObject(List<RedisAddSetParam> params) {
		final Map<RedisAddSetParam, Long> ret = Maps.newHashMap();
		final Map<String, List<RedisAddSetParam>> nodeToKeys = devideKeysAddSet(params);
		final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet().size());
		for (final String key : nodeToKeys.keySet()) {
			executorService.execute(new Runnable() {
				@Override
				public void run() {
					try {
						List<RedisAddSetParam> params = nodeToKeys.get(key);
						Map<RedisAddSetParam, Long> thisret = pipSaddByte(key, params);
						ret.putAll(thisret);
					} finally {
						cdl.countDown();
					}
				}
			 });
		}
		try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
	}
	
	public Map<String, Set<Object>> pipSmembersObject(List<String> params) {
		final Map<String, Set<Object>> ret = new HashMap<String, Set<Object>>();
		final Map<String, List<String>> nodeToKeys = devideKeysSet(params);
		final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet().size());
		for (final String key : nodeToKeys.keySet()) {
			 executorService.execute(new Runnable() {
				@Override
				public void run() {
					try {
						List<String> params = nodeToKeys.get(key);
						Map<String, Set<Object>> thisret = pipSmembersObject(key, params);
						ret.putAll(thisret);
					} finally {
						cdl.countDown();
					}
				}
			 });
		}
		try {
           cdl.await();
       } catch (InterruptedException e) {
           e.printStackTrace();
       }
       return ret;
	}
	
	public Map<RedisSortedSetParam, Set<Object>> pipZrangeByScoreObject(List<RedisSortedSetParam> params) {
		final Map<RedisSortedSetParam, Set<Object>> ret = new HashMap<RedisSortedSetParam, Set<Object>>();
		final Map<String, List<RedisSortedSetParam>> nodeToKeys = devideKeysSortedSet(params);
		final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet().size());
		for (final String key : nodeToKeys.keySet()) {
			 executorService.execute(new Runnable() {
				@Override
				public void run() {
					try {
						List<RedisSortedSetParam> params = nodeToKeys.get(key);
						Map<RedisSortedSetParam, Set<Object>> thisret = pipZrangeByScoreObject(key,
								params);
						ret.putAll(thisret);
					} finally {
						cdl.countDown();
					}
				}
			 });
		}
		try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
	}
    
	public Map<RedisSortedSetParam, Set<String>> pipZrangeByScoreString(List<RedisSortedSetParam> params) {
		final Map<RedisSortedSetParam, Set<String>> ret = new HashMap<RedisSortedSetParam, Set<String>>();
		final Map<String, List<RedisSortedSetParam>> nodeToKeys = devideKeysSortedSet(params);
		final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet().size());
		for (final String key : nodeToKeys.keySet()) {
			 executorService.execute(new Runnable() {
				@Override
				public void run() {
					try {
						List<RedisSortedSetParam> params = nodeToKeys.get(key);
						Map<RedisSortedSetParam, Set<String>> thisret = pipZrangeByScoreString(key,
								params);
						ret.putAll(thisret);
					} finally {
						cdl.countDown();
					}
				}
			 });
		}
		try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
	}
	
    // =================
	public Map<String, Long> pipDelete(List<String> keys){
		 final Map<String, Long> ret = new ConcurrentHashMap<String, Long>();
	        final Map<String, List<String>> nodeToKeys = devideKeys(keys);
	        final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet()
	                .size());
	        for (final String key : nodeToKeys.keySet()) {
	            executorService.execute(new Runnable() {
	                @Override
	                public void run() {
	                    try {
	                        List<String> thisKeys = nodeToKeys.get(key);
	                        if (thisKeys != null && thisKeys.size() > 0) {
	                            Map<String, Long> thisret = pipDelete(key,
	                                    getBArrArr(thisKeys));
	                            if (thisret != null) {
	                                ret.putAll(thisret);
	                            }
	                        }
	                    } finally {
	                        cdl.countDown();
	                    }
	                }
	            });
	        }
	        try {
	            cdl.await();
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	        return ret;
	}
	
	private Map<String, Long> pipDelete(String key, byte[]... keys) {
        Map<String, Long> map = new HashMap<String, Long>();
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for (byte[] subKey : keys) {
                    pipeline.del(subKey);
                }
                List<Object> ret = pipeline.syncAndReturnAll();
                for (int i = ret.size() - 1; i >= 0; i--) {
                    if (ret.get(i) != null)
                        map.put(SafeEncoder.encode(keys[i]),
                                (Long) ret.get(i));
                }
                return map;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return map;
    }
	
	 public Map<String, Long> pipExpire(List<String> keys, final int timeout) {
		 final Map<String, Long> ret = new ConcurrentHashMap<String, Long>();
	     final Map<String, List<String>> nodeToKeys = devideKeys(keys);
	     final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet().size());
	     for (final String key : nodeToKeys.keySet()) {
	            executorService.execute(new Runnable() {
	                @Override
	                public void run() {
	                    try {
	                        List<String> thisKeys = nodeToKeys.get(key);
	                        if (thisKeys != null && thisKeys.size() > 0) {
	                            Map<String, Long> thisret = pipExpire(key,
	                            		thisKeys, timeout);
	                            if (thisret != null) {
	                                ret.putAll(thisret);
	                            }
	                        }
	                    } finally {
	                        cdl.countDown();
	                    }
	                }
	            });
	        }
	        try {
	            cdl.await();
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	        return ret;
	 }
	 
	 private Map<String, Long> pipExpire(String key, List<String> keys, int timeout) {
	        Map<String, Long> map = new HashMap<String, Long>();
	        ShardedJedis shardJedis = null;
	        Jedis jedis = null;
	        try {
	            shardJedis = shardedJedisPool.getResource();
	            jedis = shardJedis.getShard(key);
	            if (jedis != null) {
	                Pipeline pipeline = jedis.pipelined();
	                for (String subKey : keys) {
	                    pipeline.expire(SafeEncoder.encode(subKey), timeout);
	                }
	                List<Object> ret = pipeline.syncAndReturnAll();
	                for (int i = 0; i < keys.size(); i++) {
	                    if (ret.get(i) != null)
	                        map.put(keys.get(i), (Long) ret.get(i));
	                }
	                return map;
	            }
	        } catch (Exception e) {
	            onException(shardJedis, jedis, e);
	            shardJedis = null;
	        } finally {
	            onFinally(shardJedis);
	        }
	        return map;
	    }
	
    public Map<String, Boolean> pipExists(List<String> keys) {
        final Map<String, Boolean> ret = new ConcurrentHashMap<String, Boolean>();
        final Map<String, List<String>> nodeToKeys = devideKeys(keys);
        final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet()
                .size());

        for (final String key : nodeToKeys.keySet()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<String> thisKeys = nodeToKeys.get(key);
                        if (thisKeys != null && thisKeys.size() > 0) {
                            Map<String, Boolean> thisret = pipExists(key,
                                    getBArrArr(thisKeys));
                            if (thisret != null) {
                                ret.putAll(thisret);
                            }
                        }
                    } finally {
                        cdl.countDown();
                    }
                }
            });

        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return ret;
    }

    private Map<String, Boolean> pipExists(String key, byte[]... keys) {
        Map<String, Boolean> map = new HashMap<String, Boolean>();
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for (byte[] subKey : keys) {
                    pipeline.exists(subKey);
                }
                List<Object> ret = pipeline.syncAndReturnAll();
                for (int i = ret.size() - 1; i >= 0; i--) {
                    if (ret.get(i) != null)
                        map.put(SafeEncoder.encode(keys[i]),
                                (Boolean) ret.get(i));
                }
                return map;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return map;
    }

    private Map<String, List<RedisData.RedisKeyMemByte>> devideKeysRequest(
            List<RedisData.RedisKeyMemByte> requests) {
        Map<JedisShardInfo, List<RedisData.RedisKeyMemByte>> map = new HashMap<JedisShardInfo, List<RedisData.RedisKeyMemByte>>();
        Map<String, List<RedisData.RedisKeyMemByte>> result = new HashMap<String, List<RedisData.RedisKeyMemByte>>();
        ShardedJedis shardJedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            for (RedisData.RedisKeyMemByte redisKeyMemByte : requests) {
                JedisShardInfo jedisShardInfo = shardJedis
                        .getShardInfo(redisKeyMemByte.getKey());
                List<RedisData.RedisKeyMemByte> keysList = map.get(jedisShardInfo);

                if (keysList == null) {
                    keysList = new ArrayList<RedisData.RedisKeyMemByte>();
                    map.put(jedisShardInfo, keysList);
                }
                keysList.add(redisKeyMemByte);
            }
        } catch (Exception e) {
            if (shardJedis != null) {
                shardedJedisPool.returnBrokenResource(shardJedis);
                shardJedis = null;
            }
            log.error(e.getMessage(), e);
        } finally {
            onFinally(shardJedis);
        }
        for (List<RedisData.RedisKeyMemByte> list : map.values()) {
            result.put(list.iterator().next().getKey(), list);
        }
        return result;
    }

    private Map<String, List<RedisSortData.RedisKeySortMemByte>> devideSortKeysRequest(
            List<RedisSortData.RedisKeySortMemByte> requests) {
        Map<JedisShardInfo, List<RedisSortData.RedisKeySortMemByte>> map = new HashMap<JedisShardInfo, List<RedisSortData.RedisKeySortMemByte>>();
        Map<String, List<RedisSortData.RedisKeySortMemByte>> result = new HashMap<String, List<RedisSortData.RedisKeySortMemByte>>();
        ShardedJedis shardJedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            for (RedisSortData.RedisKeySortMemByte redisKeySortMemByte : requests) {
                JedisShardInfo jedisShardInfo = shardJedis
                        .getShardInfo(redisKeySortMemByte.getKey());
                List<RedisSortData.RedisKeySortMemByte> keysList = map.get(jedisShardInfo);

                if (keysList == null) {
                    keysList = new ArrayList<RedisSortData.RedisKeySortMemByte>();
                    map.put(jedisShardInfo, keysList);
                }
                keysList.add(redisKeySortMemByte);
            }
        } catch (Exception e) {
            if (shardJedis != null) {
                shardedJedisPool.returnBrokenResource(shardJedis);
                shardJedis = null;
            }
            log.error(e.getMessage(), e);
        } finally {
            onFinally(shardJedis);
        }
        for (List<RedisSortData.RedisKeySortMemByte> list : map.values()) {
            result.put(list.iterator().next().getKey(), list);
        }
        return result;
    }

    private Map<String, List<RedisHashData.RedisKeyHashMemByte>> devideHashKeysRequest(
            List<RedisHashData.RedisKeyHashMemByte> requests) {
        Map<JedisShardInfo, List<RedisHashData.RedisKeyHashMemByte>> map = new HashMap<JedisShardInfo, List<RedisHashData.RedisKeyHashMemByte>>();
        Map<String, List<RedisHashData.RedisKeyHashMemByte>> result = new HashMap<String, List<RedisHashData.RedisKeyHashMemByte>>();
        ShardedJedis shardJedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            for (RedisHashData.RedisKeyHashMemByte redisKeySortMemByte : requests) {
                JedisShardInfo jedisShardInfo = shardJedis
                        .getShardInfo(redisKeySortMemByte.getKey());
                List<RedisHashData.RedisKeyHashMemByte> keysList = map.get(jedisShardInfo);

                if (keysList == null) {
                    keysList = new ArrayList<RedisHashData.RedisKeyHashMemByte>();
                    map.put(jedisShardInfo, keysList);
                }
                keysList.add(redisKeySortMemByte);
            }
        } catch (Exception e) {
            if (shardJedis != null) {
                shardedJedisPool.returnBrokenResource(shardJedis);
                shardJedis = null;
            }
            log.error(e.getMessage(), e);
        } finally {
            onFinally(shardJedis);
        }
        for (List<RedisHashData.RedisKeyHashMemByte> list : map.values()) {
            result.put(list.iterator().next().getKey(), list);
        }
        return result;
    }

    // ============pipZscoreByteArr=====

    public Map<RedisData.RedisKeyMemObj, Double> pipZscoreObject(
            List<RedisData.RedisKeyMemObj> requests) {
        Map<RedisData.RedisKeyMemByte, Double> result = pipZscoreByteArr(RedisData
                .getRedisKeyMemByteListFromObj(requests));
        Map<RedisData.RedisKeyMemObj, Double> ret = new HashMap<RedisData.RedisKeyMemObj, Double>();
        for (Map.Entry<RedisData.RedisKeyMemByte,Double> entry : result.entrySet()) {
            ret.put(RedisData.getRedisKeyMemObj(entry.getKey()),entry.getValue());
        }
        return ret;
    }

    public Map<RedisData.RedisKeyMemStr, Double> pipZscoreString(
            List<RedisData.RedisKeyMemStr> requests) {
        Map<RedisData.RedisKeyMemByte, Double> result = pipZscoreByteArr(RedisData
                .getRedisKeyMemByteList(requests));
        Map<RedisData.RedisKeyMemStr, Double> ret = new HashMap<RedisData.RedisKeyMemStr, Double>();
        for (Map.Entry<RedisData.RedisKeyMemByte,Double> entry : result.entrySet()) {
            ret.put(RedisData.getRedisKeyMemStr(entry.getKey()), entry.getValue());
        }
        return ret;
    }

    public Map<RedisData.RedisKeyMemByte, Double> pipZscoreByteArr(
            List<RedisData.RedisKeyMemByte> requests) {
        final Map<RedisData.RedisKeyMemByte, Double> ret = new ConcurrentHashMap<RedisData.RedisKeyMemByte, Double>();
        final Map<String, List<RedisData.RedisKeyMemByte>> nodeToKeys = devideKeysRequest(requests);
        final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet()
                .size());

        for (final String key : nodeToKeys.keySet()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<RedisData.RedisKeyMemByte> subRequest = nodeToKeys.get(key);
                        if (subRequest != null && subRequest.size() > 0) {
                            Map<RedisData.RedisKeyMemByte, Double> thisret = pipZscoreByteArr(
                                    key,
                                    RedisData
                                            .getRedisKeyByteMemByteList(subRequest));
                            if (thisret != null) {
                                ret.putAll(thisret);
                            }
                        }
                    } finally {
                        cdl.countDown();
                    }
                }
            });
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return ret;
    }

    private Map<RedisData.RedisKeyMemByte, Double> pipZscoreByteArr(String key,
                                                          List<RedisData.RedisKeyByteMemByte> requests) {
        Map<RedisData.RedisKeyMemByte, Double> map = new HashMap<RedisData.RedisKeyMemByte, Double>();
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for (int i = 0; i < requests.size(); i++) {
                    pipeline.zscore(requests.get(i).getKey(), requests.get(i)
                            .getMem());
                }
                List<Object> ret = pipeline.syncAndReturnAll();
                for (int i = ret.size() - 1; i >= 0; i--) {
                    if (ret.get(i) != null)
                        map.put(RedisData.getRedisKeyMemByte(requests.get(i)),
                                (Double) ret.get(i));
                }
                return map;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return map;
    }

    
    // =====hDelete

    public void pipHDeleteObject(List<RedisData.RedisKeyMemObj> requests) {
        pipHDeleteByte(RedisData.getRedisKeyMemByteListFromObj(requests));
    }

    public void pipHDeleteObjectAsyn(List<RedisData.RedisKeyMemObj> requests) {
        pipHDeleteByteAsyn(RedisData.getRedisKeyMemByteListFromObj(requests));
    }

    public void pipHDeleteStr(List<RedisData.RedisKeyMemStr> requests) {
        pipHDeleteByte(RedisData.getRedisKeyMemByteList(requests));
    }

    public void pipHDeleteStrAsyn(List<RedisData.RedisKeyMemStr> requests) {
        pipHDeleteByteAsyn(RedisData.getRedisKeyMemByteList(requests));
    }

    public void pipHDeleteByteAsyn(List<RedisData.RedisKeyMemByte> requests) {
        final Map<String, List<RedisData.RedisKeyMemByte>> nodeToKeys = devideKeysRequest(requests);
        for (final String key : nodeToKeys.keySet()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    List<RedisData.RedisKeyMemByte> subRequest = nodeToKeys.get(key);
                    if (subRequest != null && subRequest.size() > 0) {
                        pipHDelete(key, RedisData
                                .getRedisKeyByteMemByteList(subRequest));
                    }
                }
            });
        }

    }

    public void pipHDeleteByte(List<RedisData.RedisKeyMemByte> requests) {
        final Map<String, List<RedisData.RedisKeyMemByte>> nodeToKeys = devideKeysRequest(requests);
        final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet()
                .size());

        for (final String key : nodeToKeys.keySet()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<RedisData.RedisKeyMemByte> subRequest = nodeToKeys.get(key);
                        if (subRequest != null && subRequest.size() > 0) {
                            pipHDelete(key, RedisData
                                    .getRedisKeyByteMemByteList(subRequest));
                        }
                    } finally {
                        cdl.countDown();
                    }
                }
            });
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void pipHDelete(String key, List<RedisData.RedisKeyByteMemByte> requests) {

        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for (int i = 0; i < requests.size(); i++) {
                    pipeline.hdel(requests.get(i).getKey(), requests.get(i)
                            .getMem());
                }
                pipeline.syncAndReturnAll();
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
    }

    // =====hGet

    public Map<RedisData.RedisKeyMemByte, byte[]> pipHGetByte(
            List<RedisData.RedisKeyMemByte> requests) {
        final Map<RedisData.RedisKeyMemByte, byte[]> ret = new ConcurrentHashMap<RedisData.RedisKeyMemByte, byte[]>();
        final Map<String, List<RedisData.RedisKeyMemByte>> nodeToKeys = devideKeysRequest(requests);
        final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet()
                .size());

        for (final String key : nodeToKeys.keySet()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<RedisData.RedisKeyMemByte> subRequest = nodeToKeys.get(key);
                        if (subRequest != null && subRequest.size() > 0) {
                            Map<RedisData.RedisKeyMemByte, byte[]> thisret = pipHGet(
                                    key,
                                    RedisData
                                            .getRedisKeyByteMemByteList(subRequest));
                            if (thisret != null) {
                                ret.putAll(thisret);
                            }
                        }
                    } finally {
                        cdl.countDown();
                    }
                }
            });
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private Map<RedisData.RedisKeyMemByte, byte[]> pipHGet(String key,
                                                 List<RedisData.RedisKeyByteMemByte> requests) {
        Map<RedisData.RedisKeyMemByte, byte[]> map = new HashMap<RedisData.RedisKeyMemByte, byte[]>();
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {

                Pipeline pipeline = jedis.pipelined();
                Class clazz = pipeline.getClass();
                Field field = ReflectUtil.getFieldByName("client", clazz);
                field.setAccessible(true);
                Client client = (Client) field.get(pipeline);
                Method me = ReflectUtil.getMethodByName("getResponse",
                        Builder.class, clazz);
                me.setAccessible(true);
                for (int i = 0; i < requests.size(); i++) {
                    client.hget(requests.get(i).getKey(), requests.get(i)
                            .getMem());
                    me.invoke(pipeline, BuilderFactory.BYTE_ARRAY);
                }
                List<Object> ret = pipeline.syncAndReturnAll();

                for (int i = ret.size() - 1; i >= 0; i--) {
                    if (ret.get(i) != null)
                        map.put(RedisData.getRedisKeyMemByte(requests.get(i)),
                                (byte[]) ret.get(i));
                }
                return map;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return map;
    }

    // =====hSet

    public void pipHSetObject(List<RedisHashData.RedisKeyHashMemObj> requests) {
        pipHSetByte(RedisHashData.getRedisKeyHashMemByteListFromObj(requests));
    }

    public void pipHSetObjectAsyn(List<RedisHashData.RedisKeyHashMemObj> requests) {
        pipHSetByteAsyn(RedisHashData
                .getRedisKeyHashMemByteListFromObj(requests));
    }

    public void pipHSetStr(List<RedisHashData.RedisKeyHashMemStr> requests) {
        pipHSetByte(RedisHashData.getRedisKeyHashMemByteList(requests));
    }

    public void pipHSetStrAsyn(List<RedisHashData.RedisKeyHashMemStr> requests) {
        pipHSetByteAsyn(RedisHashData.getRedisKeyHashMemByteList(requests));
    }

    public void pipHSetByteAsyn(List<RedisHashData.RedisKeyHashMemByte> requests) {
        final Map<String, List<RedisHashData.RedisKeyHashMemByte>> nodeToKeys = devideHashKeysRequest(requests);
        for (final String key : nodeToKeys.keySet()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    List<RedisHashData.RedisKeyHashMemByte> subRequest = nodeToKeys.get(key);
                    if (subRequest != null && subRequest.size() > 0) {
                        pipHSet(key, RedisHashData
                                .getRedisKeyHashByteMemByteList(subRequest));
                    }
                }
            });
        }

    }

    public void pipHSetByte(List<RedisHashData.RedisKeyHashMemByte> requests) {
        final Map<String, List<RedisHashData.RedisKeyHashMemByte>> nodeToKeys = devideHashKeysRequest(requests);
        final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet()
                .size());

        for (final String key : nodeToKeys.keySet()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<RedisHashData.RedisKeyHashMemByte> subRequest = nodeToKeys
                                .get(key);
                        if (subRequest != null && subRequest.size() > 0) {
                            pipHSet(key, RedisHashData
                                    .getRedisKeyHashByteMemByteList(subRequest));
                        }
                    } finally {
                        cdl.countDown();
                    }
                }
            });
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void pipHSet(String key, List<RedisHashData.RedisKeyHashByteMemByte> requests) {

        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for (int i = 0; i < requests.size(); i++) {
                    pipeline.hset(requests.get(i).getKey(), requests.get(i)
                            .getMem(), requests.get(i).getResult());
                }
                pipeline.syncAndReturnAll();
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
    }

    // ===============ZremByteArr

    public void pipZremObject(List<RedisData.RedisKeyMemObj> requests) {
        pipZremByte(RedisData.getRedisKeyMemByteListFromObj(requests));
    }

    public void pipZremObjectAsyn(List<RedisData.RedisKeyMemObj> requests) {
        pipZremByteAsyn(RedisData.getRedisKeyMemByteListFromObj(requests));
    }

    public void pipZremStr(List<RedisData.RedisKeyMemStr> requests) {
        pipZremByte(RedisData.getRedisKeyMemByteList(requests));
    }

    public void pipZremStrAsyn(List<RedisData.RedisKeyMemStr> requests) {
        pipZremByteAsyn(RedisData.getRedisKeyMemByteList(requests));
    }

    public void pipZremByteAsyn(List<RedisData.RedisKeyMemByte> requests) {
        final Map<String, List<RedisData.RedisKeyMemByte>> nodeToKeys = devideKeysRequest(requests);
        for (final String key : nodeToKeys.keySet()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    List<RedisData.RedisKeyMemByte> subRequest = nodeToKeys.get(key);
                    if (subRequest != null && subRequest.size() > 0) {
                        pipZrem(key, RedisData
                                .getRedisKeyByteMemByteList(subRequest));
                    }
                }
            });
        }

    }

    public void pipZremByte(List<RedisData.RedisKeyMemByte> requests) {
        final Map<String, List<RedisData.RedisKeyMemByte>> nodeToKeys = devideKeysRequest(requests);
        final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet()
                .size());

        for (final String key : nodeToKeys.keySet()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<RedisData.RedisKeyMemByte> subRequest = nodeToKeys.get(key);
                        if (subRequest != null && subRequest.size() > 0) {
                            pipZrem(key, RedisData
                                    .getRedisKeyByteMemByteList(subRequest));
                        }
                    } finally {
                        cdl.countDown();
                    }
                }
            });
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void pipZrem(String key, List<RedisData.RedisKeyByteMemByte> requests) {

        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for (int i = 0; i < requests.size(); i++) {
                    pipeline.zrem(requests.get(i).getKey(), requests.get(i)
                            .getMem());
                }
                pipeline.syncAndReturnAll();
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
    }

    // zaddByteArr

    public void pipZaddObject(List<RedisSortData.RedisKeySortMemObj> requests) {
        pipZaddByte(RedisSortData.getRedisKeySortMemByteListFromObj(requests));
    }

    public void pipZaddObjectAsyn(List<RedisSortData.RedisKeySortMemObj> requests) {
        pipZaddByteAsyn(RedisSortData
                .getRedisKeySortMemByteListFromObj(requests));
    }

    public void pipZaddStr(List<RedisSortData.RedisKeySortMemStr> requests) {
        pipZaddByte(RedisSortData.getRedisKeySortMemByteList(requests));
    }

    public void pipZaddStrAsyn(List<RedisSortData.RedisKeySortMemStr> requests) {
        pipZaddByteAsyn(RedisSortData.getRedisKeySortMemByteList(requests));
    }

    public void pipZaddByteAsyn(List<RedisSortData.RedisKeySortMemByte> requests) {
        final Map<String, List<RedisSortData.RedisKeySortMemByte>> nodeToKeys = devideSortKeysRequest(requests);
        for (final String key : nodeToKeys.keySet()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    List<RedisSortData.RedisKeySortMemByte> subRequest = nodeToKeys.get(key);
                    if (subRequest != null && subRequest.size() > 0) {
                        pipZadd(key, RedisSortData
                                .getRedisKeyByteMemByteList(subRequest));
                    }
                }
            });
        }

    }

    public void pipZaddByte(List<RedisSortData.RedisKeySortMemByte> requests) {
        final Map<String, List<RedisSortData.RedisKeySortMemByte>> nodeToKeys = devideSortKeysRequest(requests);
        final CountDownLatch cdl = new CountDownLatch(nodeToKeys.keySet()
                .size());

        for (final String key : nodeToKeys.keySet()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<RedisSortData.RedisKeySortMemByte> subRequest = nodeToKeys
                                .get(key);
                        if (subRequest != null && subRequest.size() > 0) {
                            pipZadd(key, RedisSortData
                                    .getRedisKeyByteMemByteList(subRequest));
                        }
                    } finally {
                        cdl.countDown();
                    }
                }
            });
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void pipZadd(String key, List<RedisSortData.RedisKeySortByteMemByte> requests) {

        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {

            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);

            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for (int i = 0; i < requests.size(); i++) {
                    pipeline.zadd(requests.get(i).getKey(), requests.get(i)
                            .getSort(), requests.get(i).getMem());
                }
                pipeline.syncAndReturnAll();
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
    }

	private Long geoAdd(String key, final byte[] bytekey,
			byte[] value, double lat, double lon) {
		ShardedJedis shardJedis = null;
		Jedis jedis = null;
		try {
			shardJedis = shardedJedisPool.getResource();
			jedis = shardJedis.getShard(key);
			if (jedis != null) {
				Long ret = jedis.geoadd(bytekey, lon, lat, value);
				return ret;
			}
		} catch (Exception e) {
			onException(shardJedis, jedis, e);
			shardJedis = null;
		} finally {
			onFinally(shardJedis);
		}
		return 0L;
	}
	
	public Long geoAddByteArr(final String key, byte[] value, double lat, double lon) {
        valueTypeAssert(value);
        return geoAdd(key, SafeEncoder.encode(key), value, lat, lon);
    }
	
//	public Long geoAddObject(final String key, Object value, double lat, double lon) {
//        long start = System.currentTimeMillis();
//        try {
//            return geoAddByteArr(key, serialize(value), lat, lon);
//        } finally {
//            log.info("geoAddObj key:{},time:{}", key, System.currentTimeMillis() - start);
//        }
//    }
	
	public Long geoAddString(final String key, String value, double lat, double lon) {
        long start = System.currentTimeMillis();
        try {
            return geoAddByteArr(key, SafeEncoder.encode(value), lat, lon);
        } finally {
            log.info("geoAddString key:{},time:{}", key, System.currentTimeMillis() - start);
        }
    }
	
	private Long multiGeoAdd(String key, final byte[] bytekey,
			Map<byte[], GeoCoordinate> map) {
		ShardedJedis shardJedis = null;
		Jedis jedis = null;
		try {
			shardJedis = shardedJedisPool.getResource();
			jedis = shardJedis.getShard(key);
			if (jedis != null) {
				Long ret = jedis.geoadd(bytekey, map);
				return ret;
			}
		} catch (Exception e) {
			onException(shardJedis, jedis, e);
			shardJedis = null;
		} finally {
			onFinally(shardJedis);
		}
		return 0L;
	}
	
	private Long multiGeoAddByteArr(String key, Map<byte[], GeoCoordinate> map) {
		return multiGeoAdd(key, SafeEncoder.encode(key), map);
	}
	
//	public Long multiGeoAddObject(final String key, Map<Object, GeoCoordinate> map) {
//		 long start = System.currentTimeMillis();
//	        try {
//	            Map<byte[], GeoCoordinate> keyValuebMap = Maps.newHashMap();
//	            for (Map.Entry<Object, GeoCoordinate> entry : map.entrySet()) {
//	                Object obj = entry.getKey();
//	                if (obj != null) {
//	                    keyValuebMap.put(serialize(obj), entry.getValue());
//	                }
//	            }
//	            return multiGeoAddByteArr(key, keyValuebMap);
//	        } finally {
//	            log.info("multiGeoAddObj key:{},time:{}", map.size(), System.currentTimeMillis() - start);
//	        }
//    }
	
	public Long multiGeoAddString(final String key, Map<String, GeoCoordinate> map) {
		 long start = System.currentTimeMillis();
	        try {
	            Map<byte[], GeoCoordinate> keyValuebMap = Maps.newHashMap();
	            for (Map.Entry<String, GeoCoordinate> entry : map.entrySet()) {
	                String str = entry.getKey();
	                if (str != null) {
	                    keyValuebMap.put(SafeEncoder.encode(str), entry.getValue());
	                }
	            }
	            return multiGeoAddByteArr(key, keyValuebMap);
	        } finally {
	            log.info("multiGeoAddString key:{},time:{}", map.size(), System.currentTimeMillis() - start);
	        }
   }
	
	private List<GeoRadiusResponse> geoRadius(String key, final byte[] bytekey, double lat, double lon,
			double radius, GeoUnit unit, GeoRadiusParam param) {
		ShardedJedis shardJedis = null;
		Jedis jedis = null;
		try {
			shardJedis = shardedJedisPool.getResource();
			jedis = shardJedis.getShard(key);
			if (jedis != null) {
				List<GeoRadiusResponse> ret = jedis.georadius(bytekey, lon, lat, radius, unit, param);
				return ret;
			}
		} catch (Exception e) {
			onException(shardJedis, jedis, e);
			shardJedis = null;
		} finally {
			onFinally(shardJedis);
		}
		return Lists.newArrayList();
	}
	
	
	
//	public List<GeoRadiusObject> geoRadiusObject(String key, double lat, double lon,
//			double radius, GeoUnit unit, GeoRadiusParam param) {
//		List<GeoRadiusResponse> list = geoRadius(key, SafeEncoder.encode(key), lat, lon, radius, unit, param);
//		List<GeoRadiusObject> res = Lists.newArrayList();
//		for(GeoRadiusResponse response : list) {
//			GeoRadiusObject obj = new GeoRadiusObject();
//			byte[] data = response.getMember();
//			Object member = deserialize(data, null);
//			obj.setMember(member);
//			obj.setDistance(response.getDistance());
//			obj.setCoordinate(response.getCoordinate());
//			res.add(obj);
//		}
//		return res;
//	}
	
	public List<GeoRadiusString> geoRadiusString(String key, double lat, double lon,
			double radius) {
		List<GeoRadiusResponse> list = geoRadius(key, SafeEncoder.encode(key), lat, lon, 
				radius, GeoUnit.M, GeoRadiusParam.geoRadiusParam().withDist().sortAscending());
		List<GeoRadiusString> res = Lists.newArrayList();
		for(GeoRadiusResponse response : list) {
			byte[] data = response.getMember();
			String member = SafeEncoder.encode(data);
			if(!member.startsWith("tmp")) {
				GeoRadiusString item = new GeoRadiusString();
				item.setMember(member);
				item.setDistance(response.getDistance());
				item.setCoordinate(response.getCoordinate());
				res.add(item);
			}
		}
		return res;
	}
	
	public List<GeoNearString> geoNearString(String key, double lat, double lon, 
			double score, int count) {
		ShardedJedis shardJedis = null;
		Jedis jedis = null;
		try {
			shardJedis = shardedJedisPool.getResource();
			jedis = shardJedis.getShard(key);
			if (jedis != null) {
				byte[] bytekey = SafeEncoder.encode(key);
				String tmp = "tmp_"+UUID.randomUUID().toString();
				byte[] tmpMember = SafeEncoder.encode(tmp);
				jedis.geoadd(bytekey, lon, lat, tmpMember);
				if(score == 0) {
					score = jedis.zscore(bytekey, tmpMember);
				}
				Set<Tuple> set = jedis.zrangeByScoreWithScores(bytekey, score, Double.MAX_VALUE, 1, count);
				List<GeoNearString> res = Lists.newArrayList();
				for(Tuple tuple : set) {
					byte[] one = tuple.getBinaryElement();
					double dist = jedis.geodist(bytekey, tmpMember, one, GeoUnit.M);
					GeoNearString item = new GeoNearString();
					item.setMember(SafeEncoder.encode(one));
					item.setDistance(dist);
					item.setScore(tuple.getScore());
					res.add(item);
				}
				jedis.zrem(bytekey, tmpMember);
				return res;
			}
		} catch (Exception e) {
			onException(shardJedis, jedis, e);
			shardJedis = null;
		} finally {
			onFinally(shardJedis);
		}
		return Lists.newArrayList();
	}


    public void pipSetBit(String key, List<int[]> bitIndexesList, boolean value) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for(int[] bitIndexes : bitIndexesList) {
                    for (int bitIndex : bitIndexes) {
                        pipeline.setbit(key, bitIndex, true);
                    }
                }
                pipeline.sync();
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
    }

	public void pipSetBit(String key, int[] bitIndexes, boolean value) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for (int bitIndex : bitIndexes) {
                    pipeline.setbit(key, bitIndex, true);
                }
                pipeline.sync();
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
    }

    public boolean setBit(String key, int bitIndex, boolean value) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                boolean ret = jedis.setbit(key, bitIndex, value);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return false;
    }

    public List<List<Boolean>> pipGetBit(String key, List<int[]> bitIndexesList) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                int num = 0;
                for(int[] bitIndexes : bitIndexesList) {
                    for (int bitIndex : bitIndexes) {
                        pipeline.getbit(key, bitIndex);
                        num++;
                    }
                }
                List<Object> list = pipeline.syncAndReturnAll();
                if(num != list.size()) return null;
                for(Object obj : list) {
                    if(obj == null) return null;
                }
                List<List<Boolean>> res = Lists.newArrayList();
                for(int i = 0; i < bitIndexesList.size(); i++) {
                    int startIndex = 0;
                    if(i > 0) {
                        for(int j = 0; j < i; j++) {
                            startIndex += bitIndexesList.get(j).length;
                        }
                    }
                    List<Boolean> oneRes = Lists.newArrayList();
                    for(int j = startIndex; j < startIndex + bitIndexesList.get(i).length; j++) {
                        oneRes.add((boolean)list.get(j));
                    }
                    res.add(oneRes);
                }
                return res;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return null;
    }

    public List<Boolean> pipGetBit(String key, int[] bitIndexes) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                Pipeline pipeline = jedis.pipelined();
                for (int bitIndex : bitIndexes) {
                    pipeline.getbit(key, bitIndex);
                }
                List<Object> list = pipeline.syncAndReturnAll();
                List<Boolean> res = Lists.newArrayList();
                for(Object obj : list) {
                    res.add((Boolean) obj);
                }
                return res;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return Lists.newArrayList();
    }

    public boolean getBit(String key, int bitIndex) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                boolean ret = jedis.getbit(key, bitIndex);
                return ret;
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            onFinally(shardJedis);
        }
        return false;
    }


    public List<String> lrangeAndDelString(String key, int start, int end) {
        List<byte[]> ret = lrangeAndDelByteArr(key, start, end);
        if (ret != null) {
            List<String> trueRet = new ArrayList<String>();
            for (byte[] item : ret) {
                if (item != null) {
                    trueRet.add(SafeEncoder.encode(item));
                }
            }
            return trueRet;
        }
        return null;
    }

    public List<Object> lrangeAndDelObject(String key, int start, int end, final Class<? extends ScloudSerializable> clazz) {
        List<byte[]> ret = lrangeAndDelByteArr(key, start, end);
        if (ret != null) {
            List<Object> trueRet = new ArrayList<Object>();
            for (byte[] item : ret) {
                if (item != null) {
                    trueRet.add(deserialize(item, clazz));
                }
            }
            return trueRet;
        }
        return null;
    }

    public List<byte[]> lrangeAndDelByteArr(String key, int start, int end) {
        return lrangeAndDel(key, SafeEncoder.encode(key), start, end);

    }

    private List<byte[]> lrangeAndDel(String key, byte[] bytekey, int start, int end) {
        ShardedJedis shardJedis = null;
        Jedis jedis = null;
        Transaction ts = null;
        try {
            shardJedis = shardedJedisPool.getResource();
            jedis = shardJedis.getShard(key);
            if (jedis != null) {
                ts = jedis.multi();
                ts.lrange(bytekey, start, end);
                if(end == Integer.MAX_VALUE) ts.ltrim(bytekey, end, -1);
                else ts.ltrim(bytekey, end + 1, -1);
                List<Object> tsRes = ts.exec();
                if(tsRes != null && !tsRes.isEmpty()){
                    return (List<byte[]>)tsRes.get(0);
                }
            }
        } catch (Exception e) {
            onException(shardJedis, jedis, e);
            shardJedis = null;
        } finally {
            try {
                if(null != ts) ts.close();
            } catch (Exception e) {}
            onFinally(shardJedis);
        }
        return Lists.newArrayList();
    }
	
	
    public JedisPoolConfig getJedisPoolConfig() {
        return jedisPoolConfig;
    }

    public ShardedJedisPool getShardedJedisPool() {
        return shardedJedisPool;
    }


}
