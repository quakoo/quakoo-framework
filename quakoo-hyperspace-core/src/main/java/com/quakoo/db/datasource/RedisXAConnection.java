package com.quakoo.db.datasource;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import redis.clients.jedis.Jedis;

public class RedisXAConnection implements XAConnection, XAResource {
    private final Jedis jedis;

    protected boolean logXaCommands;

    public RedisXAConnection(Jedis jedis, boolean logXaCommands) {
        this.jedis = jedis;
        this.logXaCommands = logXaCommands;
    }

    @Override
    public Connection getConnection() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addStatementEventListener(StatementEventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeStatementEventListener(StatementEventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public XAResource getXAResource() throws SQLException {
        return this;
    }

    @Override
    public void commit(Xid xid, boolean flag) throws XAException {
        // TODO Auto-generated method stub

    }

    @Override
    public void end(Xid xid, int i) throws XAException {
        // TODO Auto-generated method stub

    }

    @Override
    public void forget(Xid xid) throws XAException {
        // TODO Auto-generated method stub

    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        return false;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean setTransactionTimeout(int i) throws XAException {
        return false;
    }

    @Override
    public void start(Xid xid, int i) throws XAException {
        // TODO Auto-generated method stub

    }

    @Override
    public Xid[] recover(int arg0) throws XAException {
        // TODO Auto-generated method stub
        return null;
    }

}
