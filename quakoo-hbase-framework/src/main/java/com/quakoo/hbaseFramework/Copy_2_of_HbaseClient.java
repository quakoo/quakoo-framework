package com.quakoo.hbaseFramework;

import com.quakoo.baseFramework.serialize.ScloudSerializable;
import com.quakoo.baseFramework.serialize.ScloudSerializeUtil;
import com.quakoo.baseFramework.util.ByteUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by 136249 on 2015/3/14.
 */
public class Copy_2_of_HbaseClient implements Serializable, InitializingBean {
    final static Logger logger = LoggerFactory.getLogger(Copy_2_of_HbaseClient.class);
    static Map<String, ShardedHbasePool> shardedHbasePoolMap = new ConcurrentHashMap<>();
    static final String columnFamily = "f";
    static final String qualifier = "q";

    /**
     * 预热table
     */
    private List<String> initTables;
    /**
     * 本地的hdfs文件
     */
    private List<String> localResources;
    private int maxActive = 60;
    private int maxIdle = 30;
    private int minIdle = 5;

    public List<String> getInitTables() {
        return initTables;
    }

    public void setInitTables(List<String> initTables) {
        this.initTables = initTables;
    }

    public List<String> getLocalResources() {
        return localResources;
    }

    public void setLocalResources(List<String> localResources) {
        this.localResources = localResources;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // init tables
        if (initTables != null) {
            for (String tableName : initTables) {
                getShardedHbasePool(tableName);
            }
            
            for(String tableName : initTables){
            	get(tableName, Bytes.toBytes(1l));
            }
            
        }
    }

