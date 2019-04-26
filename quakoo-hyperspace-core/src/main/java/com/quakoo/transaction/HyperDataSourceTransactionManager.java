package com.quakoo.transaction;

import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.space.model.transaction.JedisMethodInfo;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;


public class HyperDataSourceTransactionManager extends DataSourceTransactionManager {

    private JedisX jedisXCache;

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        if (getJedisXCache() == null) {
            throw new IllegalArgumentException("Property 'jedisXCache' is required");
        }
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);
        RedisHelp redisHelp = new RedisHelp();
        TransactionSynchronizationManager.bindResource("redis_help", redisHelp);
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        super.doCommit(status);
        try {
            RedisHelp redisHelp = (RedisHelp)TransactionSynchronizationManager.getResource("redis_help");
            List<JedisMethodInfo> methodInfos = redisHelp.getMethodInfos();
            for(JedisMethodInfo methodInfo : methodInfos) {
                MethodUtils.invokeMethod(jedisXCache, methodInfo.getMethodName(), methodInfo.getParams());
            }
        } catch (Exception e) {}
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        super.doCleanupAfterCompletion(transaction);
        TransactionSynchronizationManager.unbindResource("redis_help");
    }

    public JedisX getJedisXCache() {
        return jedisXCache;
    }

    public void setJedisXCache(JedisX jedisXCache) {
        this.jedisXCache = jedisXCache;
    }

}
