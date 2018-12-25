package com.quakoo.db.datasource;

import java.io.PrintWriter;
import java.sql.SQLException;

import javax.sql.XAConnection;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

public class RedisXADataSource {//implements XADataSource {

    protected JedisPool pool;

    protected int loginTimeout;

    protected PrintWriter logWriter;

    public XAConnection getXAConnection() throws SQLException {
        Jedis jedis = pool.getResource();
        Transaction t = jedis.multi();

        //RedisXAConnection cacheXAConnection = new RedisXAConnection(jedis, false);
       // return cacheXAConnection;
        return null;
    }

    public XAConnection getXAConnection(String user, String password) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public PrintWriter getLogWriter() throws SQLException {
        return this.logWriter;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logWriter = out;
    }

    public int getLoginTimeout() throws SQLException {
        return this.loginTimeout;
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        this.loginTimeout = seconds;
    }

	
}
