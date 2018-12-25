package com.quakoo.hbaseFramework;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import java.util.List;

public class ShardedHbasePool extends Pool<HbaseResource> {

    public ShardedHbasePool(final GenericObjectPool.Config poolConfig,String tableName,List<String> localResources) {
        super(poolConfig, new ShardedHbaseFactory(tableName,localResources));
    }

    /**
     * PoolableObjectFactory custom impl.
     */
    private static class ShardedHbaseFactory extends BasePoolableObjectFactory {

        private  String tableName;

        private  Configuration conf;
        public ShardedHbaseFactory(String tableName,List<String> localResources){
        	this.tableName=tableName;
            conf = HBaseConfiguration.create();
            if(localResources!=null){
                for (String local:localResources){
                    conf.addResource(local);
                }
            }
        }
        public Object makeObject() throws Exception {
            Connection connection = ConnectionFactory.createConnection(conf);
            return new HbaseResource(connection, connection.getTable(TableName.valueOf(tableName)));
        }

        public void destroyObject(final Object obj) throws Exception {
            if ((obj != null) && (obj instanceof HbaseResource)) {
                HbaseResource hbaseResource = (HbaseResource) obj;
                hbaseResource.getTable().close();
                hbaseResource.getConnection().close();
            }
        }

        public boolean validateObject(final Object obj) {
            try {
                HbaseResource hbaseResource = (HbaseResource) obj;
                return !hbaseResource.getConnection().isClosed();
            } catch (Exception ex) {
                return false;
            }
        }
    }
}