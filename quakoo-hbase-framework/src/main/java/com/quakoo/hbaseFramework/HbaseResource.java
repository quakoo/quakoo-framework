package com.quakoo.hbaseFramework;

import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Table;

/**
 * Created by 136249 on 2015/3/18.
 */
public class HbaseResource {
    Connection connection;
    Table table;

    public HbaseResource() {
    }

    public HbaseResource(Connection connection, Table table) {
        this.connection = connection;
        this.table = table;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }
}