    ShardedHbasePool getShardedHbasePool(final String tableName) {
        ShardedHbasePool shardedHbasePool = shardedHbasePoolMap.get(tableName);
        if (shardedHbasePool == null) {
            synchronized (shardedHbasePoolMap) {
                shardedHbasePool = shardedHbasePoolMap.get(tableName);
                if (shardedHbasePool == null) {
                    HbasePoolConfig hbasePoolConfig = new HbasePoolConfig();
                    hbasePoolConfig.setMaxIdle(maxIdle);
                    hbasePoolConfig.setMaxActive(maxActive);
                    hbasePoolConfig.setMinIdle(minIdle);
                    shardedHbasePool = new ShardedHbasePool(hbasePoolConfig, tableName, localResources);
                    List<HbaseResource> list = new ArrayList();
                    for (int i = 0; i < minIdle; i++) {
                        list.add(shardedHbasePool.getResource());
                    }
                    for (int i = 0; i < minIdle; i++) {
                        shardedHbasePool.returnResource(list.get(i));
                    }
                    shardedHbasePoolMap.put(tableName, shardedHbasePool);
                    
                    
                    final ShardedHbasePool copyShardedHbasePool = shardedHbasePool;
                    Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Field targetField = Pool.class.getDeclaredField("internalPool");
                                targetField.setAccessible(true);
                                final GenericObjectPool internalPool = (GenericObjectPool) targetField.get(copyShardedHbasePool);
                                if (internalPool != null) {
                                    logger.info("ShardedHbasePool-----tableName:"+tableName+",maxActive:" + internalPool.getMaxActive() + ",active:" + internalPool.getNumActive() + ",idle:" + internalPool.getNumIdle());
                                }
                            } catch (Exception e) {
                                logger.error("print Hbase pool info error. reason:" + e.getMessage());
                                //e.printStackTrace();
                            }
                        }

                    }, 2, 10, TimeUnit.SECONDS);
                }
            }
        }
        return shardedHbasePool;
    }

    HbaseResource getTable(String tableName) {
        ShardedHbasePool shardedHbasePool = getShardedHbasePool(tableName);
        return shardedHbasePool.getResource();
    }

    void returnTable(HbaseResource hbaseResource, String tableName) {
        ShardedHbasePool shardedHbasePool = getShardedHbasePool(tableName);
        shardedHbasePool.returnResource(hbaseResource);
    }

    void returnBrokenTable(HbaseResource hbaseResource, String tableName) {
        ShardedHbasePool shardedHbasePool = getShardedHbasePool(tableName);
        shardedHbasePool.returnBrokenResource(hbaseResource);
    }


    public void createTable(String tableName, String columnFamily,byte[][] spilts) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        Connection connection = ConnectionFactory.createConnection(conf);
        Admin admin = connection.getAdmin();
        HTableDescriptor tableDesc = new HTableDescriptor(TableName.valueOf(tableName));
        if (StringUtils.isBlank(columnFamily)) {
            tableDesc.addFamily(new HColumnDescriptor(this.columnFamily));
        } else {
            tableDesc.addFamily(new HColumnDescriptor(columnFamily));
        }
        if(spilts==null){
        	admin.createTable(tableDesc);
        }else{
        	admin.createTable(tableDesc,spilts);
        }
        admin.close();
    }

    public void deleteTable(String tableName) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        Connection connection = ConnectionFactory.createConnection(conf);
        Admin admin = connection.getAdmin();
        admin.disableTable(TableName.valueOf(tableName));
        admin.deleteTable(TableName.valueOf(tableName));
    }

    public <T extends ScloudSerializable> void put(String tableName, byte[] rowKey, T value, String logKey) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            byte[] result = ScloudSerializeUtil.encode(value);
            put(tableName, rowKey, result);
        }finally {
            logger.info("hbase put tableName:{},key:{},time:{},", new Object[]{tableName, logKey, stopWatch.getTime()});
        }

    }

    public void put(String tableName, byte[] rowKey, byte[] values) {
        HbaseResource hbaseResource = null;
        try {
            hbaseResource = getTable(tableName);
            Put put = new Put(rowKey);
            put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier), values);
            hbaseResource.getTable().put(put);
        } catch (Exception e) {
            returnBrokenTable(hbaseResource, tableName);
            hbaseResource = null;
            throw new HbaseException("", e);
        } finally {
            if (hbaseResource != null) {
                returnTable(hbaseResource, tableName);
            }
        }
    }

    public <T extends ScloudSerializable> void batchPutObj(String tableName, List<byte[]> rowKey, List<T> values, String logKey) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            List<byte[]> result = new ArrayList<>();
            for (T value : values) {
                result.add(ScloudSerializeUtil.encode(value));
            }
            batchPut(tableName, rowKey, result);
        }finally {
            logger.info("hbase batchPut tableName:{},key:{},time:{},", new Object[]{tableName, logKey, stopWatch.getTime()});
        }

    }

    public void batchPut(String tableName, List<byte[]> rowKey, List<byte[]> values) {
        HbaseResource hbaseResource = null;
        try {
            hbaseResource = getTable(tableName);
            List<Put> puts = new ArrayList<>();
            for (int i = 0; i < rowKey.size(); i++) {
                byte[] byteKey = rowKey.get(i);
                Put put = new Put(byteKey);
                put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier), values.get(i));
                puts.add(put);
            }
            hbaseResource.getTable().put(puts);
        } catch (Exception e) {
            returnBrokenTable(hbaseResource, tableName);
            hbaseResource = null;
            throw new HbaseException("", e);
        } finally {
            if (hbaseResource != null) {
                returnTable(hbaseResource, tableName);
            }
        }
    }

    public <T extends ScloudSerializable> T get(String tableName, byte[] rowKey, Class<T> clazz, String logkey) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            byte[] result = get(tableName, rowKey);
            if (result == null) {
                return null;
            } else {
                return ScloudSerializeUtil.decode(result, clazz);
            }
        } finally {
            logger.info("hbase get tableName:{},key:{},time:{},", new Object[]{tableName, logkey,stopWatch.getTime() });
        }
    }

    public byte[] get(String tableName, byte[] rowKey) {
        HbaseResource hbaseResource = null;
        try {
            hbaseResource = getTable(tableName);
            Get get = new Get(rowKey);
            Result result = hbaseResource.getTable().get(get);
            if (result != null && result.rawCells().length > 0) {
                for (Cell rowKV : result.rawCells()) {
                    return CellUtil.cloneValue(rowKV);
                }
            }
        } catch (Exception e) {
            returnBrokenTable(hbaseResource, tableName);
            hbaseResource = null;
            throw new HbaseException("", e);
        } finally {
            if (hbaseResource != null) {
                returnTable(hbaseResource, tableName);
            }
        }
        return null;
    }


    public <T extends ScloudSerializable> Map<byte[], T> batchGet(String tableName, List<byte[]> rowKey, Class<T> clazz, String logkey) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            Map<byte[], byte[]> result = batchGet(tableName, rowKey);
            if (result == null) {
                return null;
            } else {
                Map<byte[], T> map = new HashMap<>();
                for (Map.Entry<byte[],byte[]> entry : result.entrySet()) {
                    map.put(entry.getKey(), ScloudSerializeUtil.decode(entry.getValue(), clazz));
                }
                return map;
            }
        } finally {
            logger.info("hbase batchGet tableName:{},key:{},time:{},", new Object[]{tableName, logkey, stopWatch.getTime()});
        }
    }


    public Map<byte[], byte[]> batchGet(String tableName, List<byte[]> rowKey) {
        HbaseResource hbaseResource = null;
        Map<byte[], byte[]> result = new HashMap<byte[], byte[]>();
        try {
            hbaseResource = getTable(tableName);
            List<Get> gets = new ArrayList<>();
            for (int i = 0; i < rowKey.size(); i++) {
                byte[] byteKey = rowKey.get(i);
                Get get = new Get(byteKey);
                gets.add(get);
            }
            Result[] results = hbaseResource.getTable().get(gets);
            for (Result cellResult : results) {
                if (result != null && cellResult.rawCells().length > 0) {
                    for (Cell rowKV : cellResult.rawCells()) {
                        result.put(CellUtil.cloneRow(rowKV), CellUtil.cloneValue(rowKV));
                    }
                }
            }
        } catch (Exception e) {
            returnBrokenTable(hbaseResource, tableName);
            hbaseResource = null;
            throw new HbaseException("", e);
        } finally {
            if (hbaseResource != null) {
                returnTable(hbaseResource, tableName);
            }
        }
        return result;
    }


    public void delete(String tableName, byte[] rowKey,String logkey) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        HbaseResource hbaseResource = null;
        try {
            hbaseResource = getTable(tableName);
            Delete delete = new Delete(rowKey);
            hbaseResource.getTable().delete(delete);
        } catch (Exception e) {
            returnBrokenTable(hbaseResource, tableName);
            hbaseResource = null;
            throw new HbaseException("", e);
        } finally {
            if (hbaseResource != null) {
                returnTable(hbaseResource, tableName);
            }
            logger.info("hbase batchGet tableName:{},key:{},time:{},", new Object[]{tableName, logkey, stopWatch.getTime()});
        }
    }




    public <T extends ScloudSerializable>  List<T> scan(String tableName, byte[] startRow, byte[] endRow, int limit, Class<T> clazz,String logkey) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            List<byte[]> result= scan(tableName, startRow,endRow,limit);
            if (result == null) {
                return null;
            } else {
                List<T>  list = new ArrayList<>();
                for (byte[] bytes : result) {
                    list.add( ScloudSerializeUtil.decode(bytes, clazz));
                }
                return list;
            }
        } finally {
            logger.info("hbase scan tableName:{},message:{},time:{},", new Object[]{tableName, logkey, stopWatch.getTime()});
        }
    }


    public List<byte[]> scan(String tableName, byte[] startRow, byte[] endRow, int limit) {
        HbaseResource hbaseResource = null;
        List<byte[]> result = new ArrayList<>();
        try {
            hbaseResource = getTable(tableName);
            Scan scan=new Scan();
            if(startRow!=null) {
                scan.setStartRow(startRow);
            }
            if(endRow!=null) {
                scan.setStopRow(endRow);
            }
            if(limit!=0) {
            	scan.setFilter(new  PageFilter(limit));
            }

            ResultScanner  resultScanner = hbaseResource.getTable().getScanner(scan);
            if(resultScanner==null){
                return result;
            }
            Iterator<Result> resultIterator= resultScanner.iterator();
            while (resultIterator.hasNext()){
                Result hbaseResult=  resultIterator.next();
                    if (result != null && hbaseResult.rawCells().length > 0) {
                        for (Cell rowKV : hbaseResult.rawCells()) {
                            result.add(CellUtil.cloneValue(rowKV));
                        }
                    }
            }
        } catch (Exception e) {
            returnBrokenTable(hbaseResource, tableName);
            hbaseResource = null;
            throw new HbaseException("", e);
        } finally {
            if (hbaseResource != null) {
                returnTable(hbaseResource, tableName);
            }
        }
        return result;
    }




    /**
     * 顺序自增ID转换
     *
     * @return
     */
    public static byte[] getIncrementerByte(long x) {
        try {
        	byte[] bb=new byte[ByteUtil.SIZEOF_LONG];
        	bb[ 7] = (byte) (x >> 56);
    		bb[ 6] = (byte) (x >> 48);
    		bb[ 5] = (byte) (x >> 40);
    		bb[ 4] = (byte) (x >> 32);
    		bb[ 3] = (byte) (x >> 24);
    		bb[ 2] = (byte) (x >> 16);
    		bb[ 1] = (byte) (x >> 8);
    		bb[ 0] = (byte) (x >> 0);
            return bb;
        } catch (Exception e) {
            throw new HbaseException("getIncrementerByte error", e);
        }
    }
    
    public static byte[][] getIncrementerPerSpilts(int splits) {
        try {
        	int splitsNum=splits-1;
        	byte[][] bbs=new byte[splitsNum][];
        	for(int i=0;i<splitsNum;i++){
        		double b=256d/(double)splits*(double)(i+1);
        		System.out.println((long)b);
        		bbs[i]=getIncrementerByte((long)b);
        	}
        	return bbs;
        } catch (Exception e) {
            throw new HbaseException("getIncrementerByte error", e);
        }
    }
    
    
    public static void main(String[] fwef) throws Exception{
    	
    	for(int i=0;i<Integer.MAX_VALUE;i++){
    		//Random random=new Random();
    		//long l=random.nextLong();
    		byte[] bytes=getIncrementerByte(i);
    		byte[] bytes1=Bytes.toBytes((long)i);
    		for(int j=0;j<8;j++){
    			if(bytes[j]!=bytes1[7-j]){
    				System.out.println("==");
    			}
    		}
    	}
    }


}
